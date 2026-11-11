
# KM_AUTOMATIC_SELECTION_V1
def km_auto_choose(db, telegram_uid):
    """Choose mode without user setting: CV if >= 3 descriptions, otherwise explore."""
    count=0
    try:
        count=db.execute("SELECT COUNT(*) FROM km_stage1_sources WHERE user_id=? AND length(trim(coalesce(description,'')))>=80",(telegram_uid,)).fetchone()[0]
    except sqlite3.Error:
        pass
    return {"mode": "cv" if count>=3 else "random", "threshold":70,
            "decision":"described_jobs_available" if count>=3 else "insufficient_descriptions",
            "decided_by":"backend"}

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit
from pathlib import Path
import sqlite3, json, time, secrets, hashlib, threading
def register(email,password):
    from android_auth import register as implementation
    return implementation(email,password)

def login(email,password):
    from android_auth import login as implementation
    return implementation(email,password)


from runtime_paths import DB
ACCESS_REPOSITORY = None
ATTEMPTS = {}
LOCK = threading.Lock()

def initialize_sessions():
    with sqlite3.connect(DB) as db:
        db.execute("""
            CREATE TABLE IF NOT EXISTS android_sessions (
                token_hash TEXT PRIMARY KEY,
                account_id INTEGER NOT NULL,
                expires_at INTEGER NOT NULL
            )
        """)

def available_dependencies(method):
    from functools import wraps
    @wraps(method)
    def wrapped(self,*args,**kwargs):
        try: return method(self,*args,**kwargs)
        except ImportError as exc:
            return self.respond(503,{"success":False,"error":"Dependensi backend belum tersedia","missing_module":exc.name})
        except sqlite3.Error:
            return self.respond(503,{"success":False,"error":"Skema/penyimpanan backend belum tersedia"})
    return wrapped

