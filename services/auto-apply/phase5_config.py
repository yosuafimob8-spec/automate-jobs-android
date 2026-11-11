import sqlite3
from pathlib import Path

BASE = Path(__file__).resolve().parent
from runtime_paths import DB, CV_DIR



def connect():
    db = sqlite3.connect(DB, timeout=30)
    db.execute("""
        CREATE TABLE IF NOT EXISTS phase5_config (
            user_id INTEGER PRIMARY KEY,
            daily_limit INTEGER NOT NULL DEFAULT 5,
            weekdays_only INTEGER NOT NULL DEFAULT 1,
            auto_consent INTEGER NOT NULL DEFAULT 0,
            cv_path TEXT,
            gmail_connected INTEGER NOT NULL DEFAULT 0
        )
    """)
    db.commit()
    return db


def get(uid):
    with connect() as db:
        db.execute(
            "INSERT OR IGNORE INTO phase5_config(user_id) VALUES (?)",
            (uid,)
        )
        db.commit()
        db.row_factory = sqlite3.Row
        return dict(db.execute(
            "SELECT * FROM phase5_config WHERE user_id=?",
            (uid,)
        ).fetchone())


def set_consent(uid, value):
    get(uid)
    with connect() as db:
        db.execute(
            "UPDATE phase5_config SET auto_consent=? WHERE user_id=?",
            (int(bool(value)), uid)
        )


def set_cv(uid, path):
    get(uid)
    with connect() as db:
        db.execute(
            "UPDATE phase5_config SET cv_path=? WHERE user_id=?",
            (str(path), uid)
        )


def summary(uid):
    p = get(uid)
    cv = (
        "✅ Tersimpan"
        if p["cv_path"] and Path(p["cv_path"]).is_file()
        else "❌ Belum diunggah"
    )
    consent = "✅ Diizinkan" if p["auto_consent"] else "❌ Belum diizinkan"
    return (
        "✉️ KIRIM LAMARAN EMAIL — PRO\n\n"
        f"📨 Target: {p['daily_limit']} email/hari\n"
        "📅 Jadwal: Senin–Jumat\n"
        "🎯 Penerima: HRD terverifikasi\n"
        f"📄 CV: {cv}\n"
        f"🤖 Izin kirim otomatis: {consent}\n"
        "🔗 Gmail: Belum terhubung\n\n"
        "🔒 Pengiriman belum tersedia sampai Gmail OAuth "
        "dan verifikasi HRD diaktifkan."
    )
