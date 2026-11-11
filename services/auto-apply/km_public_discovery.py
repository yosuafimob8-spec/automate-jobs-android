#!/usr/bin/env python3
"""KarirMate public job discovery: RSS -> public posting -> queued vacancy.
Does not send email. User profile determines all searches.
"""
import argparse
import email.utils
import html
import json
import re
import sqlite3
from datetime import datetime, timezone, timedelta
from urllib.parse import urlparse, parse_qs
from xml.etree import ElementTree as ET

import requests
from runtime_paths import DB

RSS = 'https://www.bing.com/search'
EMAIL = re.compile(r'(?i)(?<![\w.+%-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}(?![\w.-])')
APPLICATION_WORDS = re.compile(r'(?i)lamaran|melamar|kirim\s*(cv|lamaran)|apply|rekrut|recruit|hrd|hiring|karir|career|email')
EXCLUDED_HOSTS = ('linkedin.com', 'indeed.com', 'glints.com', 'jobstreet.co.id', 'kalibrr.com', 'facebook.com', 'instagram.com')
HEADERS = {'User-Agent': 'KarirMateJobDiscovery/1.0 (+public RSS; email-contact-only)'}


def folded(value):
    return str(value or '').casefold().strip()


def match(value, choices):
    value = folded(value)
    return bool(value) and any(folded(x) in value for x in choices if folded(x))


def permitted_link(link):
    parsed = urlparse(link)
    host = (parsed.hostname or '').casefold()
    if parsed.scheme not in ('https', 'http') or not host:
        return False
    if host == 'localhost' or host.endswith('.local') or host.startswith(('127.', '10.', '192.168.', '172.16.')):
        return False
    if any(host == h or host.endswith('.' + h) for h in EXCLUDED_HOSTS):
        return False
    return True


def read_feed(session, phrase):
    response = session.get(RSS, params={'q': phrase, 'format': 'rss', 'setlang': 'id'}, timeout=(4, 10))
    response.raise_for_status()
    root = ET.fromstring(response.content)
    items = []
    for item in root.findall('.//item')[:8]:
        title = html.unescape(item.findtext('title') or '')
        summary = html.unescape(item.findtext('description') or '')
        link = html.unescape(item.findtext('link') or '').strip()
        parsed = urlparse(link)
        if parsed.hostname and 'bing.com' in parsed.hostname:
            link = parse_qs(parsed.query).get('url', [link])[0]
        if not permitted_link(link):
            continue
        published = item.findtext('pubDate')
        if published:
            try:
                dt = email.utils.parsedate_to_datetime(published).astimezone(timezone.utc)
                if dt < datetime.now(timezone.utc) - timedelta(days=45):
                    continue
            except (TypeError, ValueError, OverflowError):
                pass
        items.append((title, summary, link))
    return items


def page_text(session, url):
    response = session.get(url, timeout=(4, 7), allow_redirects=True, headers=HEADERS)
    response.raise_for_status()
    if not permitted_link(response.url) or 'text/html' not in response.headers.get('content-type', '').lower():
        return ''
    body = response.text[:350000]
    body = re.sub(r'(?is)<(script|style|nav|footer|header)\b[^>]*>.*?</\1>', ' ', body)
    return html.unescape(re.sub(r'(?s)<[^>]+>', ' ', body))[:100000]


def application_emails(text):
    valid = []
    for found in EMAIL.finditer(text):
        address = found.group().lower().strip('.;,')
        context = text[max(0, found.start() - 220):found.end() + 130]
        if APPLICATION_WORDS.search(context) and not address.endswith(('.png', '.jpg', '.webp')):
            if address not in valid:
                valid.append(address)
    return valid[:2]


