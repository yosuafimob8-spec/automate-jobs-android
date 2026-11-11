#!/data/data/com.termux/files/usr/bin/bash
set -e
cd /data/data/com.termux/files/home/automate-ai
export KARIRMATE_ALLOW_LIVE_SEND=1
exec python3 km_autoapply_all_users.py --send
