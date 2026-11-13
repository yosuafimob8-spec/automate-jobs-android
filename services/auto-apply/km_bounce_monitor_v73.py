#!/usr/bin/env python3
"""KarirMate V73: read confirmed Gmail delivery failures via owned Worker.

Read-only by default; --apply marks matching 'sent' queue rows as bounced and
stages fresh regional catalog contacts. Never sends email, edits schedules, or
retries uncertain attempts. Requires separate Gmail gmail.readonly OAuth scope.
"""
import argparse
from datetime import datetime
from pathlib import Path
import sqlite3

from runtime_paths import DB


def fetch_bounces(account_id):
    import requests
    from gmail_oauth_client import BASE, headers
    response = requests.get(
        BASE.rstrip('/') + '/api/karirmate/gmail/bounces',
        params={'account_id': str(account_id)},
        headers=headers(), timeout=(10, 40), allow_redirects=False)
    if response.status_code in (401,403):
        raise RuntimeError('Gmail read belum diberi izin; hubungkan ulang Gmail dengan scope baca.')
    response.raise_for_status()
    result = response.json()
    if str(result.get('account_id')) != str(account_id) or result.get('success') is not True:
        raise RuntimeError('Respons Gmail bounce tidak cocok dengan akun atau tidak terkonfirmasi.')
    reports = result.get('bounces')
    if not isinstance(reports, list):
        raise RuntimeError('Format laporan bounce tidak valid.')
    valid = []
    for report in reports[:100]:
        if not isinstance(report, dict) or report.get('confirmed') is not True:
            continue
        email = str(report.get('recipient') or '').strip().lower()
        message_id = str(report.get('bounce_message_id') or '').strip()
        from km_catalog_v73 import valid_email
        if valid_email(email) and message_id and len(message_id) <= 180:
            valid.append((email,message_id))
    return list(dict.fromkeys(valid))


def reconcile_bounces(db_path, account_id, reports, apply=False):
    uid=-account_id
    with sqlite3.connect(db_path,timeout=30) as db:
        db.execute('CREATE TEMP TABLE IF NOT EXISTS km_v73_bounce_emails(email TEXT PRIMARY KEY)')
        db.executemany('INSERT OR IGNORE INTO km_v73_bounce_emails VALUES(?)',[(x[0],) for x in reports])
        rows=db.execute('''SELECT id,company,email FROM phase57_queue
             WHERE user_id=? AND status='sent' AND lower(trim(email)) IN
                  (SELECT email FROM km_v73_bounce_emails)''',(uid,)).fetchall()
        enabled=db.execute('SELECT enabled FROM phase57_settings WHERE user_id=?',(uid,)).fetchone()
    print('=== MONITOR BOUNCE V73 ===')
    print('Laporan Gmail terkonfirmasi:',len(reports),'| Cocok riwayat terkirim:',len(rows))
    for _,company,email in rows[:20]:
        print('BOUNCE:',company,'|',email)
    if not apply or not rows:
        print('Tidak ada perubahan DB.' if not apply else 'Tidak ada bounce baru.')
        return len(rows)
    path=Path(db_path)
    backup=path.with_name(path.name+'.backup-v73-bounce-'+datetime.now().strftime('%Y%m%d-%H%M%S-%f'))
    with sqlite3.connect(path,timeout=30) as src,sqlite3.connect(backup,timeout=30) as dst:
        src.backup(dst)
    with sqlite3.connect(path,timeout=30) as db:
        db.execute('BEGIN IMMEDIATE')
        changed=0
        for id_,company,email in rows:
            outcome=db.execute("UPDATE phase57_queue SET status='bounced',error='gmail_bounce_confirmed' WHERE user_id=? AND id=? AND status='sent'",(uid,id_))
            changed+=outcome.rowcount
        print('Bounce dicatat:',changed,'| Backup:',backup)
    if enabled and enabled[0]:
        from km_catalog_v73 import refill
        print('Kontak pengganti diantrekan:',refill(path,uid,batch=10))
    else:
        print('Auto Apply masih jeda; antrean tidak ditambah oleh monitor.')
    print('Email terkirim dari monitor: 0')
    return len(rows)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--account-id',type=int,required=True)
    p.add_argument('--apply',action='store_true')
    args=p.parse_args()
    if args.account_id < 1:
        p.error('Account ID KarirMate harus positif.')
    bounces=fetch_bounces(args.account_id)
    reconcile_bounces(DB,args.account_id,bounces,args.apply)

if __name__=='__main__': main()
