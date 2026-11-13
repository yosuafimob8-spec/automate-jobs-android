"""Account controls save consent only. They never start a sender or edit cron."""
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
import phase57_queue
import phase5_config
from runtime_paths import DB

def state(uid):
    phase57_queue.initialize()
    from phase512_sender import quota_used, attempts_used
    with sqlite3.connect(DB, timeout=30) as db:
        row = db.execute('SELECT enabled,daily_limit,consent_at FROM phase57_settings WHERE user_id=?', (uid,)).fetchone()
        counts = dict(db.execute('SELECT status,COUNT(*) FROM phase57_queue WHERE user_id=? GROUP BY status', (uid,)))
        used, attempted = quota_used(db,uid), attempts_used(db,uid)
        cfg = db.execute('SELECT auto_consent FROM phase5_config WHERE user_id=?', (uid,)).fetchone()
        consent = bool(row and row[2] and cfg and cfg[0])
    limit = min(5,max(0,int(row[1]))) if row else 5
    return {'enabled': bool(row and row[0] and consent), 'limit': limit,
            'consent_at': row[2] if consent else None, 'consent_granted': consent,
            'counts': counts, 'quota_used_today': used,
            'remaining_today': max(0, min(limit-used, 10-attempted)),
            'attempts_used_today': attempted, 'attempts_limit_today': 10,
            'bounced': counts.get('bounced',0), 'scheduler_modified': False}

def enable(uid,gmail_connected,explicit_consent=False,daily_limit=5):
    cfg=phase5_config.get(uid)
    if gmail_connected is not True: return False,'Hubungkan Gmail terlebih dahulu.'
    path=Path(cfg.get('cv_path') or '')
    if not path.is_file() or path.stat().st_size>2_200_000 or not path.read_bytes().startswith(b'%PDF-'): return False,'Upload CV PDF valid terlebih dahulu.'
    phase57_queue.initialize()
    existing=state(uid)
    if explicit_consent is not True and not (existing['consent_granted'] and cfg.get('auto_consent')): return False,'Persetujuan eksplisit diperlukan.'
    if type(daily_limit) is not int or not 1<=daily_limit<=5: return False,'Batas harus 1–5 email per hari.'
    with sqlite3.connect(DB,timeout=30) as db:
        db.execute('BEGIN IMMEDIATE')
        db.execute('UPDATE phase5_config SET auto_consent=1,daily_limit=? WHERE user_id=?',(daily_limit,uid))
        db.execute('''INSERT INTO phase57_settings(user_id,enabled,daily_limit,consent_at) VALUES(?,1,?,?)
            ON CONFLICT(user_id) DO UPDATE SET enabled=1,daily_limit=excluded.daily_limit,
            consent_at=COALESCE(phase57_settings.consent_at,excluded.consent_at)''',(uid,daily_limit,datetime.now(timezone.utc).isoformat()))
    return True,'Izin Auto Apply tersimpan. Jadwal dan izin operasional pengiriman tetap mengikuti konfigurasi operator.'

def pause(uid):
    phase57_queue.initialize()
    with sqlite3.connect(DB,timeout=30) as db:
        db.execute('INSERT OR IGNORE INTO phase57_settings(user_id,enabled,daily_limit) VALUES(?,0,5)',(uid,))
        db.execute('UPDATE phase57_settings SET enabled=0 WHERE user_id=?',(uid,))

def history(uid,limit=100):
    phase57_queue.initialize()
    with sqlite3.connect(DB,timeout=30) as db:
        db.row_factory=sqlite3.Row
        rows=db.execute('''SELECT id,company,job_title,email,status,sent_at,gmail_message_id,error,kind
            FROM phase57_queue WHERE user_id=? ORDER BY id DESC LIMIT ?''',(uid,min(100,max(1,limit)))).fetchall()
    items=[]
    for row in rows:
        item=dict(row)
        if item['status']=='sent' and not item['gmail_message_id']: item['status']='uncertain'
        items.append(item)
    return items

def report(uid):
    s=state(uid)
    return f"Auto Apply: {'Aktif' if s['enabled'] else 'Jeda'}\nBatas: {s['limit']}/hari\nKuota terpakai/belum pasti: {s['quota_used_today']}\nTerkirim tercatat: {s['counts'].get('sent',0)}\nJadwal tidak diubah oleh tombol aktivasi."
