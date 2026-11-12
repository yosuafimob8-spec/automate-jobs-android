#!/data/data/com.termux/files/usr/bin/bash
# KarirMate V66: refresh lowongan sekali jalan, tidak mengirim email,
# tidak menjadwalkan cron, tidak memakai ulang JSON hasil crawl lama.
set -uo pipefail
umask 077
cd "$HOME/automate-ai" || exit 1
LOG="$PWD/refresh.log"
RUN_ID="$(date +%Y%m%d_%H%M%S)_$$"
STAGE="$(mktemp -d "$PWD/.refresh-v66.XXXXXX")" || exit 1
trap 'rm -rf "$STAGE"' EXIT
printf '\n===== V66 REFRESH %s =====\n' "$(date '+%F %T')" | tee -a "$LOG"

accepted=()
for engine in dealls kalibrr; do
    remote="/tmp/automate_${engine}_v66_${RUN_ID}.json"
    staged="$STAGE/${engine}.json"
    echo "Scraper: $engine" | tee -a "$LOG"

    # Hapus output lama untuk run yang sama; jangan sembunyikan kegagalan Scrapy.
    if proot-distro login ubuntu -- bash -lc \
        "cd /root/github-engines/id-jobs && . .venv/bin/activate && rm -f '$remote' && timeout 170s scrapy crawl '$engine' -O '$remote' -s ROBOTSTXT_OBEY=True -s CLOSESPIDER_ITEMCOUNT=300 -s CLOSESPIDER_TIMEOUT=140 -s CONCURRENT_REQUESTS=1 -s DOWNLOAD_DELAY=2 -s LOG_LEVEL=ERROR" \
        >> "$LOG" 2>&1; then
        if proot-distro login ubuntu -- cat "$remote" > "$staged" 2>> "$LOG"; then
            if python3 - "$engine" "$staged" <<'PY' >> "$LOG" 2>&1
import json, sys
from pathlib import Path
from idjobs_feed import canonical_url, tidy
engine, path = sys.argv[1:]
rows = json.loads(Path(path).read_text(encoding='utf-8'))
if not isinstance(rows, list):
    raise SystemExit('REJECT: bukan JSON array')
valid = 0
for item in rows:
    if not isinstance(item, dict):
        continue
    _, source = canonical_url(item.get('job_url') or item.get('url'))
    title = tidy(item.get('job_title') or item.get('title'))
    location = tidy(item.get('job_location') or item.get('location'))
    if source.casefold() == engine and title and location:
        valid += 1
print(f'{engine}: total={len(rows)}, valid={valid}')
if valid == 0:
    raise SystemExit('REJECT: tidak ada record yang valid dari sumber ini')
PY
            then
                accepted+=("$staged")
                echo "PASS: $engine -> file baru lolos validasi" | tee -a "$LOG"
            else
                echo "SKIP: $engine -> data baru tidak valid" | tee -a "$LOG"
            fi
        else
            echo "SKIP: $engine -> output scraper tidak dapat dibaca" | tee -a "$LOG"
        fi
    else
        echo "SKIP: $engine -> scraper gagal; file lama tidak digunakan" | tee -a "$LOG"
    fi
    proot-distro login ubuntu -- rm -f "$remote" >> "$LOG" 2>&1 || true
done

if [ "${#accepted[@]}" -eq 0 ]; then
    echo "STOP: Tidak ada file crawl baru yang valid. Database tidak diubah." | tee -a "$LOG"
    exit 2
fi

if python3 - "$RUN_ID" "${accepted[@]}" <<'PY' >> "$LOG" 2>&1
import os, sqlite3, sys
from pathlib import Path
from idjobs_feed import DB, import_files, fetch_feed
runid, *paths = sys.argv[1:]
db_path = Path(DB)
if not db_path.is_file():
    raise SystemExit(f'STOP: DB belum ada di {db_path}')
# Backup konsisten, tanpa mematikan backend 8787.
backup = db_path.with_name(db_path.name + f'.backup-v66-{runid}')
with sqlite3.connect(db_path) as src, sqlite3.connect(backup) as dst:
    src.backup(dst)
with sqlite3.connect(db_path) as db:
    before = db.execute('SELECT COUNT(*) FROM idjobs_feed').fetchone()[0]
result = import_files(paths)
with sqlite3.connect(db_path) as db:
    after = db.execute('SELECT COUNT(*) FROM idjobs_feed').fetchone()[0]
for path in paths:
    engine = Path(path).stem
    os.replace(path, f'refresh_{engine}.json')
print('Database:', before, '->', after)
print('Hasil impor:', result)
print('Backup:', backup)
print('Admin Warehouse Tangerang:', len(fetch_feed('Admin Warehouse', 'Tangerang', limit=20)))
print('Email dikirim: 0')
PY
then
    echo "SUKSES: refresh V66 selesai. Lihat $LOG" | tee -a "$LOG"
else
    echo "GAGAL: impor/backup bermasalah. Periksa $LOG. File refresh lama tidak ditimpa." | tee -a "$LOG"
    exit 1
fi
