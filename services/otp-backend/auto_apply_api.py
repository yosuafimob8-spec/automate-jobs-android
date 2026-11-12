"""Adapter for the existing Android API. No sender/scheduler is executed here."""
import json
import os
import sqlite3
import tempfile
from pathlib import Path
from runtime_paths import DB, CV_DIR
import gmail_oauth_client
import phase5_config
import phase511_controls

ROUTES={'/auto-apply/status','/auto-apply/preferences','/auto-apply/enable','/auto-apply/pause','/gmail/oauth/start','/cv','/cv/status','/karirmate/report','/karirmate/candidates','/karirmate/selection'}

class RequestError(Exception):
    def __init__(self,status,message): self.status,self.message=status,message

def _body(handler,limit=8192,raw=False):
    try: size=int(handler.headers.get('Content-Length','0'))
    except ValueError: raise RequestError(400,'Content-Length tidak valid.') from None
    if not 0<size<=limit: raise RequestError(413,'Ukuran permintaan tidak valid.')
    data=handler.rfile.read(size)
    if len(data)!=size: raise RequestError(400,'Permintaan tidak lengkap.')
    if raw: return data
    try: value=json.loads(data)
    except (ValueError,UnicodeError): raise RequestError(400,'JSON tidak valid.') from None
    if not isinstance(value,dict): raise RequestError(400,'Objek JSON diperlukan.')
    return value

# KARIRMATE_ACCOUNT_ID_V2
def _identity(handler, require_pro=False, allow_unlinked=False):
    account_id = handler.authenticated_account()
    if account_id is None:
        raise RequestError(401, "Sesi berakhir. Login kembali.")

    with sqlite3.connect(DB, timeout=30) as db:
        row = db.execute(
            "SELECT id FROM android_accounts WHERE id=?",
            (account_id,)
        ).fetchone()

    if not row:
        raise RequestError(401, "Akun tidak ditemukan.")

    if require_pro:
        account = handler.account_status()
        if not account:
            raise RequestError(401, "Sesi tidak valid.")
        explicit = account.get("is_pro")
        active = (
            explicit if isinstance(explicit, bool)
            else str(account.get("membership", "")).lower() == "pro"
        )
        if not active:
            raise RequestError(403, "Langganan PRO diperlukan.")

    # ID negatif memisahkan data KarirMate dari Telegram.
    return int(account_id), -int(account_id)

def _status(uid):
    cfg=phase5_config.get(uid)
    return {'success':True,'operator_send_allowed':os.environ.get('KARIRMATE_ALLOW_LIVE_SEND')=='1','schedule_status':'not_verified','gmail_connected':gmail_oauth_client.status_karirmate(-uid).get('connected') is True,
            'cv_uploaded':bool(cfg.get('cv_path') and Path(cfg['cv_path']).is_file()),
            'auto_apply':phase511_controls.state(uid)}

def candidates(account_id,uid):
    """Do not turn scraped emails or contacts without location into verified vacancies."""
    from candidate_selection import select_for_profile
    from phase512_sender import extract_cv_text
    cfg=phase5_config.get(uid)
    cv_path=Path(cfg.get('cv_path') or '')
    if not cv_path.is_file(): return [],'Unggah CV terlebih dahulu.'
    try: cv_text=extract_cv_text(cv_path)
    except (ValueError,ImportError): return [],'CV belum dapat diekstrak untuk pencocokan.'
    with sqlite3.connect(DB,timeout=30) as db:
        db.row_factory=sqlite3.Row
        profile=db.execute('SELECT desired_positions,preferred_locations FROM android_profiles WHERE account_id=?',(account_id,)).fetchone()
        if not profile: return [],'Lengkapi posisi dan lokasi pada profil.'
        positions=json.loads(profile['desired_positions']);locations=json.loads(profile['preferred_locations'])
        claimed={r[0] for r in db.execute('SELECT email FROM phase57_delivery_claims WHERE user_id=?',(uid,))}
        rows=[]
        for table in ('km_stage1_sources','km_companies'):
            columns={r[1] for r in db.execute(f'PRAGMA table_info({table})')}
            if not {'company','email','position','user_id'}<=columns: continue
            for row in db.execute(f'SELECT * FROM {table} WHERE user_id=? LIMIT 500',(uid,)):
                c=dict(row)
                if str(c.get('email') or '').strip().lower() in claimed: continue
                source=c.get('source_url') or c.get('source') or ''
                verified=c.get('vacancy_verified') in (True,1) and c.get('active') in (True,1)
                rows.append(dict(c,source_url=source,contact_source=source,kind='vacancy' if verified else 'spontaneous',vacancy_verified=verified,active=verified))
    import re
    rows=[r for r in rows if any(word in cv_text for word in re.findall(r'[a-z]{3,}',r['position'].casefold()))]
    return select_for_profile(rows,positions,locations), 'Kontak tanpa lokasi/posisi/sumber yang sesuai tidak dipilih. Tidak ada lowongan atau alamat email yang dikarang.'

