#!/usr/bin/env python3
"""Public job-posting importer for KarirMate. Never sends mail."""
import argparse
import json
import re
import sqlite3
from datetime import datetime, timezone
from html.parser import HTMLParser

import requests
from runtime_paths import DB

SOURCE = 'https://disnaker.tangerangkota.go.id/lowongan-kerja'
EMAIL = re.compile(r'(?i)\b[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}\b')
WORDS = re.compile(r'[a-z0-9]+')
GROUPS = [
    {'warehouse', 'warehousing', 'gudang', 'pergudangan'},
    {'logistik', 'logistic', 'logistics'},
    {'operasional', 'operational', 'operation', 'operations'},
    {'admin', 'administrasi', 'administration', 'administrative'},
    {'produksi', 'production'},
    {'supervisor', 'spv'},
    {'staff', 'staf'},
    {'operator'},
    {'koordinator', 'coordinator'},
]


def words(text):
    return set(WORDS.findall(str(text or '').casefold()))


def title_matches(title, preferences):
    have = words(title)
    for pref in preferences:
        want = words(pref)
        if want and all(any((w in group and have & group) for group in GROUPS) or w in have for w in want):
            return True
    return False


def match_location(address, preferences):
    text = str(address or '').casefold()
    return next((str(loc) for loc in preferences if str(loc).strip() and str(loc).casefold() in text), None)


class Listings(HTMLParser):
    """Read each heading with text and email from its own listing block only."""
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.in_h2 = False
        self.heading = []
        self.active = None
        self.records = []

    def _finish(self):
        if not self.active:
            return
        title, raw = self.active
        text = ' '.join(''.join(raw).split())
        found = list(dict.fromkeys(s.casefold() for s in EMAIL.findall(text)))
        if title.strip() and found and re.search(r'\baktif\b', text, re.I) and re.search(r'\bkuota\b', text, re.I):
            # Address and email are displayed immediately before vacancy quota.
            start = text.casefold().find(found[0])
            address = text[:start].strip()
            self.records.append((title.strip(), address, found))
        self.active = None

    def handle_starttag(self, tag, attrs):
        if tag == 'h2':
            self._finish()
            self.in_h2 = True
            self.heading = []
        elif self.active is not None and tag in ('div', 'p', 'li', 'br'):
            self.active[1].append(' ')

    def handle_endtag(self, tag):
        if tag == 'h2' and self.in_h2:
            self.in_h2 = False
            self.active = (''.join(self.heading), [])
        elif self.active is not None and tag in ('div', 'p', 'li', 'br'):
            self.active[1].append(' ')

    def handle_data(self, text):
        if self.in_h2:
            self.heading.append(text)
        elif self.active is not None:
            self.active[1].append(text)

    def close(self):
        super().close()
        self._finish()


def run(account_id):
    with sqlite3.connect(DB, timeout=30) as db:
        db.row_factory = sqlite3.Row
        profile = db.execute(
            'SELECT desired_positions, preferred_locations FROM android_profiles WHERE account_id=?',
            (account_id,)
        ).fetchone()
        if not profile:
            print('SKIP: profil akun tidak ditemukan')
            return
        positions = json.loads(profile['desired_positions'] or '[]')
        locations = json.loads(profile['preferred_locations'] or '[]')
        if not isinstance(positions, list) or not isinstance(locations, list) or not positions or not locations:
            print('SKIP: posisi/lokasi belum dipilih')
            return

        response = requests.get(SOURCE, timeout=(5, 20), headers={'User-Agent': 'KarirMate-PublicJobReader/1.1'})
        response.raise_for_status()
        parser = Listings()
        parser.feed(response.text)
        parser.close()

        columns = {r['name'] for r in db.execute('PRAGMA table_info(phase57_queue)')}
        mandatory = {'id', 'user_id', 'job_title', 'company', 'email', 'location', 'kind', 'status', 'contact_source'}
        if not mandatory <= columns:
            raise RuntimeError('STOP: Struktur phase57_queue tidak sesuai')
        added = upgraded = matched = 0
        uid = -account_id
        seen = set()
        for title, address, emails in parser.records:
            if not title_matches(title, positions):
                continue
            region = match_location(address, locations)
            if not region:
                continue
            matched += 1
            for email in emails:
                if email in seen:
                    continue
                seen.add(email)
                rows = db.execute(
                    'SELECT id,kind,status FROM phase57_queue WHERE user_id=? AND lower(email)=? ORDER BY id',
                    (uid, email)
                ).fetchall()
                if any(r['status'] != 'ready' for r in rows):
                    continue  # Never enqueue an address already processed.
                values = {
                    'user_id': uid, 'job_title': title,
                    'company': 'Perusahaan (portal Disnaker)',
                    'email': email, 'location': region + ' — ' + address[:160],
                    'job_url': SOURCE, 'kind': 'vacancy', 'status': 'ready',
                    'contact_source': 'disnaker:tangerangkota',
                    'created_at': datetime.now(timezone.utc).isoformat()
                }
                row = next((r for r in rows if r['kind'] == 'spontaneous'), None)
                if row:
                    change = {k:v for k,v in values.items() if k in columns and k not in ('id','user_id','email','status','created_at')}
                    db.execute('UPDATE phase57_queue SET ' + ','.join(k+'=?' for k in change) + ' WHERE id=?', (*change.values(), row['id']))
                    upgraded += 1
                elif not rows:
                    payload = {k:v for k,v in values.items() if k in columns}
                    try:
                        db.execute('INSERT INTO phase57_queue ('+','.join(payload)+') VALUES ('+','.join('?' for _ in payload)+')', tuple(payload.values()))
                        added += 1
                    except sqlite3.IntegrityError:
                        pass
        db.commit()
        print('=== SUMBER LOWONGAN DISNAKER ===')
        print('Akun:', account_id, '| Ada email & status aktif:', len(parser.records))
        print('Posisi/lokasi cocok:', matched, '| Antrean baru:', added, '| Upgrade:', upgraded)
        print('Email dikirim sekarang: 0')

if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--account-id', type=int, required=True)
    args = ap.parse_args()
    if args.account_id <= 0:
        ap.error('Account ID harus positif')
    run(args.account_id)