class API(BaseHTTPRequestHandler):
    def respond(self, code, data):
        body = json.dumps(data).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


    def authenticated_account(self):
        auth = self.headers.get("Authorization", "")
        if not auth.startswith("Bearer "):
            return None
        digest = hashlib.sha256(auth[7:].encode()).hexdigest()
        with sqlite3.connect(DB) as db:
            row = db.execute(
                "SELECT account_id FROM android_sessions "
                "WHERE token_hash=? AND expires_at>?",
                (digest, int(time.time()))
            ).fetchone()
        return row[0] if row else None

    def account_status(self):
        global ACCESS_REPOSITORY
        if ACCESS_REPOSITORY is None:
            from account_access_policy import ReadOnlyAccessRepository
            ACCESS_REPOSITORY = ReadOnlyAccessRepository(DB)
        return ACCESS_REPOSITORY.auth_me(self.headers.get("Authorization", ""))

    @available_dependencies
    def do_GET(self):
        path = urlsplit(self.path).path
        from auto_apply_api import handle
        if handle(self,path,'GET'): return

        if path == "/karirmate/candidates":
            account_id = self.authenticated_account()
            if account_id is None:
                return self.respond(401, {"error": "Unauthorized"})

            with sqlite3.connect(DB) as db:
                db.row_factory = sqlite3.Row

                row = db.execute(
                    "SELECT telegram_user_id FROM android_accounts WHERE id=?",
                    (account_id,)
                ).fetchone()

                if not row or row["telegram_user_id"] is None:
                    return self.respond(409, {"error": "Akun belum terhubung"})

                uid = int(row["telegram_user_id"])

                import karirmate_integrasi_final as km
                km.init(db)

                if not km.pro(db, uid).get("pro"):
                    return self.respond(403, {"error": "PRO required"})

                items = []

                for r in db.execute("""
                    SELECT company,email,position,source_url
                    FROM km_stage1_sources
                    WHERE user_id=?
                    ORDER BY id DESC
                """, (uid,)):
                    items.append({
                        "company": r["company"],
                        "email": r["email"],
                        "position": r["position"],
                        "source": r["source_url"],
                        "status": "needs_review"
                    })

                for r in db.execute("""
                    SELECT company,email,position,source
                    FROM km_companies
                    WHERE user_id=?
                    ORDER BY id DESC
                """, (uid,)):
                    if not any(
                        x["email"].lower() == (r["email"] or "").lower()
                        for x in items
                    ):
                        items.append({
                            "company": r["company"],
                            "email": r["email"],
                            "position": r["position"],
                            "source": r["source"],
                            "status": "needs_review"
                        })

                return self.respond(200, {
                    "success": True,
                    "total": len(items),
                    "items": items,
                    "sending_enabled": False
                })

        if path == "/karirmate/selection":
            account_id=self.authenticated_account()
            if account_id is None:
                return self.respond(401,{"error":"Unauthorized"})
            with sqlite3.connect(DB) as db:
                row=db.execute("SELECT telegram_user_id FROM android_accounts WHERE id=?",(account_id,)).fetchone()
                if not row or row[0] is None:
                    return self.respond(409,{"error":"Akun belum terhubung"})
                return self.respond(200,{"success":True,"selection":km_auto_choose(db,int(row[0]))})
        path = urlsplit(self.path).path
        if path == "/karirmate/report":
            # KM_REPORT_ACCOUNT_SCOPE_V44
            from auto_apply_api import handle as auto_apply_handle
            auto_apply_handle(self, path, "GET")
            return
        if path == "/auto-apply/status":
            # Delegate to account-scoped Auto Apply handler; no Telegram requirement for status.
            from auto_apply_api import handle as auto_apply_handle
            auto_apply_handle(self, path, "GET")
            return
            uid = self.authenticated_account()
            if uid is None:
                return self.respond(401, {"error": "Unauthorized"})
            with sqlite3.connect(DB) as db:
                row = db.execute(
                    "SELECT telegram_user_id FROM android_accounts WHERE id=?",
                    (uid,)
                ).fetchone()
            if not row:
                return self.respond(401, {"error": "Akun tidak ditemukan"})
            if row[0] is None:
                return self.respond(200, {'success': True, 'operator_send_allowed': False, 'schedule_status': 'telegram_link_required', 'gmail_connected': False, 'cv_uploaded': False, 'auto_apply': {'enabled': False, 'consent_granted': False, 'positions': [], 'locations': [], 'preferences_enforced': False}, 'action_required': 'telegram_link', 'message': 'Akun belum tertaut dengan identitas Telegram untuk Gmail Auto Apply. Pengiriman nonaktif.'})
            from gmail_oauth_client import status
            from phase511_controls import state
            try:
                return self.respond(200, {
                    "success": True,
                    "gmail_connected": status(row[0]).get("connected") is True,
                    "auto_apply": state(row[0])
                })
            except Exception:
                return self.respond(503, {"error": "Status tidak tersedia"})

        if path == "/profile":
            uid = self.authenticated_account()
            if uid is None:
                return self.respond(401, {"error": "Unauthorized"})
            with sqlite3.connect(DB) as db:
                db.row_factory = sqlite3.Row
                row = db.execute(
                    "SELECT * FROM android_profiles WHERE account_id=?",
                    (uid,)
                ).fetchone()
            if not row:
                return self.respond(200, {"completed": False})
            result = dict(row)
            result["desired_positions"] = json.loads(result["desired_positions"])
            result["preferred_locations"] = json.loads(result["preferred_locations"])
            result["completed"] = bool(result["completed"])
            return self.respond(200, result)

        if path == "/jobs":
            from urllib.parse import parse_qs
            from idjobs_feed import fetch_feed

            uid = self.authenticated_account()
            if uid is None:
                return self.respond(401, {"error": "Unauthorized"})

            params = parse_qs(urlsplit(self.path).query)
            position = params.get("position", [""])[0].strip()
            location = params.get("location", [""])[0].strip()

            allowed_positions = {
                "Admin Warehouse", "Admin Logistic",
                "Admin Operational", "Staff Warehouse",
                "Staff Logistic", "Supervisor Warehouse",
                "Supervisor Logistic"
            }
            allowed_locations = {
                "Tangerang", "Bogor", "Depok",
                "Jakarta", "Bekasi"
            }

            if len(position) > 120 or len(location) > 120:
                return self.respond(400, {"error": "Filter tidak valid"})

            try:
                jobs = fetch_feed(position, location, limit=20)
                return self.respond(200, {
                    "success": True,
                    "position": position,
                    "location": location,
                    "total": len(jobs),
                    "jobs": jobs
                })
            except Exception:
                return self.respond(500, {"error": "Pencarian gagal"})

        if path == "/health":
            try:
                with sqlite3.connect(f"file:{DB}?mode=ro", uri=True) as db:
                    db.execute("SELECT 1")
                return self.respond(200, {
                    "status": "online",
                    "app": "AutoMateSmartBot",
                    "database": "connected"
                })
            except sqlite3.Error:
                return self.respond(503, {"error": "Database unavailable"})

        if path == "/auth/me":
            auth = self.headers.get("Authorization", "")
            if not auth.startswith("Bearer "):
                return self.respond(401, {"error": "Unauthorized"})

            try:
                account = self.account_status()
            except (sqlite3.Error,ImportError):
                return self.respond(503, {"error": "Account status unavailable"})
            if account is None:
                return self.respond(401, {"error": "Invalid session"})
            return self.respond(200, account)

        return self.respond(404, {"error": "Not found"})

    @available_dependencies
    def do_PUT(self):
        from auto_apply_api import handle
        if handle(self,urlsplit(self.path).path,'PUT'): return
        if urlsplit(self.path).path != "/profile":
            return self.respond(404, {"error": "Not found"})
        uid = self.authenticated_account()
        if uid is None:
            return self.respond(401, {"error": "Unauthorized"})
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 8192:
                return self.respond(413, {"error": "Invalid request size"})
            data = json.loads(self.rfile.read(size))
            fields = [
                "fullName", "whatsapp", "domicile",
                "education", "desiredPositions",
                "preferredLocations", "applicationEmail"
            ]
            if not isinstance(data, dict):
                raise ValueError()
            if any(k not in data for k in fields):
                raise ValueError()
            for k in fields:
                if k in ("desiredPositions", "preferredLocations"):
                    if (not isinstance(data[k], list) or len(data[k]) > 20
                        or any(not isinstance(x, str) or len(x) > 100
                               for x in data[k])):
                        raise ValueError()
                elif not isinstance(data[k], str) or len(data[k]) > 254:
                    raise ValueError()
            if not data["fullName"].strip() or not data["applicationEmail"].strip():
                raise ValueError()
        except (ValueError, TypeError, UnicodeDecodeError):
            return self.respond(400, {"error": "Invalid profile"})

        with sqlite3.connect(DB, timeout=20) as db:
            db.execute("""
                INSERT INTO android_profiles
                (account_id, full_name, whatsapp, domicile,
                 education, desired_positions, preferred_locations,
                 application_email, completed, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
                ON CONFLICT(account_id) DO UPDATE SET
                full_name=excluded.full_name,
                whatsapp=excluded.whatsapp,
                domicile=excluded.domicile,
                education=excluded.education,
                desired_positions=excluded.desired_positions,
                preferred_locations=excluded.preferred_locations,
                application_email=excluded.application_email,
                completed=1,
                updated_at=excluded.updated_at
            """, (
                uid, data["fullName"].strip(), data["whatsapp"].strip(),
                data["domicile"].strip(), data["education"].strip(),
                json.dumps(data["desiredPositions"]),
                json.dumps(data["preferredLocations"]),
                data["applicationEmail"].strip(), int(time.time())
            ))
        return self.respond(200, {"success": True, "completed": True})

    @available_dependencies
    def do_POST(self):
        path = urlsplit(self.path).path
        from auto_apply_api import handle
        if handle(self,path,'POST'): return
        # KARIRMATE_PRO_GUARD_V1
        if path == "/karirmate/auto-apply/request":
            account_id=self.authenticated_account()
            if account_id is None:
                return self.respond(401,{"error":"Unauthorized"})
            with sqlite3.connect(DB) as db:
                row=db.execute("SELECT telegram_user_id FROM android_accounts WHERE id=?",(account_id,)).fetchone()
                if not row or row[0] is None:
                    return self.respond(409,{"error":"Akun belum terhubung"})
                try:
                    import karirmate_integrasi_final as km_pro
                    km_pro.init(db)
                    entitlement=km_pro.pro(db,int(row[0]))
                except Exception:
                    return self.respond(503,{"error":"PRO validation unavailable"})
                if not entitlement.get("pro"):
                    return self.respond(403,{"error":"PRO required","reason":entitlement.get("reason")})
                return self.respond(409,{"error":"Aktivasi menunggu persetujuan eksplisit dan pengaturan scheduler; belum mengirim email", "selection":km_auto_choose(db,int(row[0]))})
        if urlsplit(self.path).path == "/auto-apply/enable":
            account_id = self.authenticated_account()
            if account_id is None:
                return self.respond(401, {"error": "Unauthorized"})
            try:
                import karirmate_integrasi_final as km_pro
                with sqlite3.connect(DB) as km_db:
                    km_pro.init(km_db)
                    km_row = km_db.execute(
                        "SELECT telegram_user_id FROM android_accounts WHERE id=?",
                        (account_id,)
                    ).fetchone()
                    if not km_row or km_row[0] is None:
                        return self.respond(409, {"error": "Akun belum terhubung"})
                    km_status = km_pro.pro(km_db, int(km_row[0]))
                if not km_status.get("pro"):
                    return self.respond(403, {"error": "Fitur khusus PRO", "pro": False,
                                              "reason": km_status.get("reason", "inactive")})
            except Exception:
                return self.respond(503, {"error": "Validasi PRO tidak tersedia"})
            # PRO aktif masih memerlukan persetujuan pengguna dan verifikasi pembayaran.
            # Handler lama di bawah tetap menolak aktivasi (fail closed).
        path = urlsplit(self.path).path
        if path in ("/auto-apply/enable", "/auto-apply/pause"):
            uid = self.authenticated_account()
            if uid is None:
                return self.respond(401, {"error": "Unauthorized"})
            with sqlite3.connect(DB) as db:
                row = db.execute("SELECT telegram_user_id FROM android_accounts WHERE id=?", (uid,)).fetchone()
            if not row or row[0] is None:
                return self.respond(409, {"error": "Gmail belum terhubung"})
            from phase511_controls import enable, pause, state
            if path.endswith("/pause"):
                pause(row[0])
                return self.respond(200, {"success": True, "auto_apply": state(row[0])})

            try:
                from gmail_oauth_client import status as gmail_status
                gmail_connected = gmail_status(row[0]).get("connected") is True
                ok, message = enable(row[0], gmail_connected)
            except Exception:
                return self.respond(503, {"error": "Pemeriksaan Gmail atau aktivasi gagal"})

            if not ok:
                return self.respond(409, {"success": False, "error": message})

            return self.respond(200, {
                "success": True,
                "message": message,
                "auto_apply": state(row[0])
            })


        if path in ("/auth/email-verification/request", "/auth/email-verification/confirm"):
            try:
                size = int(self.headers.get("Content-Length", "0"))
                if size < 0 or size > 1024:
                    return self.respond(413, {"error": "Invalid request size"})
                payload = json.loads(self.rfile.read(size)) if size else {}
                if not isinstance(payload, dict):
                    raise ValueError("Invalid JSON")
            except (ValueError, TypeError, json.JSONDecodeError):
                return self.respond(400, {"error": "Invalid JSON"})
            from email_verification_api import handle_email_verification
            status, result = handle_email_verification(
                path, self.headers.get("Authorization", ""), payload
            )
            return self.respond(status, result)

        if path == "/auth/google":
            key = self.client_address[0] + ":" + path
            now = time.monotonic()

            with LOCK:
                ATTEMPTS[key] = [
                    t for t in ATTEMPTS.get(key, [])
                    if now - t < 300
                ]
                if len(ATTEMPTS[key]) >= 10:
                    return self.respond(
                        429, {"error": "Too many attempts"}
                    )
                ATTEMPTS[key].append(now)

            try:
                size = int(
                    self.headers.get("Content-Length", "0")
                )
                if size <= 0 or size > 10000:
                    return self.respond(
                        413, {"error": "Invalid request size"}
                    )

                data = json.loads(self.rfile.read(size))
                if not isinstance(data, dict):
                    raise ValueError("Invalid JSON")
            except (ValueError, TypeError, json.JSONDecodeError):
                return self.respond(
                    400, {"error": "Invalid JSON"}
                )

            from google_login_auth import google_login
            code, result = google_login(data.get("id_token"))
            return self.respond(code, result)

        if path not in ("/auth/register", "/auth/login"):
            return self.respond(404, {"error": "Not found"})

        key = self.client_address[0] + ":" + path
        now = time.monotonic()
        with LOCK:
            ATTEMPTS[key] = [
                t for t in ATTEMPTS.get(key, []) if now-t < 300
            ]
            if len(ATTEMPTS[key]) >= 10:
                return self.respond(429, {"error": "Too many attempts"})
            ATTEMPTS[key].append(now)

        try:
            size = int(self.headers.get("Content-Length", "0"))
            if size <= 0 or size > 4096:
                return self.respond(413, {"error": "Invalid request size"})
            data = json.loads(self.rfile.read(size))
            email = data.get("email", "")
            password = data.get("password", "")
            if not isinstance(email, str) or not isinstance(password, str):
                raise ValueError("Invalid input")
        except (ValueError, TypeError, json.JSONDecodeError):
            return self.respond(400, {"error": "Invalid JSON"})

        if path == "/auth/register":
            result = register(email, password)
            return self.respond(201 if result["success"] else 400, result)

        result = login(email, password)
        if not result["success"]:
            return self.respond(401, result)

        token = secrets.token_urlsafe(32)
        digest = hashlib.sha256(token.encode()).hexdigest()
        expires = int(time.time()) + 86400

        with sqlite3.connect(DB) as db:
            db.execute("""
                INSERT INTO android_sessions
                (token_hash, account_id, expires_at)
                VALUES (?, ?, ?)
            """, (digest, result["account_id"], expires))

        return self.respond(200, {
            "success": True,
            "access_token": token,
            "token_type": "Bearer",
            "expires_at": expires,
            "email_verified": result["email_verified"]
        })

if __name__ == "__main__":
    initialize_sessions()
    ThreadingHTTPServer(("127.0.0.1", 8787), API).serve_forever()
