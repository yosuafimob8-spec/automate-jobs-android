"""KarirMate V73: region-first public-contact selection, no sending/network.

The catalog is a list of public *contacts*, not proof of vacancies or delivery.
Company name / email are deduplicated for each account, across all queue states.
"""
import json
import re
import sqlite3
import unicodedata
from urllib.parse import urlparse

EMAIL = re.compile(r"^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+(?:\.[A-Za-z0-9-]+)+$")
SKIP_DOMAINS = {"test", "invalid", "example", "localhost"}
BLOCKED_HOSTS = {"glints.com", "indeed.com", "jobstreet.com", "jobstreet.co.id", "linkedin.com", "kalibrr.com", "dealls.com"}
WARNING_WORDS = ("hoaks", "hoax", "penipuan", "scam", "palsu", "fraud")


def normalize(value):
    value = unicodedata.normalize('NFKC', str(value or '')).casefold()
    value = re.sub(r"[^\w]+", " ", value, flags=re.UNICODE)
    return ' '.join(value.split())


def company_key(name):
    name = re.sub(r"(?i)\((?:unit|cabang|branch|plant|site)\b[^)]*\)", " ", str(name or ''))
    name = re.split(r"(?i)\s+(?:-|–|—)\s+(?:cabang|unit|plant|site)\b", name)[0]
    parts = normalize(name).split()
    while parts and parts[0] in ('pt', 'cv', 'ud', 'pd'):
        parts.pop(0)
    while parts and parts[-1] in ('tbk', 'persero'):
        parts.pop()
    return ' '.join(parts)


def valid_email(value):
    email = str(value or '').strip().lower()
    if not EMAIL.fullmatch(email) or len(email) > 254:
        return False
    host = email.rsplit('@', 1)[1]
    return host.rsplit('.', 1)[-1] not in SKIP_DOMAINS and not any(
        host == x or host.endswith('.' + x) for x in BLOCKED_HOSTS)


def valid_source(value, note=''):
    value = str(value or '').strip()
    parsed = urlparse(value)
    if parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.password:
        return False
    if any(word in (value + ' ' + str(note or '')).casefold() for word in WARNING_WORDS):
        return False
    return True


def locations_match(area, choice):
    """Location-only match with the Android picker semantics; never cross areas."""
    a, c = normalize(area), normalize(choice)
    if not a or not c:
        return False
    tangsel = any(word in a for word in (
        'tangerang selatan', 'tangsel', 'bsd', 'serpong', 'pamulang', 'ciputat'))
    if c in ('jabodetabek', 'seluruh jabodetabek'):
        return any(re.search(r'\b' + re.escape(x) + r'\b', a) for x in
                   ('jakarta', 'bogor', 'depok', 'tangerang', 'bekasi', 'cikarang'))
    if c == 'tangerang selatan':
        return tangsel
    if c == 'tangerang':
        return not tangsel and any(re.search(r'\b' + re.escape(x) + r'\b', a) for x in
                                   ('tangerang', 'karawaci', 'cikupa', 'balaraja'))
    if c == 'bekasi':
        return 'bekasi' in a or 'cikarang' in a
    if c == 'cikarang':
        return 'cikarang' in a
    if c.startswith(('kabupaten ', 'kota ')):
        if c == 'kota tangerang' and tangsel:
            return False
        return bool(re.search(r'\b' + re.escape(c) + r'\b', a))
    return bool(re.search(r'\b' + re.escape(c) + r'\b', a))


def decode_choices(value):
    try:
        parsed = json.loads(value or '[]')
    except (TypeError, ValueError):
        return []
    return [str(x).strip() for x in parsed if isinstance(x, str) and x.strip()] if isinstance(parsed, list) else []


def profile(db, account_id):
    row = db.execute('SELECT desired_positions,preferred_locations FROM android_profiles WHERE account_id=?', (account_id,)).fetchone()
    if not row:
        return None, []
    positions, places = decode_choices(row[0]), decode_choices(row[1])
    return (positions[0] if positions else None), places


def _table_exists(db, name):
    return bool(db.execute('SELECT 1 FROM sqlite_master WHERE type=\'table\' AND name=?', (name,)).fetchone())


def _blocked(db, uid):
    emails, companies = set(), set()
    for company, email in db.execute('SELECT company,email FROM phase57_queue WHERE user_id=?', (uid,)):
        if email:
            emails.add(str(email).strip().casefold())
        if company and company_key(company):
            companies.add(company_key(company))
    if _table_exists(db, 'phase57_delivery_claims'):
        emails.update(str(x[0]).strip().casefold() for x in db.execute(
            'SELECT email FROM phase57_delivery_claims WHERE user_id=?', (uid,)) if x[0])
    return emails, companies


