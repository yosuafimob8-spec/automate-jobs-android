#!/usr/bin/env bash
set -euo pipefail
cd "${KARIRMATE_REPO_DIR:-$(dirname "$0")/..}"
REPO="$PWD"
proot-distro login ubuntu --bind "$REPO:/opt/karirmate" -- bash -lc '
set -e
PY=/root/karirmate-voice-venv/bin/python
[ -x "$PY" ] || { echo "ERROR: VoxCPM2 venv belum tersedia"; exit 1; }
cd /opt/karirmate/backend-contract
if ! curl -fsS --max-time 2 http://127.0.0.1:8791/openapi.json | grep -q "KarirMate VoxCPM2 local TTS"; then
  nohup "$PY" -m uvicorn voxcpm2_tts_api:app --host 127.0.0.1 --port 8791 >/root/karirmate-voxcpm.log 2>&1 </dev/null &
fi
READY=0
for i in $(seq 1 30); do
  if curl -fsS --max-time 2 http://127.0.0.1:8791/openapi.json | grep -q "KarirMate VoxCPM2 local TTS"; then READY=1; break; fi
  sleep 1
done
if [ "$READY" != 1 ]; then echo "SERVER GAGAL: port 8791 dipakai layanan lain atau VoxCPM2 gagal start"; [ ! -f /root/karirmate-voxcpm.log ] || tail -25 /root/karirmate-voxcpm.log; exit 1; fi
echo "SERVER OK: port 8791"
CODE=$(curl -sS --max-time 240 -o /root/karirmate-voice-test.wav -w "%{http_code}" -H "Content-Type: application/json" -d '"'"'{"text":"Selamat datang di wawancara kerja KarirMate.","language":"id"}'"'"' http://127.0.0.1:8791/tts) || { echo "TES AUDIO GAGAL (timeout atau koneksi)"; tail -25 /root/karirmate-voxcpm.log; exit 1; }
if [ "$CODE" != 200 ]; then echo "TES AUDIO GAGAL HTTP $CODE"; head -c 600 /root/karirmate-voice-test.wav; echo; tail -15 /root/karirmate-voxcpm.log; exit 1; fi
"$PY" -c '"'"'import soundfile as sf; f="/root/karirmate-voice-test.wav"; x,rate=sf.read(f); assert len(x)>rate//4, "Audio terlalu pendek"; print(f"TES AUDIO BERHASIL: {len(x)/rate:.1f} detik, {rate} Hz; {f}")'"'"'
'