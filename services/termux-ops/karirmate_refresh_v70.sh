#!/data/data/com.termux/files/usr/bin/bash
# KarirMate V70: conservative keyword refresh for Karir.com, plus optional V66.
# Default: PREVIEW (scrape and validate only, no DB writes).
# With --apply: run proven V66 for Dealls/Kalibrr, then import validated Karir.com.
# Does not touch cron, Gmail, GitHub or APK.
set -uo pipefail
umask 077

MODE="${1:---preview}"
if [[ "$MODE" != "--preview" && "$MODE" != "--apply" || $# -gt 1 ]]; then
  echo 'Usage: bash karirmate_refresh_v70.sh [--preview|--apply]' >&2
  exit 2
fi
APP="$HOME/automate-ai"
cd "$APP" || exit 2
IMPORTER="$APP/karirmate_v69_import_karir.py"
V66="$APP/karirmate_refresh_jobs_v66_safe.sh"
[ -f "$IMPORTER" ] || { echo 'STOP: Importir V69 tidak ditemukan' >&2; exit 2; }
[ -f "$APP/idjobs_feed.py" ] || { echo 'STOP: Mesin filter tidak ditemukan' >&2; exit 2; }
if [[ "$MODE" == '--apply' && ! -f "$V66" ]]; then
  echo 'STOP: Skrip refresh V66 tidak ditemukan; integrasi empat sumber tidak lengkap' >&2
  exit 2
fi
command -v flock >/dev/null || { echo 'STOP: flock belum tersedia' >&2; exit 2; }
command -v proot-distro >/dev/null || { echo 'STOP: proot-distro belum tersedia' >&2; exit 2; }
exec 9> "$APP/.karirmate-refresh-v70.lock"
flock -n 9 || { echo 'STOP: Refresh lain masih berjalan'; exit 3; }
STAGE=$(mktemp -d "$APP/.karirmate-v70.XXXXXXXX") || exit 2
trap 'rm -rf "$STAGE"' EXIT
RUN_ID="$(date '+%Y%m%d_%H%M%S')_$$"
LOG="$APP/refresh_v70.log"
printf '\n===== V70 %s MODE=%s =====\n' "$(date '+%F %T')" "$MODE" | tee -a "$LOG"

# Use only the verified, locally patched Karir.com spider.
# Timeout and robots.txt rules are not disabled.
accepted=()
for keyword in gudang inventory logistik operasional; do
  remote="/tmp/km_v70_${keyword}_${RUN_ID}.json"
  local_file="$STAGE/${keyword}.json"
  echo "SCRAPE: ${keyword}" | tee -a "$LOG"
  if proot-distro login ubuntu -- bash -lc \
    "cd /root/github-engines/id-jobs && . .venv/bin/activate && rm -f '$remote' && timeout 100s scrapy crawl karir -a keyword='$keyword' -O '$remote' -s ROBOTSTXT_OBEY=True -s TELNETCONSOLE_ENABLED=False -s CLOSESPIDER_ITEMCOUNT=40 -s CLOSESPIDER_TIMEOUT=80 -s CONCURRENT_REQUESTS=1 -s LOG_LEVEL=ERROR" \
    >> "$LOG" 2>&1 \
    && proot-distro login ubuntu -- cat "$remote" > "$local_file" 2>> "$LOG" \
    && python3 - "$local_file" "$keyword" <<'PY' >> "$LOG" 2>&1
import json,sys
from pathlib import Path
p=Path(sys.argv[1]); keyword=sys.argv[2]
if p.stat().st_size > 15_000_000:
    raise SystemExit('REJECT: JSON terlalu besar')
data=json.loads(p.read_text(encoding='utf-8'))
if not isinstance(data,list) or not all(isinstance(x,dict) for x in data):
    raise SystemExit('REJECT: Bukan daftar lowongan')
print(f'{keyword}: {len(data)} record baru dari spider')
PY
  then
    accepted+=("$local_file")
    echo "PASS: ${keyword} diunduh dan divalidasi" | tee -a "$LOG"
  else
    echo "SKIP: ${keyword} gagal atau invalid; jangan gunakan file lama" | tee -a "$LOG"
  fi
  proot-distro login ubuntu -- rm -f "$remote" >> "$LOG" 2>&1 || true
done

if [ "${#accepted[@]}" -eq 0 ]; then
  echo 'STOP: Tidak ada hasil baru yang valid dari Karir.com. Database tidak diubah.' | tee -a "$LOG"
  exit 2
fi

# Join fresh files. Exclude explicit cross-city contradictions in the title,
# e.g. PSIM Jogja with a Jakarta location; not a replacement for human verification.
COMBINED="$STAGE/karir_combined.json"
python3 - "$COMBINED" "${accepted[@]}" <<'PY' || exit 2
import json,sys,re
from pathlib import Path
out=Path(sys.argv[1]); selected={}; excluded=[]
for src in sys.argv[2:]:
    for job in json.loads(Path(src).read_text(encoding='utf-8')):
        title=str(job.get('job_title') or job.get('title') or '').lower()
        loc=str(job.get('job_location') or job.get('location') or '').lower()
        if re.search(r'\b(jogja|yogyakarta)\b',title) and not re.search(r'jogja|yogyakarta|sleman|bantul',loc):
            excluded.append((title,loc))
            continue
        url=job.get('job_url') or job.get('url')
        if isinstance(url,str): selected[url]=job
out.write_text(json.dumps(list(selected.values()),ensure_ascii=False),encoding='utf-8')
print(f'V70 gabungan unik: {len(selected)}; lokasi ambigu ditahan: {len(excluded)}')
PY

echo '=== HASIL VALIDASI (TANPA IMPOR) ===' | tee -a "$LOG"
python3 "$IMPORTER" "$COMBINED" | tee -a "$LOG" || exit 2
if [[ "$MODE" == '--preview' ]]; then
  echo 'PREVIEW selesai. Database belum berubah. Tidak ada cron yang diaktifkan.' | tee -a "$LOG"
  exit 0
fi

# Execute already validated V66 for Dealls and Kalibrr.
echo '=== REFRESH V66 DEALLS + KALIBRR ===' | tee -a "$LOG"
if bash "$V66" >> "$LOG" 2>&1; then
  echo 'PASS: V66 selesai' | tee -a "$LOG"
else
  echo 'WARN: V66 gagal; hasil Karir.com tetap diproses mandiri' | tee -a "$LOG"
fi

echo '=== IMPOR KARIR.COM DENGAN BACKUP ===' | tee -a "$LOG"
python3 "$IMPORTER" "$COMBINED" --apply | tee -a "$LOG" || exit 1
python3 "$APP/idjobs_feed.py" stats | tee -a "$LOG"
echo 'V70 selesai. Email terkirim: 0. Cron/GitHub/APK tidak diubah.' | tee -a "$LOG"
