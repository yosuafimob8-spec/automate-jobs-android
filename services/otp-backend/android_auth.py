import hashlib
import hmac
import secrets
import sqlite3
import time
from pathlib import Path

DB = Path(__file__).resolve().parent / "automate.db"

def password_hash(password):
    salt = secrets.token_bytes(16)
    iterations = 600_000
    digest = hashlib.pbkdf2_hmac(
        "sha256", password.encode(), salt, iterations
    )
    return f"pbkdf2_sha256${iterations}${salt.hex()}${digest.hex()}"

def verify_password(password, stored):
    try:
        algorithm, iterations, salt, expected = stored.split("$")
        if algorithm != "pbkdf2_sha256":
            return False
        actual = hashlib.pbkdf2_hmac(
            "sha256",
            password.encode(),
            bytes.fromhex(salt),
            int(iterations)
        )
        return hmac.compare_digest(actual, bytes.fromhex(expected))
    except (ValueError, TypeError, OverflowError):
        return False

def register(email, password):
    email = email.strip().lower()
    if len(password) < 12 or len(email) > 254 or "@" not in email:
        return {"success": False, "error": "Email atau password tidak valid"}
    try:
        with sqlite3.connect(DB, timeout=20) as db:
            db.execute(
                """INSERT INTO android_accounts
                (email, password_hash, created_at)
                VALUES (?, ?, ?)""",
                (email, password_hash(password), int(time.time()))
            )
        return {"success": True, "message": "Akun berhasil dibuat"}
    except sqlite3.IntegrityError:
        return {"success": False, "error": "Email sudah terdaftar"}

def login(email, password):
    with sqlite3.connect(DB, timeout=20) as db:
        row = db.execute(
            "SELECT id, password_hash, email_verified FROM android_accounts WHERE email=?",
            (email.strip().lower(),)
        ).fetchone()
    if not row or not verify_password(password, row[1]):
        return {"success": False, "error": "Email atau password salah"}
    return {
        "success": True,
        "account_id": row[0],
        "email_verified": bool(row[2])
    }
