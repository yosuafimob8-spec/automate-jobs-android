"""AutoMate AI: local-only import/search of ID-Jobs exported JSON.
No paid API or scraper calls. Pilot use; check upstream redistribution permission.
"""
import json
import re
import sqlite3
import sys
import time
from datetime import datetime
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit

from config import ROOT

DB = ROOT / 'automate.db'
SOURCES = {'dealls.com': 'Dealls', 'kalibrr.com': 'Kalibrr'}


def connection():
    c = sqlite3.connect(DB, timeout=20)
    c.row_factory = sqlite3.Row
    c.execute('PRAGMA busy_timeout=20000')
    return c


def init_feed():
    with connection() as c:
        c.execute('''CREATE TABLE IF NOT EXISTS idjobs_feed (
           url TEXT PRIMARY KEY, title TEXT NOT NULL, company TEXT NOT NULL,
           location TEXT NOT NULL, source TEXT NOT NULL,
           scraped_at TEXT NOT NULL, imported_at INTEGER NOT NULL
        )''')
        c.execute('CREATE INDEX IF NOT EXISTS idx_idjobs_source ON idjobs_feed(source)')


def tidy(value, limit=220):
    if value is None: return ''
    if isinstance(value, dict):
        value = value.get('name') or value.get('title') or ''
    if not isinstance(value, (int, float, str)): return ''
    return re.sub(r'\s+', ' ', str(value)).strip()[:limit]


def canonical_url(url):
    if not isinstance(url, str): return ('', '')
    try:
        s = urlsplit(url.strip())
        domain = (s.hostname or '').lower()
        label = next((name for root,name in SOURCES.items()
                      if domain == root or domain.endswith('.'+root)), '')
        if s.scheme != 'https' or not label or not s.path.strip('/'):
            return ('', '')
        # Drop fragment, retain query because some job sites identify roles by it
        return (urlunsplit(('https',s.netloc.lower(),s.path,s.query,'')),label)
    except ValueError:
        return ('', '')


def import_files(paths):
    init_feed()
    counts = {'read': 0, 'valid': 0, 'invalid': 0}
    at = int(time.time())
    with connection() as conn:
        for path in paths:
            raw = json.loads(Path(path).read_text(encoding='utf-8'))
            if not isinstance(raw, list):
                raise ValueError(f'{path}: hasil bukan daftar JSON')
            for item in raw:
                counts['read'] += 1
                if not isinstance(item, dict):
                    counts['invalid'] += 1
                    continue
                url, source = canonical_url(item.get('job_url') or item.get('url'))
                title = tidy(item.get('job_title') or item.get('title'))
                location = tidy(item.get('job_location') or item.get('location'))
                company = tidy(item.get('company') or item.get('company_name')
                               or item.get('job_company') or item.get('employer'))
                seen = tidy(item.get('first_seen') or '', 60)
                if not url or not title or not location:
                    counts['invalid'] += 1
                    continue
                conn.execute('''INSERT INTO idjobs_feed
                  (url,title,company,location,source,scraped_at,imported_at)
                  VALUES(?,?,?,?,?,?,?)
                  ON CONFLICT(url) DO UPDATE SET
                   title=excluded.title,company=excluded.company,
                   location=excluded.location,source=excluded.source,
                   scraped_at=excluded.scraped_at,imported_at=excluded.imported_at''',
                  (url,title,company or 'Tidak tercantum',location,
                   source,seen,at))
                counts['valid'] += 1
    counts['stored'] = stats()['total']
    return counts


CITY = {
    'Tangerang': ['tangerang', 'serpong', 'bsd', 'cikupa', 'balaraja', 'karawaci', 'alam sutera', 'gading serpong'],
    'Bogor': ['bogor', 'cibinong', 'cileungsi', 'sentul', 'gunung putri'],
    'Depok': ['depok', 'cinere', 'sawangan'],
    'Jakarta': ['jakarta'],
    'Bekasi': ['bekasi', 'cikarang', 'tambun', 'cibitung'],
}
TOPICS = {
    'Warehouse': ['warehouse', 'gudang', 'inventory', 'stock keeper', 'storekeeper', 'stock control', 'pergudangan'],
    'Logistic': ['logistic', 'logistik', 'supply chain', 'distribution', 'distribusi', 'transportasi', 'ekspedisi'],
    'Operational': ['operational', 'operations', 'operasional'],
}


def contains(text, patterns):
    return any(p in text for p in patterns)


LEVEL_SUP = ('supervisor', 'supervisi', 'spv', 'team leader', 'team lead',
             'koordinator', 'coordinator', 'foreman')
LEVEL_HIGH = ('manager', 'head of', 'director', 'direktur', 'vp ', 'chief ')
LEVEL_JUNIOR = ('intern', 'magang', 'internship')
WAREHOUSE = ('warehouse', 'gudang', 'inventory', 'stock keeper', 'storekeeper',
             'stock control', 'pergudangan', 'inbound', 'outbound')
LOGISTICS = ('logistic', 'logistik', 'supply chain', 'distribution', 'distribusi',
             'transportasi', 'ekspedisi', 'freight', 'shipping')
