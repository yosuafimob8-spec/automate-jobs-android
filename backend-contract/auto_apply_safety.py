"""Auto Apply safety boundary for integration with the missing Gmail backend.

No HTTP routes, scheduler, OAuth credentials, or live sender are instantiated here.
The production backend must call this gate around its authenticated Gmail adapter.
"""
from __future__ import annotations

from contextlib import contextmanager
from datetime import datetime
from email.utils import parseaddr
from pathlib import Path
import sqlite3
import uuid
from zoneinfo import ZoneInfo

DAILY_LIMIT = 5


def select_candidates(candidates, *, positions, locations, now):
    """Only use stored contacts and explicit vacancy evidence. No invented fallback."""
    positions = [p.strip().casefold() for p in positions if p.strip()]
    locations = [p.strip().casefold() for p in locations if p.strip()]
    if not positions or not locations:
        return []
    ranked = []
    for c in candidates:
        email = c.get('email', '').strip().casefold()
        if not c.get('company_id') or not c.get('contact_source'):
            continue
        if parseaddr(email)[1] != email or '@' not in email or any(ch.isspace() for ch in email):
            continue
        title, location = c.get('position', '').casefold(), c.get('location', '').casefold()
        if not any(p in title for p in positions) or not any(loc in location for loc in locations):
            continue
        spontaneous = c.get('kind') == 'spontaneous'
        active = (c.get('kind') == 'vacancy' and c.get('active') is True
                  and c.get('vacancy_verified') is True and bool(c.get('evidence_url'))
                  and c.get('expires_at', 0) > now)
        if not active and not spontaneous:
            continue
        ranked.append((1 if spontaneous else 0, c.get('company_id'), email, c))
    result, seen = [], set()
    for _, _, email, c in sorted(ranked, key=lambda row: row[:3]):
        if email not in seen:
            seen.add(email)
            result.append(dict(c, email=email, vacancy_verified=bool(c.get('vacancy_verified')) if c['kind'] == 'vacancy' else False))
        if len(result) == DAILY_LIMIT:
            break
    return result


class AutoApplyGate:
    def __init__(self, database: str | Path):
        self.database = str(database)
        with self._db() as db:
            db.executescript('''
                CREATE TABLE IF NOT EXISTS consent(subject TEXT PRIMARY KEY, granted_at INTEGER NOT NULL);
                CREATE TABLE IF NOT EXISTS deliveries(
                    id TEXT PRIMARY KEY, subject TEXT NOT NULL, recipient TEXT NOT NULL,
                    day TEXT NOT NULL, status TEXT NOT NULL, gmail_id TEXT, error TEXT,
                    UNIQUE(subject,recipient));
            ''')

    @contextmanager
    def _db(self):
        db = sqlite3.connect(self.database, timeout=10)
        db.row_factory = sqlite3.Row
        try:
            with db:
                yield db
        finally:
            db.close()

    def consent(self, subject, *, explicit, now):
        if explicit is not True or not subject:
            raise ValueError('Persetujuan eksplisit diperlukan')
        with self._db() as db:
            db.execute('INSERT OR IGNORE INTO consent VALUES(?,?)', (subject, int(now)))

    def reserve(self, subject, recipient, *, now):
        recipient = recipient.strip().casefold()
        if parseaddr(recipient)[1] != recipient or '@' not in recipient or any(c.isspace() for c in recipient):
            raise ValueError('Alamat email tidak valid')
        day = datetime.fromtimestamp(now, ZoneInfo('Asia/Jakarta')).date().isoformat()
        with self._db() as db:
            db.execute('BEGIN IMMEDIATE')
            if not db.execute('SELECT 1 FROM consent WHERE subject=?', (subject,)).fetchone():
                raise PermissionError('Persetujuan belum tersedia')
            if db.execute('SELECT 1 FROM deliveries WHERE subject=? AND recipient=?', (subject, recipient)).fetchone():
                raise ValueError('Lamaran duplikat')
            used = db.execute("SELECT COUNT(*) FROM deliveries WHERE subject=? AND day=? AND status IN ('reserved','unknown','sent')", (subject, day)).fetchone()[0]
            if used >= DAILY_LIMIT:
                raise ValueError('Batas 5 lamaran per hari tercapai')
            delivery_id = uuid.uuid4().hex
            db.execute("INSERT INTO deliveries VALUES(?,?,?,?,'reserved',NULL,NULL)", (delivery_id, subject, recipient, day))
            return delivery_id

    def send(self, subject, recipient, *, now, gmail_sender):
        """Caller supplies an authenticated sender. Unknown outcomes are never retried.

        Any exception reserves the slot, including expired-token and transport errors.
        This conservative rule prevents duplicate delivery after ambiguous timeouts.
        """
        delivery_id = self.reserve(subject, recipient, now=now)
        try:
            response = gmail_sender(recipient)
            message_id = response.get('id') if isinstance(response, dict) else None
            if not isinstance(message_id, str) or not message_id.strip():
                raise ValueError('Gmail belum mengonfirmasi message ID')
        except Exception as exc:
            with self._db() as db:
                db.execute("UPDATE deliveries SET status='unknown',error=? WHERE id=?", (type(exc).__name__, delivery_id))
            raise
        with self._db() as db:
            db.execute("UPDATE deliveries SET status='sent',gmail_id=? WHERE id=?", (message_id, delivery_id))
        return delivery_id

    def history(self, subject):
        with self._db() as db:
            return [dict(row) for row in db.execute('SELECT * FROM deliveries WHERE subject=? ORDER BY day,id', (subject,))]
