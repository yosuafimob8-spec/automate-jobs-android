"""Persistent, transactional queue. Migrations only add columns/tables; no deletes."""
import sqlite3
from runtime_paths import DB

def initialize():
    with sqlite3.connect(DB, timeout=30) as db:
        db.execute('BEGIN IMMEDIATE')
        db.execute('''CREATE TABLE IF NOT EXISTS phase57_queue (
            id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER NOT NULL,
            job_url TEXT NOT NULL, job_title TEXT NOT NULL DEFAULT '', company TEXT NOT NULL DEFAULT '',
            email TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'queued', created_at TEXT DEFAULT CURRENT_TIMESTAMP,
            sent_at TEXT, gmail_message_id TEXT, error TEXT, UNIQUE(user_id,job_url,email))''')
        db.execute('''CREATE TABLE IF NOT EXISTS phase57_settings (
            user_id INTEGER PRIMARY KEY, enabled INTEGER NOT NULL DEFAULT 0,
            daily_limit INTEGER NOT NULL DEFAULT 5, consent_at TEXT)''')
        existing = {row[1] for row in db.execute('PRAGMA table_info(phase57_queue)')}
        additions = {'attempt_day':'TEXT', 'claimed_at':'TEXT', 'job_key':'TEXT',
                     'kind':"TEXT NOT NULL DEFAULT 'legacy_unverified'", 'location':"TEXT NOT NULL DEFAULT ''",
                     'evidence_url':"TEXT NOT NULL DEFAULT ''", 'expires_at':'INTEGER',
                     'contact_source':"TEXT NOT NULL DEFAULT ''"}
        for name, definition in additions.items():
            if name not in existing: db.execute(f'ALTER TABLE phase57_queue ADD COLUMN {name} {definition}')
        db.execute('CREATE INDEX IF NOT EXISTS idx_phase57_status ON phase57_queue(user_id,status)')
        db.execute('''CREATE TABLE IF NOT EXISTS phase57_delivery_claims (
            user_id INTEGER NOT NULL, email TEXT NOT NULL, queue_id INTEGER NOT NULL,
            PRIMARY KEY(user_id,email), UNIQUE(queue_id))''')
        # Existing sending/uncertain rows remain blocked even when their attempt date is unknown.
        db.execute('''INSERT OR IGNORE INTO phase57_delivery_claims(user_id,email,queue_id)
                      SELECT user_id,lower(trim(email)),min(id) FROM phase57_queue
                      WHERE status IN ('sent','sending','uncertain') GROUP BY user_id,lower(trim(email))''')

def enqueue(uid, candidate):
    initialize()
    kind = candidate.get('kind')
    if kind not in ('vacancy', 'spontaneous') or not candidate.get('contact_source'):
        raise ValueError('Kandidat memerlukan sumber kontak dan jenis peluang yang jelas.')
    if kind == 'vacancy' and not (candidate.get('vacancy_verified') is True and candidate.get('active') is True and candidate.get('evidence_url') and candidate.get('expires_at')):
        raise ValueError('Lowongan belum terverifikasi.')
    if not candidate.get('location') or not candidate.get('position'):
        raise ValueError('Lokasi/posisi kandidat belum tersedia.')
    with sqlite3.connect(DB, timeout=30) as db:
        db.execute('''INSERT OR IGNORE INTO phase57_queue
            (user_id,job_url,job_title,company,email,status,kind,location,evidence_url,expires_at,contact_source)
            VALUES(?,?,?,?,?,'ready',?,?,?,?,?)''', (uid,candidate['source_url'],candidate['position'],candidate['company'],candidate['email'].strip().lower(),
                kind,candidate['location'],candidate.get('evidence_url',''),candidate.get('expires_at'),candidate['contact_source']))

if __name__ == '__main__':
    initialize()
