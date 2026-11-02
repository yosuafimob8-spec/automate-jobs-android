"""Training HTTP handler mixin; uses the existing KarirMate auth/me endpoint.
Deploy only behind HTTPS and configure KARIRMATE_AUTH_ME_URL to the trusted
backend's fixed /auth/me URL. Never accept user IDs from request bodies.
"""
import json
import os
import urllib.error
import urllib.request
from urllib.parse import urlsplit
from trusted_training import initialize, public_questions, submit_exam

MAX_BODY = 8192

def authenticated_identity(authorization, *, opener=urllib.request.urlopen):
    url = os.getenv("KARIRMATE_AUTH_ME_URL", "")
    if not url or not (url.startswith("https://") or (os.getenv("ALLOW_LOCAL_AUTH_HTTP") == "1" and url.startswith("http://127.0.0.1:"))):
        raise RuntimeError("trusted auth/me URL not configured")
    if not authorization.startswith("Bearer ") or not authorization[7:].strip():
        return None
    req = urllib.request.Request(url, headers={"Authorization":authorization, "Accept":"application/json"}, method="GET")
    try:
        with opener(req, timeout=8) as response:
            if response.status != 200:
                return None
            data = json.load(response)
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, ValueError):
        return None
    if not isinstance(data, dict):
        return None
    # Only use identity returned by the trusted auth backend.
    user_id = data.get("id")
    if user_id is None and isinstance(data.get("user"), dict):
        user_id = data["user"].get("id")
    if not isinstance(user_id, (str, int)) or isinstance(user_id, bool) or not str(user_id).strip():
        return None
    name = data.get("name")
    if name is None and isinstance(data.get("user"), dict):
        name = data["user"].get("name")
    return str(user_id), (name if isinstance(name, str) and name.strip() else "Peserta KarirMate")

class TrainingApiMixin:
    def handle_training(self):
        path = urlsplit(self.path).path
        if not path.startswith("/training/"):
            return False
        try:
            identity = authenticated_identity(self.headers.get("Authorization", ""))
        except RuntimeError:
            self.reply(503, {"error":"auth_backend_not_configured"})
            return True
        if identity is None:
            self.reply(401, {"error":"unauthorized"})
            return True
        user_id, name = identity
        if self.command == "GET" and path.startswith("/training/courses/") and path.endswith("/questions"):
            course_id = path[len("/training/courses/"):-len("/questions")]
            if not course_id or "/" in course_id:
                self.reply(404, {"error":"not_found"})
                return True
            try:
                with self.training_connection() as conn:
                    questions = public_questions(conn, course_id)
                self.reply(200, {"courseId":course_id, "questions":questions})
            except ValueError:
                self.reply(404, {"error":"unknown_course"})
            return True
        if self.command == "POST" and path == "/training/exams":
            try:
                size = int(self.headers.get("Content-Length", "0"))
                if not 1 <= size <= MAX_BODY:
                    self.reply(413, {"error":"invalid_size"})
                    return True
                payload = json.loads(self.rfile.read(size))
                if not isinstance(payload, dict):
                    raise ValueError("invalid_payload")
                with self.training_connection() as conn:
                    result = submit_exam(conn, user_id, payload.get("courseId"), payload.get("answers"), name)
                self.reply(200, result)
            except (ValueError, TypeError):
                self.reply(400, {"error":"invalid_or_incomplete_assessment"})
            except __import__("sqlite3").IntegrityError:
                self.reply(409, {"error":"concurrent_or_duplicate_submission"})
            return True
        self.reply(404, {"error":"not_found"})
        return True
