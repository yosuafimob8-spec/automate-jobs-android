import argparse
import re
import sqlite3
import subprocess
import sys
from pathlib import Path
from runtime_paths import DB

BASE = Path(__file__).resolve().parent

def applicant_name(db, account_id):
    for table, id_column in (
        ("android_profiles", "account_id"),
        ("android_accounts", "id")
    ):
        columns = {
            row[1]
            for row in db.execute(f"PRAGMA table_info({table})")
        }
        if id_column not in columns:
            continue

        for field in (
            "full_name", "nama_lengkap",
            "display_name", "name", "nama"
        ):
            if field not in columns:
                continue

            row = db.execute(
                f"SELECT {field} FROM {table} WHERE {id_column}=?",
                (account_id,)
            ).fetchone()

            if row and isinstance(row[0], str) and row[0].strip():
                return row[0].strip()

    # Pertahankan nama scheduler lama khusus akun 2.
    if account_id == 2:
        scheduler = BASE / "km_autoapply_daily.sh.bak.v47"
        if scheduler.exists():
            match = re.search(
                r'--name\s+"([^"]+)"',
                scheduler.read_text()
            )
            if match:
                return match.group(1)

    return None

def run(command, timeout):
    try:
        result = subprocess.run(
            command, cwd=BASE, timeout=timeout,
            check=False
        )
        return result.returncode == 0
    except subprocess.TimeoutExpired:
        print("TIMEOUT:", command[1], flush=True)
        return False

def main(live):
    with sqlite3.connect(DB, timeout=30) as db:
        accounts = db.execute("""
            SELECT -s.user_id
            FROM phase57_settings s
            JOIN phase5_config c
              ON c.user_id=s.user_id
            JOIN android_accounts a
              ON a.id=-s.user_id
            WHERE s.user_id<0
              AND s.enabled=1
              AND s.consent_at IS NOT NULL
              AND c.auto_consent=1
              AND c.cv_path IS NOT NULL
              AND c.cv_path!=''
            ORDER BY a.id
        """).fetchall()

        targets = [
            (int(row[0]), applicant_name(db, int(row[0])))
            for row in accounts
        ]

    print("=== AUTO APPLY MULTI-USER ===", flush=True)
    print("Akun dengan persetujuan aktif:", len(targets), flush=True)

    for account_id, name in targets:
        print(f"\n=== AKUN {account_id} ===", flush=True)

        if not name:
            print("SKIP: Nama pelamar belum tersedia", flush=True)
            continue

        uid = -account_id

        if not live:
            run([
                sys.executable, "phase512_sender.py",
                "--uid", str(uid)
            ], 15)
            continue

        sources = [
            ("km_disnaker_v39.py", 32),
            ("km_public_discovery.py", 65),
            ("km_jobs_v2_bridge.py", 50),
            ("km_auto_refill.py", 30)
        ]

        for script, limit in sources:
            file = BASE / script
            if not file.is_file():
                print("SKIP: Sumber tidak tersedia:", script, flush=True)
                continue

            args = [
                sys.executable, script,
                "--account-id", str(account_id)
            ]

            if script == "km_public_discovery.py":
                args += ["--max-queries", "3"]

            run(args, limit)

        # Kuota, PRO, Gmail, CV, consent dan anti-duplikat
        # tetap diperiksa oleh phase512_sender.
        run([
            sys.executable, "phase512_sender.py",
            "--uid", str(uid),
            "--name", name,
            "--send"
        ], 110)

    print("\nSelesai. Mode:", "LIVE" if live else "DRY-RUN")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--send", action="store_true")
    args = parser.parse_args()
    main(args.send)
