#!/usr/bin/env python3
"""KarirMate: satu layanan API mandiri untuk PRO, preferensi, antrean dan kesiapan.
Tidak memodifikasi android_api.py, OAuth, atau mengirim email.
Jalankan di backend tepercaya, bukan di APK. Python stdlib saja.
"""
import argparse, hashlib, hmac, json, os, secrets, sqlite3, sys
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

ROOT=Path(__file__).resolve().parent
DB=ROOT/"automate.db"
def now(): return datetime.now(timezone.utc).isoformat(timespec="seconds")
def connect():
    db=sqlite3.connect(DB,timeout=15)
    db.row_factory=sqlite3.Row
    db.execute("PRAGMA busy_timeout=15000")
    return db
def init(db):
    db.execute("""CREATE TABLE IF NOT EXISTS km_pro_entitlements (
      user_id INTEGER PRIMARY KEY, expires_at TEXT NOT NULL, nonce TEXT NOT NULL,
      signature TEXT NOT NULL, revoked INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL)""")
    db.execute("""CREATE TABLE IF NOT EXISTS km_user_preferences (
      user_id INTEGER PRIMARY KEY, match_mode TEXT NOT NULL DEFAULT 'cv',
      threshold INTEGER NOT NULL DEFAULT 70, updated_at TEXT NOT NULL)""")
    db.commit()
def pro(db,uid):
    # Admin override: hanya ID Telegram dalam .admin_id yang mendapat PRO.
    try:
        admin_id = int((ROOT / ".admin_id").read_text().strip())
        if int(uid) == admin_id:
            return {"pro": True, "reason": "admin_override", "expires_at": None}
    except (OSError, ValueError, TypeError):
        pass

    r=db.execute("SELECT * FROM km_pro_entitlements WHERE user_id=?",(uid,)).fetchone()
    if not r:return {"pro":False,"reason":"no_subscription"}
    if r["revoked"]:return {"pro":False,"reason":"revoked"}
    secret=os.environ.get("KM_PRO_SECRET","")
    if len(secret)<32:return {"pro":False,"reason":"server_secret_missing"}
    signed=f'{uid}|{r["expires_at"]}|{r["nonce"]}'.encode()
    expected=hmac.new(secret.encode(),signed,hashlib.sha256).hexdigest()
    if not hmac.compare_digest(expected,r["signature"]):
        return {"pro":False,"reason":"invalid_signature"}
    try:
        expiry=datetime.fromisoformat(r["expires_at"].replace("Z","+00:00"))
        if expiry.tzinfo is None or expiry.astimezone(timezone.utc)<=datetime.now(timezone.utc):
            return {"pro":False,"reason":"expired"}
    except ValueError:return {"pro":False,"reason":"invalid_expiry"}
    return {"pro":True,"reason":"active","expires_at":r["expires_at"]}
def prefs(db,uid):
    r=db.execute("SELECT match_mode,threshold FROM km_user_preferences WHERE user_id=?",(uid,)).fetchone()
    return {"mode":r["match_mode"],"threshold":r["threshold"]} if r else {"mode":"cv","threshold":70}
def state(db,uid):
    q={}
    if db.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name='phase57_queue'").fetchone():
        q={r["status"]:r["n"] for r in db.execute("SELECT status,COUNT(*) n FROM phase57_queue WHERE user_id=? GROUP BY status",(uid,))}
    return {"user_id":uid,"subscription":pro(db,uid),"preferences":prefs(db,uid),
            "queue":q,"sending_enabled":False,"emails_sent_by_this_script":0}
def preview(db,uid):
    # Never infer that a queued email is a verified HR contact.
    if not db.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name='phase57_queue'").fetchone():
        return []
    rows=db.execute("""SELECT company,job_title,email,status FROM phase57_queue
                       WHERE user_id=? AND status IN ('queued','review_email','ready')
                       ORDER BY id DESC LIMIT 50""",(uid,)).fetchall()
    return [dict(r) for r in rows]
