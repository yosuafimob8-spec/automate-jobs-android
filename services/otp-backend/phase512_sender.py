"""Gmail sender: dry-run by default, no cron edits and no automatic retries.
Live CLI use additionally requires KARIRMATE_ALLOW_LIVE_SEND=1 after operator approval.
"""
import argparse
import base64
import hashlib
import json
import os
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from zoneinfo import ZoneInfo
import re
import phase57_queue
import phase5_config
from runtime_paths import DB
from gmail_oauth_client import status as gmail_status, send_application

WIB = ZoneInfo('Asia/Jakarta')
BLOCKED = ('kalibrr.com','glints.com','indeed.com','dealls.com','jobstreet.com','linkedin.com')

def valid_target(email):
    if not isinstance(email,str) or not re.fullmatch(r"[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}",email): return False
    domain=email.rsplit('@',1)[1].lower()
    return not any(domain == d or domain.endswith('.'+d) for d in BLOCKED)

def _now(): return datetime.now(timezone.utc)

def _day(value):
    if not value: return None
    try:
        dt=datetime.fromisoformat(value)
        if dt.tzinfo is None: dt=dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(WIB).date().isoformat()
    except (ValueError,TypeError): return None

def quota_used(db,uid,now=None):
    today=(now or _now()).astimezone(WIB).date().isoformat()
    rows=db.execute("SELECT status,attempt_day,sent_at FROM phase57_queue WHERE user_id=? AND status IN ('sent','sending','uncertain')",(uid,))
    return sum(1 for status,attempt_day,sent_at in rows if (attempt_day or _day(sent_at)) == today or (status in ('sending','uncertain') and not attempt_day))

def sent_today(db,uid):
    today=_now().astimezone(WIB).date().isoformat()
    return sum(_day(row[0])==today for row in db.execute("SELECT sent_at FROM phase57_queue WHERE user_id=? AND status='sent'",(uid,)))

def _is_pro(uid):
    try:
        if uid < 0:
            from account_access_policy import resolve_access
            from pathlib import Path

            account_id = -uid
            uri = Path(DB).resolve().as_uri() + "?mode=ro"

            with sqlite3.connect(uri, uri=True, timeout=5) as db:
                db.row_factory = sqlite3.Row

                account = db.execute(
                    "SELECT id,telegram_user_id FROM android_accounts WHERE id=?",
                    (account_id,)
                ).fetchone()

                if account is None:
                    return False

                grant = None
                exists = db.execute(
                    "SELECT 1 FROM sqlite_master WHERE type='table' AND name='account_access'"
                ).fetchone()

                if exists:
                    row = db.execute(
                        "SELECT account_id,role,pro_permanent,pro_until "
                        "FROM account_access WHERE account_id=?",
                        (account_id,)
                    ).fetchone()
                    if row:
                        grant = dict(row)

                legacy = 0
                if account["telegram_user_id"] is not None:
                    has_users = db.execute(
                        "SELECT 1 FROM sqlite_master WHERE type='table' AND name='users'"
                    ).fetchone()
                    if has_users:
                        row = db.execute(
                            "SELECT pro_until FROM users WHERE id=?",
                            (account["telegram_user_id"],)
                        ).fetchone()
                        if row:
                            legacy = row["pro_until"]

                return resolve_access(
                    account_id, grant, legacy
                ).is_pro

        import karirmate_integrasi_final as membership
        with sqlite3.connect(DB, timeout=30) as db:
            return membership.pro(db, uid).get("pro") is True

    except Exception:
        return False


def extract_cv_text(path):
    from pypdf import PdfReader
    pdf=PdfReader(path)
    if pdf.is_encrypted or not 1<=len(pdf.pages)<=30: raise ValueError('CV harus PDF 1–30 halaman tanpa kata sandi.')
    text='\n'.join(page.extract_text() or '' for page in pdf.pages).casefold()
    if sum(c.isalpha() for c in text)<40: raise ValueError('Teks CV belum dapat dibaca untuk pencocokan. Gunakan PDF teks; OCR belum tersedia.')
    return text

