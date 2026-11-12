
import hashlib
import secrets
import sqlite3
import time
from pathlib import Path

from google.auth.transport.requests import Request
from google.oauth2 import id_token
from runtime_paths import DB


def web_client_id():
    path = Path.home() / ".karirmate-google-oauth"
    for line in path.read_text().splitlines():
        if line.startswith("GOOGLE_WEB_CLIENT_ID="):
            return line.split("=", 1)[1].strip()
    raise RuntimeError("Google Client ID tidak ditemukan")


def google_login(token):
    if not isinstance(token, str) or not 20 < len(token) <= 8192:
        return 400, {"error": "Google ID token tidak valid"}

    try:
        claims = id_token.verify_oauth2_token(
            token, Request(), web_client_id()
        )
    except Exception:
        return 401, {"error": "Verifikasi Google gagal"}

    subject = claims.get("sub")
    email = claims.get("email", "").strip().lower()

    if not subject or not email or claims.get("email_verified") is not True:
        return 403, {"error": "Identitas Google belum terverifikasi"}

    with sqlite3.connect(DB, timeout=20) as db:
        db.execute("BEGIN IMMEDIATE")

        db.execute("""
            CREATE TABLE IF NOT EXISTS android_google_identities (
                google_sub TEXT PRIMARY KEY,
                account_id INTEGER NOT NULL UNIQUE,
                created_at INTEGER NOT NULL
            )
        """)

        mapped = db.execute("""
            SELECT account_id FROM android_google_identities
            WHERE google_sub=?
        """, (subject,)).fetchone()

        if mapped:
            account_id = mapped[0]
            existing = db.execute(
                "SELECT id, email FROM android_accounts WHERE id=?",
                (account_id,)
            ).fetchone()

            if not existing:
                return 409, {"error": "Akun Google tidak ditemukan"}

            if existing[1].strip().lower() != email:
                return 409, {"error": "Email identitas Google berubah. Perlu pemeriksaan akun."}

            # Hanya dijalankan setelah ID Token Google terverifikasi.
            db.execute(
                "UPDATE android_accounts SET email_verified=1 "
                "WHERE id=? AND COALESCE(email_verified, 0)<>1",
                (account_id,)
            )
        else:
            existing = db.execute("""
                SELECT id FROM android_accounts
                WHERE lower(email)=?
            """, (email,)).fetchone()

            if existing:
                # KARIRMATE_GOOGLE_LINK_V71
                account_id = int(existing[0])
                verified = db.execute(
                    "SELECT COALESCE(email_verified, 0) FROM android_accounts WHERE id=?",
                    (account_id,)
                ).fetchone()

                if not verified or int(verified[0]) != 1:
                    return 409, {"error": "Verifikasi email akun terlebih dahulu."}

                linked = db.execute(
                    "SELECT google_sub FROM android_google_identities WHERE account_id=?",
                    (account_id,)
                ).fetchone()

                if linked:
                    return 409, {"error": "Akun sudah tertaut ke identitas Google lain."}

                db.execute(
                    "INSERT INTO android_google_identities (google_sub, account_id, created_at) VALUES (?, ?, ?)",
                    (subject, account_id, int(time.time()))
                )
            else:
                from android_auth import password_hash

                now = int(time.time())
                unusable_password = password_hash(
                    secrets.token_urlsafe(48)
                )

                cur = db.execute("""
                    INSERT INTO android_accounts
                    (email, password_hash, email_verified, created_at)
                    VALUES (?, ?, 1, ?)
                """, (email, unusable_password, now))

                account_id = cur.lastrowid

                db.execute("""
                    INSERT INTO android_google_identities
                    (google_sub, account_id, created_at)
                    VALUES (?, ?, ?)
                """, (subject, account_id, now))

        access_token = secrets.token_urlsafe(32)
        expires = int(time.time()) + 86400

        db.execute("""
            INSERT INTO android_sessions
            (token_hash, account_id, expires_at)
            VALUES (?, ?, ?)
        """, (
            hashlib.sha256(access_token.encode()).hexdigest(),
            account_id, expires
        ))

    return 200, {
        "success": True,
        "access_token": access_token,
        "token_type": "Bearer",
        "expires_at": expires,
        "email_verified": True
    }
