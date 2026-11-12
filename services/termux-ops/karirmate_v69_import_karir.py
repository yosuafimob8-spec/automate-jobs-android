#!/usr/bin/env python3
"""KarirMate V69: conservative, isolated Karir.com -> idjobs_feed importer.

Reads an exported Scrapy JSON array. Default is DRY RUN; --apply enables
an atomic upsert into the existing KarirMate database after an SQLite backup.
Only active, unexpired vacancies in warehouse/logistics/operations in
Jabodetabek are eligible. Uses V67 position_tier/location_match unmodified.

No web requests, email sends, Git actions, APK builds, or cron changes.
"""
from __future__ import annotations

import argparse
import json
import re
import sqlite3
import sys
import time
from datetime import datetime
from pathlib import Path
from urllib.parse import urlsplit

APP_DIR = Path.home() / 'automate-ai'
sys.path.insert(0, str(APP_DIR))

LOCATIONS = ('Tangerang', 'Bogor', 'Depok', 'Jakarta', 'Bekasi')
POSITIONS = (
    'Admin Warehouse', 'Staff Warehouse', 'Supervisor Warehouse',
    'Admin Logistic', 'Staff Logistic', 'Supervisor Logistic',
    'Admin Operational',
)


def canonical_karir_url(value):
    if not isinstance(value, str):
        return None
    try:
        p = urlsplit(value.strip())
    except ValueError:
        return None
    if p.scheme != 'https' or (p.hostname or '').lower() not in {'karir.com', 'www.karir.com'}:
        return None
    if p.username or p.password or p.port not in (None, 443):
        return None
    m = re.fullmatch(r'/opportunities/(\d+)/?', p.path)
    if not m:
        return None
    return f'https://karir.com/opportunities/{m.group(1)}'


def active(value):
    return value is True or (isinstance(value, str) and value.strip().lower() == 'true')


def expiry_future(value, now):
    if not isinstance(value, str) or not value.strip():
        return False
    try:
        date = datetime.fromisoformat(value.strip().replace('Z', '+00:00'))
    except ValueError:
        return False
    if date.tzinfo is None:
        # Karir.com export shows local-style timestamps without a timezone.
        return date > now.replace(tzinfo=None)
    return date > now.astimezone(date.tzinfo)


def trimmed(value, limit=220):
    if not isinstance(value, str):
        return ''
    return re.sub(r'\s+', ' ', value).strip()[:limit]


def eligible_rows(rows, now, position_tier, location_match):
    records = {}
    counts = {'read': 0, 'valid': 0, 'inactive_or_expired': 0,
              'invalid_url_or_fields': 0, 'out_of_scope': 0}
    for item in rows:
        counts['read'] += 1
        if not isinstance(item, dict):
            counts['invalid_url_or_fields'] += 1
            continue
        url = canonical_karir_url(item.get('job_url') or item.get('url'))
        title = trimmed(item.get('job_title') or item.get('title'))
        company = trimmed(item.get('company'))
        location = trimmed(item.get('job_location') or item.get('location'))
        if not url or not title or not company or not location:
            counts['invalid_url_or_fields'] += 1
            continue
        if not active(item.get('is_active')) or not expiry_future(item.get('job_apply_end_date'), now):
            counts['inactive_or_expired'] += 1
            continue
        if not any(position_tier(title, p) > 0 for p in POSITIONS) or not any(
            location_match(location, city) for city in LOCATIONS
        ):
            counts['out_of_scope'] += 1
            continue
        # Use the exported posting timestamp if supplied, rather than claiming
        # the posting was created today. The schema field is named scraped_at.
        published = trimmed(item.get('last_seen') or '', 60)
        records[url] = (url, title, company, location, 'Karir.com', published)
        counts['valid'] += 1
    return list(records.values()), counts


def import_rows(records, dbpath):
    # Strictly avoid silently creating a new/empty KarirMate database.
    if not dbpath.is_file():
        raise RuntimeError(f'Database KarirMate tidak ditemukan: {dbpath}')
    with sqlite3.connect(f'file:{dbpath}?mode=ro', uri=True) as db:
        exists = db.execute(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name='idjobs_feed'"
        ).fetchone()
    if not exists:
        raise RuntimeError('Tabel idjobs_feed tidak ditemukan. STOP tanpa impor.')

    runid = datetime.now().strftime('%Y%m%d_%H%M%S_%f')
    backup = dbpath.with_name(dbpath.name + f'.backup-v69-{runid}')
    with sqlite3.connect(dbpath) as source, sqlite3.connect(backup) as dest:
        source.backup(dest)

    inserted = 0
    updated = 0
    now_ts = int(time.time())
    with sqlite3.connect(dbpath, timeout=20) as db:
        db.execute('PRAGMA busy_timeout=20000')
        for url, title, company, location, source, seen in records:
            already = db.execute('SELECT 1 FROM idjobs_feed WHERE url=?', (url,)).fetchone()
            db.execute('''
                INSERT INTO idjobs_feed
                  (url, title, company, location, source, scraped_at, imported_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(url) DO UPDATE SET
                  title=excluded.title, company=excluded.company,
                  location=excluded.location, source=excluded.source,
                  scraped_at=excluded.scraped_at, imported_at=excluded.imported_at
            ''', (url, title, company, location, source, seen, now_ts))
            if already:
                updated += 1
            else:
                inserted += 1
    return inserted, updated, backup


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('file', type=Path, help='Hasil JSON baru dari scraper karir')
    parser.add_argument('--apply', action='store_true', help='Backup dan simpan lowongan valid ke DB')
    args = parser.parse_args()

    if not args.file.is_file() or args.file.stat().st_size > 30_000_000:
        parser.error('File JSON tidak ditemukan atau terlalu besar (>30 MB)')
    try:
        rows = json.loads(args.file.read_text(encoding='utf-8'))
    except (OSError, json.JSONDecodeError) as exc:
        parser.error(f'File JSON tidak valid: {type(exc).__name__}')
    if not isinstance(rows, list):
        parser.error('JSON harus berupa array lowongan')

    try:
        from idjobs_feed import DB, position_tier, location_match
    except ImportError as exc:
        parser.error(f'Modul KarirMate gagal dimuat: {exc}')

    valid, counts = eligible_rows(rows, datetime.now().astimezone(), position_tier, location_match)
    print('=== KARIRMATE KARIR.COM V69 ===')
    for label, n in counts.items():
        print(f'{label}: {n}')
    print('unik_sesuai:', len(valid))
    for item in valid[:12]:
        print('LOWONGAN:', item[1], '|', item[3], '|', item[0])

    if not args.apply:
        print('DRY RUN: Database tidak diubah. Tambahkan --apply setelah meninjau hasil.')
        return
    if not valid:
        print('SKIP: Tidak ada lowongan yang lolos validasi. Database tidak diubah.')
        return
    inserted, updated, backup = import_rows(valid, Path(DB))
    print(f'PASS: ditambahkan={inserted}, diperbarui={updated}')
    print('BACKUP:', backup)
    print('Email dikirim: 0; Cron/GitHub/APK tidak diubah.')


if __name__ == '__main__':
    main()
