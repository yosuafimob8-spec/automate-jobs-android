"""Authenticated email verification for KarirMate (OTP, not self-attestation)."""
from __future__ import annotations

import hashlib
import hmac
import os
from pathlib import Path
import re
import secrets
import smtplib
import sqlite3
import ssl
import time
from email.message import EmailMessage

from runtime_paths import DB

CODE_LIFETIME = 600
MIN_RESEND_SECONDS = 60
MAX_SENDS_24H = 8
MAX_ATTEMPTS = 5
SMTP_CONFIG = Path.home() / ".karirmate-smtp"
SECRET_FILE = Path.home() / ".karirmate-email-otp-key"


def _tables(db):
    db.execute("""CREATE TABLE IF NOT EXISTS karirmate_email_challenges (
        account_id INTEGER PRIMARY KEY,
        target_email TEXT NOT NULL,
        code_digest TEXT NOT NULL,
        expires_at INTEGER NOT NULL,
        requested_at INTEGER NOT NULL,
        attempts INTEGER NOT NULL DEFAULT 0
    )""")
    db.execute("""CREATE TABLE IF NOT EXISTS karirmate_email_send_log (
        account_id INTEGER NOT NULL,
        sent_at INTEGER NOT NULL
    )""")
    db.execute("""CREATE INDEX IF NOT EXISTS idx_karirmate_email_send_log
        ON karirmate_email_send_log(account_id, sent_at)""")


def _authenticated(db, authorization, now):
    if not isinstance(authorization, str) or not authorization.startswith("Bearer "):
        return None
    token = authorization[7:]
    if not token or len(token) > 1024 or any(c.isspace() for c in token):
        return None
    digest = hashlib.sha256(token.encode()).hexdigest()
    return db.execute("""
        SELECT a.id, a.email, a.email_verified
        FROM android_sessions s
        JOIN android_accounts a ON a.id = s.account_id
        WHERE s.token_hash=? AND s.expires_at > ?
    """, (digest, now)).fetchone()


def _smtp_settings():
    if not SMTP_CONFIG.is_file():
        return None
    values = {}
    for line in SMTP_CONFIG.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        values[key.strip()] = value.strip()
    needed = ('SMTP_HOST', 'SMTP_USER', 'SMTP_PASSWORD')
    if any(not values.get(k) for k in needed):
        return None
    port = int(values.get('SMTP_PORT', '465'))
    if port not in (465, 587):
        raise ValueError('SMTP_PORT harus 465 atau 587')
    return values, port