def _claim(uid,now,cv_text,allow_spontaneous=False):
    today=now.astimezone(WIB).date().isoformat()
    with sqlite3.connect(DB,timeout=30) as db:
        db.execute('BEGIN IMMEDIATE')
        settings=db.execute('SELECT enabled,daily_limit,consent_at FROM phase57_settings WHERE user_id=?',(uid,)).fetchone()
        cfg=db.execute('SELECT auto_consent,daily_limit FROM phase5_config WHERE user_id=?',(uid,)).fetchone()
        if not settings or not settings[0] or not settings[2] or not cfg or not cfg[0]: return None
        limit=max(0,min(5,int(settings[1]),int(cfg[1])))
        if quota_used(db,uid,now)>=limit: return None
        try:
            if uid < 0:
                profile=db.execute(
                    'SELECT desired_positions,preferred_locations FROM android_profiles WHERE account_id=? LIMIT 1',
                    (-uid,)
                ).fetchone()
            else:
                profile=db.execute(
                    'SELECT p.desired_positions,p.preferred_locations FROM android_profiles p JOIN android_accounts a ON a.id=p.account_id WHERE a.telegram_user_id=? LIMIT 1',
                    (uid,)
                ).fetchone()
            if not profile: return None
            positions=json.loads(profile[0]);locations=json.loads(profile[1])
            if not isinstance(positions,list) or not isinstance(locations,list) or not positions or not locations: return None
        except (sqlite3.Error,ValueError): return None
        db.row_factory=sqlite3.Row
        rows=db.execute("SELECT * FROM phase57_queue WHERE user_id=? AND status='ready' ORDER BY CASE kind WHEN 'vacancy' THEN 0 ELSE 1 END,id",(uid,)).fetchall()

        # KM_USER_PRIORITY_V1
        # Lowongan aktif > urutan lokasi pengguna > posisi > antrean lama
        def candidate_priority(row):
            title = str(row["job_title"] or "").casefold()
            place = str(row["location"] or "").casefold()

            location_rank = next(
                (
                    i for i, loc in enumerate(locations)
                    if str(loc).strip()
                    and str(loc).casefold() in place
                ),
                len(locations)
            )

            position_rank = next(
                (
                    i for i, pos in enumerate(positions)
                    if str(pos).strip()
                    and str(pos).casefold() in title
                ),
                len(positions)
            )

            return (
                0 if row["kind"] == "vacancy" else 1,
                location_rank,
                position_rank,
                row["id"]
            )

        rows = sorted(rows, key=candidate_priority)
        for row in rows:
            item=dict(row);email=item['email'].strip().lower()
            title=item['job_title'].casefold();location=item['location'].casefold()
            if not any(str(p).casefold() in title for p in positions) or not any(str(loc).casefold() in location for loc in locations): continue
            if not any(word in cv_text for word in re.findall(r'[a-z]{3,}',title)): continue
            if not valid_target(email) or item['kind'] not in ('vacancy','spontaneous') or not item['contact_source'] or not item['location']: continue
            if item['kind']=='spontaneous' and not allow_spontaneous: continue
            if item['kind']=='vacancy' and (not item['evidence_url'] or not item['expires_at'] or item['expires_at']<=int(now.timestamp())): continue
            try: db.execute('INSERT INTO phase57_delivery_claims VALUES(?,?,?)',(uid,email,item['id']))
            except sqlite3.IntegrityError: continue
            key=hashlib.sha256(f"{uid}\0{item['job_url']}\0{email}".encode()).hexdigest()
            db.execute("UPDATE phase57_queue SET status='sending',attempt_day=?,claimed_at=?,job_key=?,error=NULL WHERE id=? AND status='ready'",(today,now.isoformat(),key,item['id']))
            item.update(email=email,job_key=key)
            return item
    return None

# KARIRMATE_SENDER_ROUTE_V1
def _route_status(uid):
    if uid < 0:
        from gmail_oauth_client import status_karirmate
        result = status_karirmate(-uid)
        return {
            "connected": result.get("connected") is True,
            "supports_spontaneous": True
        }
    return gmail_status(uid)

def _route_send(payload):
    uid = int(payload["telegram_user_id"])
    if uid < 0:
        from km_gmail_sender import send_application as km_send
        return km_send(-uid, payload)
    return send_application(payload)