def main(account_id, max_queries):
    uid = -account_id
    with sqlite3.connect(DB, timeout=30) as db:
        db.row_factory = sqlite3.Row
        profile = db.execute('SELECT desired_positions, preferred_locations FROM android_profiles WHERE account_id=?', (account_id,)).fetchone()
        if not profile:
            print('SKIP: Profil akun tidak ditemukan')
            return
        positions = [str(p).strip() for p in json.loads(profile['desired_positions'] or '[]') if str(p).strip()]
        locations = [str(l).strip() for l in json.loads(profile['preferred_locations'] or '[]') if str(l).strip()]
        if not positions or not locations:
            print('SKIP: Posisi/lokasi belum dipilih')
            return
        columns = {r['name'] for r in db.execute('PRAGMA table_info(phase57_queue)')}
        mandatory = {'user_id', 'job_title', 'email', 'location', 'kind', 'status'}
        if not mandatory <= columns:
            raise RuntimeError('Kolom antrean tidak cocok; hentikan tanpa menulis')
        existing = {folded(r['email']): dict(r) for r in db.execute('SELECT id,email,kind,status FROM phase57_queue WHERE user_id=?', (uid,))}
        # Every run checks primary location; other positions/locations rotate daily.
        day = datetime.now(timezone.utc).toordinal()
        other_places = locations[1:] or locations
        pairs = [(positions[day % len(positions)], locations[0])]
        pairs.extend((positions[(day+i+1) % len(positions)], other_places[(day+i) % len(other_places)])
                     for i in range(max_queries - 1))
        added = upgraded = scanned = errors = 0
        seen_urls = set()
        with requests.Session() as session:
            session.headers.update(HEADERS)
            for position, location in pairs[:max_queries]:
                phrase = f'lowongan kerja "{position}" "{location}" "email"'
                try:
                    items = read_feed(session, phrase)
                except (requests.RequestException, ET.ParseError) as exc:
                    errors += 1
                    print('RSS gagal:', location, position, type(exc).__name__)
                    continue
                for title, summary, url in items[:5]:
                    if url in seen_urls or not match(title + ' ' + summary, positions) or not match(title + ' ' + summary, locations):
                        continue
                    seen_urls.add(url)
                    scanned += 1
                    try:
                        body = page_text(session, url)
                    except requests.RequestException:
                        continue
                    text = title + ' ' + summary + ' ' + body[:40000]
                    if not match(text, locations) or not match(text, positions):
                        continue
                    if not re.search(r'(?i)lowongan|loker|rekrut|recruit|hiring|vacancy|dibutuhkan', title + ' ' + summary):
                        continue
                    emails = application_emails(body or summary)
                    if not emails:
                        continue
                    matched_position = next((p for p in positions if match(title + ' ' + summary, [p])), None)
                    if not matched_position:
                        continue
                    matched_location = next((l for l in locations if match(title + ' ' + summary, [l])), None)
                    if not matched_location:
                        continue
                    company_match = re.search(r'\bPT\.?\s+[\w .&-]{3,65}', title, re.I)
                    company = company_match.group(0).strip() if company_match else 'Perusahaan (lihat lowongan)'
                    for address in emails:
                        data = {'user_id':uid, 'job_title':matched_position, 'company':company,
                                'location':matched_location, 'email':address, 'job_url':url,
                                'kind':'vacancy', 'status':'ready', 'contact_source':'public-rss:bing',
                                'created_at':datetime.now(timezone.utc).isoformat()}
                        old = existing.get(address)
                        if old:
                            if old['kind'] == 'spontaneous' and old['status'] == 'ready':
                                edits = {k:v for k,v in data.items() if k in columns and k not in ('user_id','email','status','created_at')}
                                if edits:
                                    db.execute('UPDATE phase57_queue SET ' + ','.join(f'{k}=?' for k in edits) + ' WHERE id=?', (*edits.values(), old['id']))
                                    existing[address]['kind'] = 'vacancy'
                                    upgraded += 1
                            continue
                        payload = {k:v for k,v in data.items() if k in columns}
                        try:
                            db.execute('INSERT INTO phase57_queue ('+','.join(payload)+') VALUES ('+','.join('?' for _ in payload)+')', tuple(payload.values()))
                        except sqlite3.IntegrityError:
                            continue
                        existing[address] = {'kind':'vacancy','status':'ready','email':address}
                        added += 1
                    db.commit()  # Preserve found jobs if scheduled timeout interrupts later searches.
                    if added + upgraded >= 10:
                        break
                if added + upgraded >= 10:
                    break
        db.commit()
    print('=== PUBLIC JOB DISCOVERY ===')
    print('Akun:', account_id, '| Query:', len(pairs[:max_queries]), '| Artikel:', scanned, '| Error RSS:', errors)
    print('Lowongan antre baru:', added, '| Katalog ditingkatkan:', upgraded)
    print('Email dikirim sekarang: 0')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--account-id', type=int, required=True)
    parser.add_argument('--max-queries', type=int, default=6)
    args = parser.parse_args()
    if args.account_id <= 0 or not 1 <= args.max_queries <= 20:
        parser.error('Account ID atau jumlah query tidak valid')
    main(args.account_id, args.max_queries)
