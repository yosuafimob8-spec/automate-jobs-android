"""KarirMate certificate registry prototype. Run behind HTTPS reverse proxy in production.
Registration requires REGISTRY_ADMIN_TOKEN; public verification is read-only.
"""
import hmac
import json
import os
import re
import uuid
import sqlite3
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit, parse_qs

DB = os.getenv("CERTIFICATE_DB", "certificates.sqlite3")
TOKEN = os.getenv("REGISTRY_ADMIN_TOKEN", "")
ID_RE = re.compile(r"^[0-9a-fA-F-]{36}$")
COURSE_RE = re.compile(r"^[a-z0-9_]+$")

def connect():
    conn = sqlite3.connect(DB)
    conn.execute("""CREATE TABLE IF NOT EXISTS certificates (
        id TEXT PRIMARY KEY, name TEXT NOT NULL, course TEXT NOT NULL,
        course_id TEXT NOT NULL, date TEXT NOT NULL, score INTEGER NOT NULL,
        issued_at TEXT NOT NULL, revoked INTEGER NOT NULL DEFAULT 0
    )""")
    conn.commit()
    return conn

def validate(record):
    if not isinstance(record, dict):
        raise ValueError("invalid record")
    cid = record.get("id")
    course_id = record.get("courseId")
    name = record.get("name")
    course = record.get("course")
    date = record.get("date")
    score = record.get("score")
    if not isinstance(cid, str) or not ID_RE.fullmatch(cid):
        raise ValueError("invalid id")
    try:
        uuid.UUID(cid)
    except (ValueError, AttributeError):
        raise ValueError("invalid uuid")
    if not isinstance(course_id, str) or not COURSE_RE.fullmatch(course_id):
        raise ValueError("invalid course id")
    if not isinstance(name, str) or not 1 <= len(name.strip()) <= 150:
        raise ValueError("invalid name")
    if not isinstance(course, str) or not 1 <= len(course.strip()) <= 200:
        raise ValueError("invalid course")
    if not isinstance(date, str):
        raise ValueError("invalid date")
    try:
        datetime.strptime(date, "%Y-%m-%d")
    except ValueError:
        raise ValueError("invalid date")
    if type(score) is not int or score < 80 or score > 100:
        raise ValueError("invalid passing score")
    return cid, name.strip(), course.strip(), course_id, date, score

def register(record):
    values = validate(record)
    with connect() as conn:
        conn.execute("""INSERT INTO certificates
          (id,name,course,course_id,date,score,issued_at)
          VALUES (?,?,?,?,?,?,?)""", (*values, datetime.utcnow().isoformat() + "Z"))
    return values[0]

def verify(cid):
    if not ID_RE.fullmatch(cid):
        return None
    with connect() as conn:
        row = conn.execute(
            "SELECT id,name,course,course_id,date,score,revoked FROM certificates WHERE id=?",
            (cid,)
        ).fetchone()
    if row is None:
        return None
    return dict(zip(("id","name","course","courseId","date","score","revoked"), row))

class Handler(BaseHTTPRequestHandler):
    def reply(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        url = urlsplit(self.path)
        if url.path == "/certificate":
            cid = parse_qs(url.query).get("id", [""])[0]
            record = verify(cid)
            valid = record is not None and not record["revoked"]
            title = "Sertifikat terdaftar" if valid else "Sertifikat tidak terverifikasi"
            fields = ("name", "course", "date", "score", "id")
            details = "".join("<p><b>" + key + ":</b> " + str(record[key]).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</p>" for key in fields) if valid else ""
            body = ("<!doctype html><html><meta charset=utf-8><title>KarirMate</title><h1>KarirMate</h1><h2>" + title + "</h2>" + details + "<p>Sertifikat pelatihan internal, bukan sertifikasi BNSP.</p></html>").encode()
            self.send_response(200 if valid else 404)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        if url.path == "/health":
            return self.reply(200, {"status": "ok"})
        if url.path == "/verify":
            cid = parse_qs(url.query).get("id", [""])[0]
            record = verify(cid)
            if record is None:
                return self.reply(404, {"valid": False, "reason": "not_found"})
            if record["revoked"]:
                return self.reply(200, {"valid": False, "reason": "revoked"})
            return self.reply(200, {"valid": True, "certificate": {k:v for k,v in record.items() if k != "revoked"}})
        return self.reply(404, {"error": "not_found"})

    def do_POST(self):
        if self.path not in ("/certificates", "/revoke"):
            return self.reply(404, {"error": "not_found"})
        auth = self.headers.get("Authorization", "")
        if not TOKEN or not hmac.compare_digest(auth, "Bearer " + TOKEN):
            return self.reply(401, {"error": "unauthorized"})
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if size < 1 or size > 8192:
                return self.reply(413, {"error": "invalid_size"})
            payload = json.loads(self.rfile.read(size))
            if self.path == "/revoke":
                cid = payload.get("id") if isinstance(payload, dict) else None
                if not isinstance(cid, str) or not ID_RE.fullmatch(cid):
                    return self.reply(400, {"error": "invalid_id"})
                with connect() as conn:
                    result = conn.execute("UPDATE certificates SET revoked=1 WHERE id=?", (cid,))
                    if result.rowcount == 0:
                        return self.reply(404, {"error": "not_found"})
                return self.reply(200, {"id": cid, "revoked": True})
            cid = register(payload)
            return self.reply(201, {"id": cid, "registered": True})
        except (ValueError, KeyError, json.JSONDecodeError) as exc:
            return self.reply(400, {"error": str(exc)})
        except sqlite3.IntegrityError:
            return self.reply(409, {"error": "duplicate_id"})

if __name__ == "__main__":
    if not TOKEN:
        raise SystemExit("Set REGISTRY_ADMIN_TOKEN before starting.")
    connect().close()
    ThreadingHTTPServer(("127.0.0.1", int(os.getenv("PORT", "8792"))), Handler).serve_forever()