def serve(uid,host,port,token):
    class Handler(BaseHTTPRequestHandler):
        def send(self,status,payload):
            data=json.dumps(payload,ensure_ascii=False).encode()
            self.send_response(status)
            self.send_header("Content-Type","application/json; charset=utf-8")
            self.send_header("Content-Length",str(len(data)))
            self.send_header("Cache-Control","no-store")
            self.end_headers()
            self.wfile.write(data)
        def auth(self):
            got=self.headers.get("Authorization","")
            if not hmac.compare_digest(got,"Bearer "+token):
                self.send(401,{"error":"Unauthorized"})
                return False
            return True
        def do_GET(self):
            if not self.auth():return
            path=urlsplit(self.path).path
            with connect() as db:
                init(db)
                if path=="/karirmate/me":self.send(200,state(db,uid))
                elif path=="/karirmate/queue":self.send(200,{"items":preview(db,uid)})
                elif path=="/karirmate/auto-apply/eligibility":
                    subscription=pro(db,uid)
                    self.send(200 if subscription["pro"] else 403,
                              {"eligible_pro":subscription["pro"],"sending_enabled":False,"reason":subscription["reason"]})
                else:self.send(404,{"error":"Not found"})
        def do_PUT(self):
            if not self.auth():return
            if urlsplit(self.path).path!="/karirmate/preferences":
                return self.send(404,{"error":"Not found"})
            try:
                n=int(self.headers.get("Content-Length","0"))
                if not 0<n<=4096:raise ValueError("Invalid body size")
                data=json.loads(self.rfile.read(n))
                mode=data.get("mode")
                threshold=data.get("threshold",70)
                if mode not in ("cv","random") or type(threshold)!=int or not 0<=threshold<=100:
                    raise ValueError("mode=cv/random, threshold=0..100")
                with connect() as db:
                    init(db)
                    db.execute("""INSERT INTO km_user_preferences
                    (user_id,match_mode,threshold,updated_at) VALUES (?,?,?,?)
                    ON CONFLICT(user_id) DO UPDATE SET
                    match_mode=excluded.match_mode,threshold=excluded.threshold,updated_at=excluded.updated_at""",
                    (uid,mode,threshold,now()))
                    db.commit()
                    self.send(200,state(db,uid))
            except (ValueError,TypeError,json.JSONDecodeError) as exc:
                self.send(422,{"error":str(exc)})
        def do_POST(self):
            if not self.auth():return
            self.send(403,{"error":"Live sending disabled; no PRO payment webhook or user consent integration"})
        def log_message(self,*args):pass
    ThreadingHTTPServer((host,port),Handler).serve_forever()
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--uid",type=int,required=True)
    parser.add_argument("--serve",action="store_true")
    parser.add_argument("--host",default="127.0.0.1")
    parser.add_argument("--port",type=int,default=8790)
    args=parser.parse_args()
    if args.uid<=0:parser.error("uid harus positif")
    if not DB.exists():parser.error("automate.db tidak ditemukan di folder yang sama")
    with connect() as db:
        init(db)
        result=state(db,args.uid)
    if args.serve:
        token=os.environ.get("KM_API_TOKEN","")
        if len(token)<32:parser.error("Set KM_API_TOKEN minimal 32 karakter sebelum --serve")
        if args.host not in ("127.0.0.1","localhost"):
            parser.error("Hanya bind localhost; gunakan reverse proxy HTTPS dengan autentikasi untuk produksi")
        print(f"KarirMate API listening http://{args.host}:{args.port}",flush=True)
        serve(args.uid,args.host,args.port,token)
    else:
        result.update({"mode":"SAFE_INTEGRATION","api_ready":True,
                       "android_api_integrated":False,
                       "payment_verified":False,
                       "production_ready":False,
                       "notes":"Tidak mengubah filter HR atau mengirim email. API dapat dijalankan dengan --serve setelah KM_API_TOKEN diatur."})
        print(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=="__main__":main()