OPERATIONS = ('operational', 'operations', 'operasional')
BACKOFFICE = ('procurement', 'purchasing', 'pembelian', 'administrasi', 'administration')


def position_tier(title, position):
    """1 exact-ish role family; 2 related, same seniority; 0 not suitable.

    Classification is based on title only; not an employment verification.
    """
    t = tidy(title, 400).casefold()
    p = position.casefold()
    senior = contains(t, LEVEL_SUP)
    managerial = contains(t, LEVEL_HIGH)
    junior = contains(t, LEVEL_JUNIOR)
    if managerial or junior:
        return 0
    if p.startswith('supervisor'):
        if not senior:
            return 0
    elif senior:
        return 0

    wh = contains(t, WAREHOUSE)
    lg = contains(t, LOGISTICS)
    op = contains(t, OPERATIONS)
    adm = contains(t, ('admin', 'administrasi', 'administration', 'administrative', 'clerk'))
    bo = contains(t, BACKOFFICE)

    if 'warehouse' in p:
        if wh:
            if p.startswith('admin'):
                return 1 if adm else 2
            if p.startswith('staff'):
                return 1 if contains(t, ('warehouse', 'gudang')) else 2
            return 1 if contains(t, ('warehouse', 'gudang')) else 2
        return 0

    if 'logistic' in p:
        if lg:
            if p.startswith('admin'):
                return 1 if adm else 2
            return 1 if contains(t, ('logistic', 'logistik')) else 2
        # Inventory / warehouse operations are related, not equivalent
        return 2 if wh else 0

    if 'operational' in p:
        if op and adm:
            return 1
        if adm and bo:
            return 2
        if op and not p.startswith('admin'):
            return 2
        return 0
    return 0


def position_match(title, position):
    return position_tier(title, position) > 0


def location_match(actual, location):
    candidates = CITY.get(location, [location.casefold()])
    a = ' ' + tidy(actual, 300).casefold() + ' '
    return contains(a,candidates)


def fetch_feed(position, location, limit=12):
    init_feed()
    with connection() as c:
        rows = c.execute('''SELECT * FROM idjobs_feed ORDER BY imported_at DESC, url LIMIT 15000''').fetchall()
    matches = []
    for row in rows:
        if not location_match(row['location'], location):
            continue
        tier = position_tier(row['title'], position)
        if not tier:
            continue
        seen = row['scraped_at'][:10] if row['scraped_at'] else 'Tidak diketahui'
        matches.append((tier, {
            'title':row['title'], 'company':row['company'],
            'location':row['location'], 'position':position,
            'link':row['url'], 'source':row['source'] + ' (ID-Jobs)',
            'source_link':'', 'updated':seen, 'remote':False,
            'expiry_ts':0,
            'match_label': '✅ Sesuai jabatan' if tier == 1 else '🔄 Alternatif terkait',
        }))
    # Exact matches always rank before related titles; location never silently expands.
    matches.sort(key=lambda pair: pair[0])
    return [item for _, item in matches[:limit]]


def stats():
    init_feed()
    with connection() as c:
        count = c.execute('SELECT COUNT(*) FROM idjobs_feed').fetchone()[0]
        by = [dict(r) for r in c.execute('SELECT source,COUNT(*) AS n FROM idjobs_feed GROUP BY source')]
    return {'total':count,'sources':by}


if __name__ == '__main__':
    if len(sys.argv)>1 and sys.argv[1]=='import':
        print(json.dumps(import_files(sys.argv[2:]),ensure_ascii=False,indent=2))
    elif len(sys.argv)>1 and sys.argv[1]=='stats':
        print(json.dumps(stats(),ensure_ascii=False,indent=2))
    else:
        print('Usage: python idjobs_feed.py import FILE.json ... | stats')

# SMART_ROLE_FALLBACK_V3
# Memperluas sinonim jabatan tanpa memperluas lokasi.
import re as _role_re

_original_position_tier = position_tier

def position_tier(title, position):
    original = _original_position_tier(title, position)
    if original:
        return original

    t = str(title).casefold()
    p = str(position).casefold()

    def has(*words):
        return any(
            _role_re.search(r"\b" + _role_re.escape(w) + r"\b", t)
            for w in words
        )

    managerial = has(
        "manager", "head", "director", "direktur",
        "supervisor", "koordinator", "coordinator", "lead"
    )

    internship = has("intern", "internship", "magang")

    warehouse = has(
        "warehouse", "gudang", "inventory",
        "stock", "stok"
    )

    logistics = has(
        "logistic", "logistics", "logistik",
        "supply chain", "distribution", "distribusi"
    )

    operational = has(
        "operation", "operations", "operational",
        "operasional"
    )

    if p == "admin operational":
        if not managerial and not internship:
            if has("admin", "administrator", "administration") and operational:
                return 1
            if has("operational support", "operations support"):
                return 2

    elif p == "admin warehouse":
        if not managerial and not internship and warehouse:
            if has("admin", "administration", "administrator"):
                return 1
            if has("officer", "staff", "staf"):
                return 2

    elif p == "admin logistic":
        if not managerial and not internship and logistics:
            if has("admin", "administration", "administrator"):
                return 1
            if has("officer", "staff", "staf"):
                return 2

    elif p == "staff warehouse":
        if not managerial and not internship and warehouse:
            if has("staff", "staf", "helper", "operator"):
                return 1
            if has("officer", "associate"):
                return 2

    elif p == "staff logistic":
        if not managerial and not internship:
            if logistics and has("staff", "staf", "officer", "associate"):
                return 1
            if warehouse and has("inventory", "stock"):
                return 2

    elif p == "supervisor warehouse":
        if has("supervisor", "koordinator", "coordinator") and warehouse:
            return 1
        if has("lead") and warehouse:
            return 2

    elif p == "supervisor logistic":
        if has("supervisor", "koordinator", "coordinator") and logistics:
            return 1
        if has("lead") and logistics:
            return 2

    return 0