def handle(handler,path,method):
    if path not in ROUTES: return False
    try:
        # karirmate_gmail_account_oauth_v1: Gmail OAuth keyed by authenticated KarirMate account.
        if path == '/gmail/oauth/start' and method == 'POST':
            account_id = handler.authenticated_account()
            if account_id is None:
                raise RequestError(401, 'Masuk ke akun KarirMate terlebih dahulu.')
            _body(handler)  # ensure valid JSON request, e.g. {}
            handler.respond(200, {'success': True,
                                  'url': gmail_oauth_client.begin_karirmate(account_id)})
            return True
        account_id,uid=_identity(handler,require_pro=path in ('/auto-apply/enable','/karirmate/candidates'),allow_unlinked=path=='/auto-apply/status')
        if method=='GET' and path=='/auto-apply/status':
            data=_status(uid) if uid is not None else {'success': True, 'operator_send_allowed': False, 'schedule_status': 'telegram_link_required', 'gmail_connected': False, 'cv_uploaded': False, 'auto_apply': {'enabled': False, 'consent_granted': False, 'positions': [], 'locations': [], 'preferences_enforced': False}, 'action_required': 'telegram_link', 'message': 'Akun belum tertaut dengan identitas Telegram untuk Gmail Auto Apply. Pengiriman nonaktif.'}
            km_connected=gmail_oauth_client.status_karirmate(account_id).get('connected') is True
            data['gmail_connected']=km_connected
            if uid is None:
                data['schedule_status']='not_configured'
                data['action_required']='auto_apply_integration_pending'
                data['message']='Gmail dapat ditautkan langsung; pengiriman otomatis tetap nonaktif.'
            data['operator_send_allowed']=False
            # Read preference used by the real sender; never infer it from local UI.
            if uid is not None:
                with sqlite3.connect(DB,timeout=30) as db:
                    pref=db.execute('SELECT desired_positions,preferred_locations FROM android_profiles WHERE account_id=?',(account_id,)).fetchone()
                if pref and isinstance(data.get('auto_apply'),dict):
                    try:
                        roles=json.loads(pref[0] or '[]')
                        places=json.loads(pref[1] or '[]')
                    except (ValueError,TypeError):
                        roles,places=[],[]
                    data['auto_apply'].update(positions=roles,locations=places,
                        preferences_enforced=bool(roles and places))

        # KM_PREFERENCES_V56: authenticated, account-scoped, never sends email.
        elif method=='POST' and path=='/auto-apply/preferences':
            request=_body(handler)
            roles=request.get('positions')
            locations=request.get('locations')
            if not isinstance(roles,list) or not isinstance(locations,list):
                raise RequestError(400,'Posisi dan wilayah wajib berupa daftar.')
            def safe_choices(values, maximum):
                if not 1<=len(values)<=maximum or any(not isinstance(x,str) or not 1<=len(x.strip())<=80 for x in values):
                    raise RequestError(400,'Pilihan posisi atau wilayah tidak valid.')
                result=list(dict.fromkeys(x.strip() for x in values))
                if not result: raise RequestError(400,'Pilihan tidak boleh kosong.')
                return result
            roles=safe_choices(roles,20)
            locations=safe_choices(locations,30)
            import phase57_queue
            phase57_queue.initialize()
            with sqlite3.connect(DB,timeout=30) as db:
                db.execute('BEGIN IMMEDIATE')
                running=db.execute('SELECT enabled FROM phase57_settings WHERE user_id=?',(uid,)).fetchone()
                if running and running[0]:
                    raise RequestError(409,'Jeda Auto Apply sebelum mengubah target.')
                changed=db.execute('UPDATE android_profiles SET desired_positions=?,preferred_locations=? WHERE account_id=?',
                    (json.dumps(roles,ensure_ascii=False),json.dumps(locations,ensure_ascii=False),account_id))
                if changed.rowcount!=1:
                    raise RequestError(409,'Lengkapi profil sebelum memilih target.')
            data={'success':True,'positions':roles,'locations':locations,'applied_to_delivery':True}
        elif method=='POST' and path=='/gmail/oauth/start':
            _body(handler)
            data={'success':True,'url':gmail_oauth_client.begin(uid)}
        elif method=='GET' and path=='/cv/status':
            cfg=phase5_config.get(uid);file=Path(cfg.get('cv_path') or '')
            data={'success':True,'uploaded':file.is_file(),'size_bytes':file.stat().st_size if file.is_file() else 0}
        elif method=='PUT' and path=='/cv':
            content=_body(handler,2_200_000,raw=True)
            if not content.startswith(b'%PDF-'): raise RequestError(400,'CV harus PDF.')
            # Reject corrupt/encrypted documents without executing external commands.
            from io import BytesIO
            from pypdf import PdfReader
            pdf=PdfReader(BytesIO(content))
            if pdf.is_encrypted or not 1<=len(pdf.pages)<=30: raise RequestError(400,'PDF harus 1–30 halaman tanpa kata sandi.')
            CV_DIR.mkdir(parents=True,exist_ok=True)
            fd,name=tempfile.mkstemp(prefix=f'cv-{uid}-',suffix='.pdf',dir=CV_DIR)
            try:
                with os.fdopen(fd,'wb') as stream: stream.write(content)
                phase5_config.set_cv(uid,name)
            except Exception:
                Path(name).unlink(missing_ok=True);raise
            data={'success':True,'size_bytes':len(content)}
        elif method=='POST' and path=='/auto-apply/enable':
            body=_body(handler)
            connected=gmail_oauth_client.status_karirmate(account_id).get('connected') is True
            ok,message=phase511_controls.enable(uid,connected,body.get('consent') is True,body.get('max_emails_per_day',5))
            if not ok: raise RequestError(409,message)
            items,note=candidates(account_id,uid)
            import phase57_queue
            for candidate in items: phase57_queue.enqueue(uid,candidate)
            data={'success':True,'message':message,'queue_note':note,'auto_apply':phase511_controls.state(uid)}
        elif method=='POST' and path=='/auto-apply/pause':
            phase511_controls.pause(uid);data={'success':True,'auto_apply':phase511_controls.state(uid)}
        elif method=='GET' and path=='/karirmate/report':
            data={'success':True,'items':phase511_controls.history(uid),'auto_apply':phase511_controls.state(uid)}
        elif method=='GET' and path=='/karirmate/candidates':
            items,note=candidates(account_id,uid);data={'success':True,'items':items,'note':note,'sending_enabled':False}
        elif method=='GET' and path=='/karirmate/selection': data={'success':True,'selection':{'mode':'cv_and_location','decided_by':'backend','random_fallback':False}}
        else: raise RequestError(405,'Metode tidak didukung untuk endpoint ini.')
        handler.respond(200,data)
    except RequestError as exc: handler.respond(exc.status,{'success':False,'error':exc.message})
    except gmail_oauth_client.GmailReauthorizationRequired: handler.respond(409,{'success':False,'error':'Gmail perlu dihubungkan ulang.','reauthorization_required':True})
    except (ImportError,sqlite3.Error): handler.respond(503,{'success':False,'error':'Dependensi/skema backend belum tersedia; aktivasi ditolak.'})
    except Exception:
        handler.respond(503,{'success':False,'error':'Layanan belum dapat memproses permintaan. Periksa status sebelum mencoba ulang.'})
    return True
