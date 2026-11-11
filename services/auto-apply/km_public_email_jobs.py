import argparse
import json
import sqlite3
from datetime import datetime, timezone
from runtime_paths import DB

JOBS = [
    {
        "job_title": "Admin Logistik",
        "company": "Solusi Klik Group",
        "location": "Karawaci, Tangerang",
        "email": "recruitment@solusiklik.id",
        "job_url": "https://infolokerbanten.com/lowongan-kerja-admin-logistik-solusi-klik-group-karawaci-tangerang-2026/"
    },
    {
        "job_title": "Warehouse Coordinator Staff",
        "company": "TAILG Indonesia",
        "location": "Balaraja, Tangerang",
        "email": "taffarelalghazali@tailg.com.cn",
        "job_url": "https://id.linkedin.com/in/taffarel-al-ghazali-34b300248"
    }
]

def main(account_id):
    uid = -account_id

    with sqlite3.connect(DB, timeout=30) as db:
        db.row_factory = sqlite3.Row

        profile = db.execute("""
            SELECT desired_positions, preferred_locations
            FROM android_profiles
            WHERE account_id=?
        """, (account_id,)).fetchone()

        if not profile:
            print("SKIP: Profil tidak ditemukan")
            return

        positions = [
            str(x).casefold()
            for x in json.loads(profile["desired_positions"] or "[]")
        ]
        locations = [
            str(x).casefold()
            for x in json.loads(profile["preferred_locations"] or "[]")
        ]

        columns = {
            row["name"]
            for row in db.execute(
                "PRAGMA table_info(phase57_queue)"
            )
        }

        added = upgraded = 0

        for job in JOBS:
            title = job["job_title"].casefold()
            place = job["location"].casefold()
            email = job["email"].casefold()

            if not any(p and p in title for p in positions):
                continue
            if not any(loc and loc in place for loc in locations):
                continue

            old = db.execute("""
                SELECT id, kind, status
                FROM phase57_queue
                WHERE user_id=? AND lower(email)=?
                LIMIT 1
            """, (uid, email)).fetchone()

            fields = {
                **job,
                "user_id": uid,
                "kind": "vacancy",
                "status": "ready",
                "contact_source": "public:job-posting",
                "created_at": datetime.now(
                    timezone.utc
                ).isoformat()
            }

            if old:
                if (
                    old["kind"] == "spontaneous"
                    and old["status"] == "ready"
                ):
                    updates = {
                        k: v for k, v in fields.items()
                        if k in columns and k not in (
                            "user_id", "email", "status",
                            "created_at"
                        )
                    }
                    sql = ", ".join(
                        f"{key}=?" for key in updates
                    )
                    db.execute(
                        f"UPDATE phase57_queue SET {sql} "
                        "WHERE id=?",
                        (*updates.values(), old["id"])
                    )
                    upgraded += 1
                continue

            data = {
                k: v for k, v in fields.items()
                if k in columns
            }
            names = ", ".join(data)
            marks = ", ".join("?" for _ in data)

            try:
                db.execute(
                    f"INSERT INTO phase57_queue ({names}) "
                    f"VALUES ({marks})",
                    tuple(data.values())
                )
                added += 1
            except sqlite3.IntegrityError as exc:
                print("Gagal antre:", email, str(exc))

        db.commit()

        print("=== SUMBER EMAIL LOWONGAN ===")
        print("Akun:", account_id)
        print("Lowongan baru:", added)
        print("Kontak diperbarui:", upgraded)
        print("Email dikirim sekarang: 0")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--account-id", type=int, required=True
    )
    args = parser.parse_args()
    if args.account_id <= 0:
        parser.error("Account ID tidak valid")
    main(args.account_id)