# ASHBY_DOMAIN_SUPPORT
# Tambahkan dukungan URL Ashby tanpa mengubah sumber lain.
from urllib.parse import urlsplit as _ashby_urlsplit

_previous_canonical_url = canonical_url

def canonical_url(url):
    original_url, source = _previous_canonical_url(url)

    if original_url:
        return original_url, source

    if not isinstance(url, str):
        return None, None

    parsed = _ashby_urlsplit(url.strip())

    if (
        parsed.scheme == "https"
        and parsed.hostname == "jobs.ashbyhq.com"
        and parsed.path.strip("/")
    ):
        return url.strip(), "Ashby"

    return original_url, source


# KARIRMATE_ROLE_MATCH_V65
# Safety guard for known permissive legacy role matching.
# This gate only rejects unsuitable titles; it never turns a failed legacy
# match into an accepted vacancy.
import re as _km65_re
_km65_previous_position_tier = position_tier

def position_tier(title, position):
    name = str(title or '').casefold().strip()
    requested = str(position or '').casefold().strip()
    if not name or not requested:
        return 0

    # Data-warehouse/technology roles are not physical warehouse roles.
    if _km65_re.search(
        r'\b(data\s+warehouse|software|developer|programmer|'
        r'data\s+engineer|data\s+scientist|machine\s+learning|'
        r'warehouse\s+(?:engineer|architect|analyst)|'
        r'it\s+engineer|system\s+engineer)\b', name
    ):
        return 0

    # Do not silently cross seniority or select unrelated commercial roles.
    if _km65_re.search(r'\b(intern|internship|magang|manager|director|direktur|chief|head\s+of)\b', name):
        return 0
    is_supervisor_title = bool(_km65_re.search(
        r'\b(supervisor|supervisi|spv|coordinator|koordinator|team\s+lead|team\s+leader)\b', name
    ))
    wants_supervisor = requested.startswith('supervisor')
    if is_supervisor_title != wants_supervisor:
        return 0

    warehouse = bool(_km65_re.search(
        r'\b(warehouse|gudang|pergudangan|inventory|inventori|stock|stok|stockroom|fulfilment|fulfillment)\b', name
    ))
    logistics = bool(_km65_re.search(
        r'\b(logistic|logistics|logistik|supply\s+chain|distribusi|distribution|'
        r'freight|transportasi|transportation|shipping|ekspedisi)\b', name
    ))
    operations = bool(_km65_re.search(
        r'\b(operational|operations|operation|operasional|operasi)\b', name
    ))
    if 'warehouse' in requested:
        if not warehouse:
            return 0
    elif 'logistic' in requested or 'logistik' in requested:
        if not logistics:
            return 0
    elif 'operational' in requested or 'operasional' in requested:
        if not operations:
            return 0
    else:
        return 0

    return _km65_previous_position_tier(title, position)


# KARIRMATE_ROLE_SENIORITY_V67
# Do not report management/head-of-department vacancies as admin or staff jobs.
# This sits on top of the existing V65 match guard; rejects only.
import re as _km67_re
_km67_previous_position_tier = position_tier

def position_tier(title, position):
    name = _km67_re.sub(r'\s+', ' ', str(title or '').casefold()).strip()
    requested = str(position or '').casefold().strip()
    if not name or not requested:
        return 0

    # "Head Office" describes a location rather than the job holder's level.
    level_text = _km67_re.sub(r'\bhead[\s-]+office\b', ' ', name)

    # Position families supported by the KarirMate UI only extend to supervisor.
    # A Warehouse Head, Kepala Gudang or Manager is NOT an Admin Warehouse job.
    if _km67_re.search(
        r'\b(?:head|kepala|manager|manajer|chief|director|direktur|'
        r'vice[\s-]+president|president|pimpinan|general[\s-]+manager)\b',
        level_text,
    ):
        return 0

    # Lead/coordinator/foreman roles are not staff/admin positions.
    # Supervisor searches continue to rely on the existing V65 seniority logic.
    wants_supervisor = requested.startswith('supervisor')
    if not wants_supervisor and _km67_re.search(
        r'\b(?:lead|leader|foreman|supervisor|spv|supervisi|'
        r'coordinator|koordinator)\b',
        level_text,
    ):
        return 0

    return _km67_previous_position_tier(title, position)
