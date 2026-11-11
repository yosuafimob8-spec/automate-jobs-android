import argparse
import json
import re
import sqlite3
from datetime import datetime, timezone

import requests
from runtime_paths import DB

API = "http://127.0.0.1:8788/jobs"
EMAIL_RE = re.compile(
    r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"
)

def norm(value):
    return str(value or "").strip().casefold()

def matches(value, preferences):
    value = norm(value)
    return bool(value) and any(
        norm(p) in value or value in norm(p)
        for p in preferences if norm(p)
    )

def extract_emails(job):
    found = []
    def scan(value):
        if isinstance(value, str):
            found.extend(EMAIL_RE.findall(value))
        elif isinstance(value, (list, tuple)):
            for item in value:
                scan(item)
        elif isinstance(value, dict):
            for item in value.values():
                scan(item)

    for key, value in job.items():
        key = key.lower()
        if any(word in key for word in (
            "email", "contact", "recruiter",
            "hr_", "apply", "description"
        )):
            scan(value)

    return list(dict.fromkeys(e.lower() for e in found))

def main(account_id):
    uid = -account_id
    db = sqlite3.connect(DB, timeout=30)
    db.row_factory = sqlite3.Row

    try:
        profile = db.execute("""
            SELECT desired_positions, preferred_locations
            FROM android_profiles
            WHERE account_id=?
        """, (account_id,)).fetchone()

        if not profile:
            print("SKIP: Profil pengguna tidak ditemukan")
            return

        positions = json.loads(profile["desired_positions"] or "[]")
        locations = json.loads(profile["preferred_locations"] or "[]")

        if not positions or not locations:
            print("SKIP: Preferensi posisi/lokasi kosong")
            return

        columns = {
            row["name"] for row in
            db.execute("PRAGMA table_info(phase57_queue)")
        }

        existing = {
            norm(row["email"]): dict(row)
            for row in db.execute("""
                SELECT id, email, kind, status
                FROM phase57_queue
                WHERE user_id=?
            """, (uid,))
        }

        added = upgraded = 0
        seen = set()

        for preferred_location in locations:
            if added + upgraded >= 5:
                break

            try:
                response = requests.get(
                    API,
                    params={
                        "locations": preferred_location,
                        "page_size": 100
                    },
                    timeout=(5, 30)
                )
                response.raise_for_status()
                jobs = response.json().get("jobs", [])
            except Exception as exc:
                print(
                    "Sumber dilewati:",
                    preferred_location,
                    type(exc).__name__
                )
                continue

            for job in jobs:
                if not isinstance(job, dict):
                    continue

                if job.get("result_type") == "spontaneous":
                    continue

                title = str(
                    job.get("title")
                    or job.get("job_title")
                    or job.get("position")
                    or ""
                ).strip()

                location = str(
                    job.get("location") or ""
                ).strip()

                if not matches(title, positions):
                    continue

                if not matches(location, locations):
                    continue

                emails = extract_emails(job)
                if not emails:
                    continue

                company = job.get("company_name") or job.get("company") or ""
                if isinstance(company, dict):
                    company = company.get("name", "")

                job_url = str(
                    job.get("job_url")
                    or job.get("url")
                    or job.get("apply_url")
                    or ""
                )

                for email in emails:
                    if email in seen:
                        continue
                    seen.add(email)

                    values = {
                        "user_id": uid,
                        "job_url": job_url,
                        "job_title": title,
                        "company": str(company),
                        "email": email,
                        "location": location,
                        "kind": "vacancy",
                        "status": "ready",
                        "contact_source": "jobs-v2:live",
                        "created_at": datetime.now(
                            timezone.utc
                        ).isoformat()
                    }

                    old = existing.get(email)

                    if old:
                        if (
                            old["status"] == "ready"
                            and old["kind"] == "spontaneous"
                        ):
                            updates = {
                                k: v for k, v in values.items()
                                if k in columns and k not in (
                                    "user_id", "email",
                                    "status", "created_at"
                                )
                            }
                            assignments = ", ".join(
                                f"{k}=?" for k in updates
                            )
                            db.execute(
                                f"UPDATE phase57_queue SET {assignments} "
                                "WHERE id=?",
                                (*updates.values(), old["id"])
                            )
                            upgraded += 1
                        continue

                    payload = {
                        k: v for k, v in values.items()
                        if k in columns
                    }
                    names = ", ".join(payload)
                    marks = ", ".join("?" for _ in payload)

                    try:
                        db.execute(
                            f"INSERT INTO phase57_queue ({names}) "
                            f"VALUES ({marks})",
                            tuple(payload.values())
                        )
                        existing[email] = {
                            "email": email,
                            "kind": "vacancy",
                            "status": "ready"
                        }
                        added += 1
                    except sqlite3.IntegrityError:
                        continue

                    if added + upgraded >= 5:
                        break

                if added + upgraded >= 5:
                    break

        db.commit()

        print("=== JOBS V2 TERHUBUNG ===")
        print("Akun:", account_id)
        print("Prioritas pertama:", locations[0])
        print("Lowongan baru:", added)
        print("Kontak katalog naik ke lowongan aktif:", upgraded)
        print("Fallback katalog: tetap tersedia")
        print("Email dikirim sekarang: 0")

    finally:
        db.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--account-id", type=int, required=True
    )
    args = parser.parse_args()
    if args.account_id <= 0:
        parser.error("Account ID harus positif")
    main(args.account_id)
