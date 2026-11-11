import sqlite3
import json
import re
from pathlib import Path

from runtime_paths import DB
import phase57_queue
from phase512_sender import valid_target



CATALOG = (
    Path.home()
    / "automate-jobs-v2/data/companies_v2.sqlite3"
)

def main(account_id):
    UID = -account_id
    with sqlite3.connect(DB) as db:
        settings = db.execute(
            "SELECT enabled,consent_at FROM phase57_settings WHERE user_id=?",
            (UID,)
        ).fetchone()

        cfg = db.execute(
            "SELECT auto_consent FROM phase5_config WHERE user_id=?",
            (UID,)
        ).fetchone()

        if not settings or not all(settings) or not cfg or not cfg[0]:
            print("STOP: Auto Apply belum diizinkan")
            return

        profile = db.execute(
            "SELECT desired_positions,preferred_locations "
            "FROM android_profiles WHERE account_id=?",
            (account_id,)
        ).fetchone()

        if not profile:
            print("STOP: Profil tidak ditemukan")
            return

        positions = json.loads(profile[0])
        locations = json.loads(profile[1])

    positions = [
        str(x).strip() for x in positions if str(x).strip()
    ]
    locations = [
        str(x).strip() for x in locations if str(x).strip()
    ]

    if not positions or not locations:
        print("STOP: Posisi atau lokasi kosong")
        return

    warehouse_keywords = re.compile(
        r"warehouse|gudang|logistic|logistik|"
        r"supply.chain|fulfillment|cold.storage|"
        r"distribution|(?:^|\W)dc(?:\W|$)",
        re.I
    )

    with sqlite3.connect(
        f"file:{CATALOG}?mode=ro", uri=True
    ) as db:
        rows = db.execute("""
            SELECT DISTINCT
                c.id,c.name,e.address,
                f.location,COALESCE(f.business,'')
            FROM companies c
            JOIN company_facts f ON f.company_id=c.id
            JOIN company_emails ce ON ce.company_id=c.id
            JOIN emails e ON e.id=ce.email_id
        """).fetchall()

    phase57_queue.initialize()

    with sqlite3.connect(DB) as db:
        before = db.execute(
            "SELECT COUNT(*) FROM phase57_queue "
            "WHERE user_id=? AND status='ready'",
            (UID,)
        ).fetchone()[0]

    seen = set()
    candidates = []

    for company_id, company, email, location, business in rows:
        location = str(location or "")
        email = str(email or "").strip().lower()

        if not any(
            loc.casefold() in location.casefold()
            for loc in locations
        ):
            continue

        if not valid_target(email) or email in seen:
            continue

        if not warehouse_keywords.search(
            f"{company} {business}"
        ):
            continue

        # Hanya posisi gudang/logistik yang didukung
        # oleh fallback katalog ini.
        position = next(
            (
                p for p in positions
                if warehouse_keywords.search(p)
            ),
            None
        )

        if not position:
            continue

        seen.add(email)

        source = (
            f"catalog:companies_v2/company/{company_id}"
        )

        candidates.append({
            "company": company,
            "email": email,
            "position": position,
            "location": location,
            "kind": "spontaneous",
            "source_url": source,
            "contact_source": source
        })

    for candidate in candidates:
        phase57_queue.enqueue(UID, candidate)

    with sqlite3.connect(DB) as db:
        after = db.execute(
            "SELECT COUNT(*) FROM phase57_queue "
            "WHERE user_id=? AND status='ready'",
            (UID,)
        ).fetchone()[0]

    print("=== AUTO REFILL KARIRMATE ===")
    print("Posisi:", positions)
    print("Lokasi:", locations)
    print("Kontak katalog relevan:", len(candidates))
    print("Antrean sebelumnya:", before)
    print("Antrean sekarang:", after)
    print("Kandidat baru:", after - before)
    print("Email terkirim oleh proses ini: 0")

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--account-id", type=int, required=True)
    args = parser.parse_args()

    if args.account_id <= 0:
        parser.error("ID akun tidak valid")

    main(args.account_id)