def _signing_key():
    try:
        fd = os.open(str(SECRET_FILE), os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    except FileExistsError:
        pass
    else:
        with os.fdopen(fd, 'w') as file:
            file.write(secrets.token_hex(32))
    raw = bytes.fromhex(SECRET_FILE.read_text().strip())
    if len(raw) != 32:
        raise ValueError('Kunci verifikasi email tidak valid')
    return raw


def _code_digest(account_id, code):
    return hmac.new(_signing_key(), f'{account_id}:{code}'.encode(), hashlib.sha256).hexdigest()


def _send_email(email, code, settings):
    # KARIRMATE_BRANDED_OTP_EMAIL_V2
    cfg, port = settings
    msg = EmailMessage()
    msg['Subject'] = 'Kode verifikasi email KarirMate'
    msg['From'] = cfg.get('SMTP_FROM') or cfg['SMTP_USER']
    msg['To'] = email
    msg.set_content(
        'Kode verifikasi email KarirMate kamu: ' + code + '\n\n'
        'Kode berlaku 10 menit, hanya untuk akun KarirMate milikmu.\n'
        'Jangan bagikan kode ini kepada siapa pun. Jika tidak meminta kode, abaikan email ini.\n'
    )
    from otp_email_design import render_otp_html, attach_otp_assets
    msg.add_alternative(
        render_otp_html(code, cfg['SMTP_USER'], minutes=CODE_LIFETIME // 60),
        subtype='html'
    )
    attach_otp_assets(msg.get_payload()[-1])
    context = ssl.create_default_context()
    if port == 465:
        with smtplib.SMTP_SSL(cfg['SMTP_HOST'], port, timeout=15, context=context) as smtp:
            smtp.login(cfg['SMTP_USER'], cfg['SMTP_PASSWORD'])
            smtp.send_message(msg)
    elif port == 587:
        with smtplib.SMTP(cfg['SMTP_HOST'], port, timeout=15) as smtp:
            smtp.starttls(context=context)
            smtp.login(cfg['SMTP_USER'], cfg['SMTP_PASSWORD'])
            smtp.send_message(msg)
    else:
        raise ValueError('SMTP_PORT harus 465 atau 587')




def handle_email_verification(path, authorization, payload):
    if path not in ('/auth/email-verification/request', '/auth/email-verification/confirm'):
        return 404, {'error': 'Not found'}
    if not isinstance(payload, dict):
        return 400, {'error': 'Permintaan tidak valid'}

    now = int(time.time())
    with sqlite3.connect(DB, timeout=20) as db:
        db.execute('BEGIN IMMEDIATE')
        identity = _authenticated(db, authorization, now)
        if identity is None:
            return 401, {'error': 'Sesi tidak valid. Masuk kembali.'}
        account_id, email, verified = identity
        if verified:
            return 200, {'success': True, 'email_verified': True,
                         'message': 'Email sudah terverifikasi.'}
        if not isinstance(email, str) or '@' not in email or len(email) > 254:
            return 400, {'error': 'Alamat email akun tidak valid.'}
        email = email.strip().lower()
        _tables(db)

        if path.endswith('/request'):
            try:
                smtp_settings = _smtp_settings()
            except (OSError, ValueError):
                return 503, {'error': 'Pengiriman email belum dikonfigurasi dengan benar.'}
            if smtp_settings is None:
                return 503, {'error': 'Pengiriman email verifikasi belum dikonfigurasi di server.'}

            # Expired send logs are removed; fresh attempts remain bounded per account.
            db.execute('DELETE FROM karirmate_email_send_log WHERE sent_at<?',
                       (now - 86400,))
            attempts_today = db.execute("""
                SELECT COUNT(*) FROM karirmate_email_send_log
                WHERE account_id=? AND sent_at>=?
            """, (account_id, now - 86400)).fetchone()[0]
            if attempts_today >= MAX_SENDS_24H:
                return 429, {'error': 'Batas pengiriman kode harian tercapai. Coba lagi besok.'}
            last = db.execute("""
                SELECT MAX(sent_at) FROM karirmate_email_send_log WHERE account_id=?
            """, (account_id,)).fetchone()[0]
            if last is not None and now - last < MIN_RESEND_SECONDS:
                return 429, {'error': 'Tunggu 60 detik sebelum mengirim ulang kode.'}

            code = f'{secrets.randbelow(1_000_000):06d}'
            digest = _code_digest(account_id, code)
            db.execute("""
                INSERT INTO karirmate_email_challenges
                (account_id, target_email, code_digest, expires_at, requested_at, attempts)
                VALUES (?, ?, ?, ?, ?, 0)
                ON CONFLICT(account_id) DO UPDATE SET
                  target_email=excluded.target_email,
                  code_digest=excluded.code_digest,
                  expires_at=excluded.expires_at,
                  requested_at=excluded.requested_at,
                  attempts=0
            """, (account_id, email, digest, now + CODE_LIFETIME, now))
            db.execute('INSERT INTO karirmate_email_send_log(account_id,sent_at) VALUES (?,?)',
                       (account_id, now))
            # Commit reservation before network I/O; request is rate-limited even if SMTP fails.
            db.commit()
            try:
                _send_email(email, code, smtp_settings)
            except Exception:
                with sqlite3.connect(DB, timeout=20) as reset_db:
                    reset_db.execute("""DELETE FROM karirmate_email_challenges
                        WHERE account_id=? AND code_digest=?""", (account_id, digest))
                return 503, {'error': 'Email gagal dikirim. Periksa konfigurasi SMTP server.'}
            return 200, {'success': True,
                         'message': 'Kode enam digit dikirim ke email akun.',
                         'expires_in_seconds': CODE_LIFETIME}

        code = payload.get('code')
        if not isinstance(code, str) or not re.fullmatch(r'\d{6}', code):
            return 400, {'error': 'Masukkan kode enam digit.'}
        row = db.execute("""
            SELECT target_email, code_digest, expires_at, attempts
            FROM karirmate_email_challenges WHERE account_id=?
        """, (account_id,)).fetchone()
        if row is None:
            return 400, {'error': 'Minta kode verifikasi terlebih dahulu.'}
        target_email, expected_digest, expires_at, attempts = row
        if now > expires_at or attempts >= MAX_ATTEMPTS or target_email != email:
            db.execute('DELETE FROM karirmate_email_challenges WHERE account_id=?', (account_id,))
            return 400, {'error': 'Kode kedaluwarsa atau sudah tidak berlaku. Minta kode baru.'}
        if not hmac.compare_digest(_code_digest(account_id, code), expected_digest):
            db.execute("""UPDATE karirmate_email_challenges SET attempts=attempts+1
                WHERE account_id=?""", (account_id,))
            return 400, {'error': 'Kode salah. Periksa kembali email.'}
        db.execute('UPDATE android_accounts SET email_verified=1 WHERE id=?', (account_id,))
        db.execute('DELETE FROM karirmate_email_challenges WHERE account_id=?', (account_id,))
        return 200, {'success': True, 'email_verified': True,
                     'message': 'Email berhasil diverifikasi.'}