def candidates(db, uid, account_id, limit=100, include_queued=False):
    """Deterministic candidate selection. Read-only; no internet calls or queue changes."""
    if not _table_exists(db, 'km_hr_public_contacts'):
        return [], 'Katalog perusahaan belum tersedia.'
    role, places = profile(db, account_id)
    if not role or not places:
        return [], 'Isi posisi untuk surat lamaran dan wilayah target.'
    blocked_emails, blocked_companies = _blocked(db, uid)
    existing_emails, existing_companies = set(), set()
    options = []
    for company, email, category, area, source, year, note in db.execute('''
            SELECT company,email,category,area,source_url,source_year,source_note
            FROM km_hr_public_contacts'''):
        company, email = str(company or '').strip(), str(email or '').strip().casefold()
        ck = company_key(company)
        if not ck or not valid_email(email) or not valid_source(source, note):
            continue
        if (email in blocked_emails or ck in blocked_companies or
                email in existing_emails or ck in existing_companies):
            continue
        place_rank = next((i for i, p in enumerate(places) if locations_match(area, p)), None)
        if place_rank is None:
            continue
        # Reuse the catalog's source URL as a per-company queue key and proof of provenance.
        hr = 'hr' in str(category).casefold() or 'rekrut' in str(category).casefold() or 'lamaran' in str(category).casefold()
        yr = int(year) if str(year or '').isdigit() else 0
        options.append(((place_rank, not hr, -yr, ck, email), {
            'company': company, 'email': email, 'position': role, 'location': str(area).strip(),
            'kind': 'spontaneous', 'source_url': str(source).strip(), 'contact_source': str(source).strip(),
            'vacancy_verified': False, 'active': False,
        }))
    options.sort(key=lambda item: item[0])
    chosen = []
    for _, c in options:
        ck = company_key(c['company'])
        if c['email'] in existing_emails or ck in existing_companies:
            continue
        existing_emails.add(c['email'])
        existing_companies.add(ck)
        chosen.append(c)
        if len(chosen) >= max(0, limit):
            break
    return chosen, f'Kandidat berdasarkan wilayah: {len(chosen)}. Posisi hanya untuk surat lamaran spontan; bukan lowongan terverifikasi.'


def approved_queue_row(db, item, locations):
    """Reject legacy guessed rows: only exact catalog company/email/source/region."""
    if str(item.get('kind')) != 'spontaneous' or not valid_email(item.get('email')):
        return False
    if not _table_exists(db, 'km_hr_public_contacts'):
        return False
    company = company_key(item.get('company'))
    email = str(item.get('email') or '').strip().casefold()
    source = str(item.get('contact_source') or '').strip()
    if not company or not source:
        return False
    for cname, area, src, note in db.execute('''
            SELECT company,area,source_url,source_note FROM km_hr_public_contacts
            WHERE lower(trim(email))=?''', (email,)):
        if (company_key(cname) == company and str(src or '').strip() == source
                and valid_source(src, note) and any(locations_match(area, p) for p in locations)
                and any(locations_match(item.get('location'), p) for p in locations)):
            return True
    return False


def already_attempted_company(db, uid, name, current_id):
    ck = company_key(name)
    for item_id, other_name, status in db.execute('''
            SELECT id,company,status FROM phase57_queue
            WHERE user_id=? AND id!=? AND status IN ('sent','bounced','sending','uncertain')''', (uid, current_id)):
        if company_key(other_name) == ck:
            return True
    return False


def refill(db_path, uid, batch=10):
    """Stage catalog contacts; never send. Uses SQLite BEGIN IMMEDIATE to avoid double inserts."""
    if uid >= 0:
        raise ValueError('This catalog integration handles KarirMate account IDs (negative uid) only')
    with sqlite3.connect(db_path, timeout=30) as db:
        db.execute('BEGIN IMMEDIATE')
        _, places = profile(db, -uid)
        db.row_factory = sqlite3.Row
        pending = [dict(x) for x in db.execute(
            "SELECT * FROM phase57_queue WHERE user_id=? AND status='ready'", (uid,))]
        # Legacy guesses and out-of-region rows MUST NOT consume new catalog slots.
        ready = sum(approved_queue_row(db, item, places) for item in pending)
        if ready >= batch:
            return 0
        choices, _ = candidates(db, uid, -uid, batch - ready)
        added = 0
        for c in choices:
            cursor = db.execute('''INSERT OR IGNORE INTO phase57_queue
              (user_id,job_url,job_title,company,email,status,kind,location,contact_source)
              VALUES (?,?,?,?,?,'ready','spontaneous',?,?)''',
              (uid, c['source_url'], c['position'], c['company'], c['email'], c['location'], c['contact_source']))
            added += max(0, cursor.rowcount)
        return added