def send_batch(uid,name,*,sender=_route_send,status_reader=_route_status,pro_checker=_is_pro,clock=_now):
    """No caller retry of sending/uncertain rows; every reservation counts toward five."""
    if sender in (send_application, _route_send) and os.environ.get('KARIRMATE_ALLOW_LIVE_SEND')!='1':
        raise PermissionError('Pengiriman produksi belum diizinkan operator.')
    now=clock()
    if now.astimezone(WIB).weekday()>=5: return {'sent':0,'reason':'weekend'}
    if not name.strip(): raise ValueError('Nama pelamar wajib diisi.')
    if pro_checker(uid) is not True: raise PermissionError('Langganan PRO belum terverifikasi.')
    cfg=phase5_config.get(uid)
    cv=Path(cfg.get('cv_path') or '')
    if not cv.is_file(): raise ValueError('CV PDF belum tersedia.')
    if cv.stat().st_size>2_200_000: raise ValueError('CV melebihi batas 2,2 MB.')
    pdf=cv.read_bytes()
    if not pdf.startswith(b'%PDF-'): raise ValueError('CV bukan PDF valid.')
    cv_text=extract_cv_text(cv)
    gmail=status_reader(uid)
    if gmail.get('connected') is not True: raise PermissionError('Gmail belum terhubung; otorisasi ulang diperlukan.')
    phase57_queue.initialize()
    # Refill only from existing, matching company records. Unknown schemas never create contacts.
    from auto_apply_api import candidates
    with sqlite3.connect(DB,timeout=30) as db:
        row=db.execute('SELECT id FROM android_accounts WHERE telegram_user_id=? LIMIT 1',(uid,)).fetchone()
    if row:
        fresh,_=candidates(row[0],uid)
        for candidate in fresh: phase57_queue.enqueue(uid,candidate)
    result={'sent':0,'uncertain':0}
    for _ in range(5):
        if pro_checker(uid) is not True: break
        job=_claim(uid,clock(),cv_text,gmail.get('supports_spontaneous') is True)
        if not job: break
        try:
            # KM_PERSONAL_COVER_LETTER_V40
            from km_cover_letter import compose
            letter_subject, letter_body = compose(
                name, job['job_title'], job['company'], job['kind']
            )
            message_id=sender({'telegram_user_id':str(uid),'job_key':job['job_key'],'to':job['email'],
                'applicant_name':name.strip(),'job_title':job['job_title'],'company':job['company'],
                'application_kind':job['kind'],'subject':letter_subject,'body':letter_body,'pdf_base64':base64.b64encode(pdf).decode('ascii')})
            if not isinstance(message_id,str) or not message_id.strip() or message_id.strip().lower()=='null': raise ValueError('Missing Gmail message ID')
        except Exception as exc:
            # Only exception class is stored: never persist token-bearing provider error bodies.
            with sqlite3.connect(DB,timeout=30) as db:
                db.execute("UPDATE phase57_queue SET status='uncertain',error=? WHERE id=? AND user_id=? AND status='sending'",(type(exc).__name__,job['id'],uid))
            result['uncertain']+=1
            break  # connection/auth error: stop the batch rather than consuming more rows
        with sqlite3.connect(DB,timeout=30) as db:
            db.execute("UPDATE phase57_queue SET status='sent',sent_at=?,gmail_message_id=?,error=NULL WHERE id=? AND user_id=? AND status='sending'",(clock().isoformat(),message_id,job['id'],uid))
        result['sent']+=1
    return result

def execute(uid,name,live):
    if not live:
        # Dry-run does not initialize/migrate a database, check Gmail, or call the worker.
        if not DB.is_file(): return {'mode':'dry-run','ready':0,'reason':'database_missing'}
        with sqlite3.connect(f'file:{DB}?mode=ro',uri=True) as db:
            try: count=db.execute("SELECT count(*) FROM phase57_queue WHERE user_id=? AND status='ready'",(uid,)).fetchone()[0]
            except sqlite3.Error: count=0
        return {'mode':'dry-run','ready':count,'sending_enabled':False}
    if os.environ.get('KARIRMATE_ALLOW_LIVE_SEND')!='1': raise PermissionError('Pengiriman produksi belum diizinkan operator.')
    return send_batch(uid,name)

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--uid',type=int,required=True)
    parser.add_argument('--name',default=os.environ.get('AUTOMATE_APPLICANT_NAME',''))
    parser.add_argument('--send',action='store_true')
    args=parser.parse_args()
    print(execute(args.uid,args.name,args.send))
