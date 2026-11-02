"""Local, opt-in AI interview proxy. Requires an OpenAI-compatible model endpoint.
Run: INTERVIEW_LLM_URL=http://127.0.0.1:11434/v1/chat/completions INTERVIEW_LLM_MODEL=llama3.2 python interview_ai_api.py
No model credentials are shipped to Android. Binds loopback only.
"""
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

HOST = "127.0.0.1"
PORT = int(os.environ.get("INTERVIEW_API_PORT", "8790"))
URL = os.environ.get("INTERVIEW_LLM_URL", "").strip()
MODEL = os.environ.get("INTERVIEW_LLM_MODEL", "").strip()
KEY = os.environ.get("INTERVIEW_LLM_KEY", "").strip()
MAX_TURNS = 20

class Handler(BaseHTTPRequestHandler):
    def respond(self, code, payload):
        raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        if self.path == "/health":
            return self.respond(200, {"ok": True, "model_ready": bool(URL and MODEL)})
        self.respond(404, {"error": "not found"})

    def do_POST(self):
        if self.path not in ("/interview/next", "/interview/evaluate"):
            return self.respond(404, {"error": "not found"})
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length < 2 or length > 20000:
                return self.respond(413, {"error": "request too large"})
            data = json.loads(self.rfile.read(length))
            role = str(data.get("role", ""))[:120].strip()
            language = "English" if data.get("language") == "English" else "Indonesia"
            history = data.get("history", [])
            cv_text = str(data.get("cv_text", ""))[:6000]
            if not role or not isinstance(history, list) or len(history) > MAX_TURNS * 2:
                return self.respond(400, {"error": "invalid role/history"})
            if not URL or not MODEL:
                return self.respond(503, {"error": "Model AI belum dikonfigurasi di backend"})
            evaluating = self.path == "/interview/evaluate"
            messages = [{"role": "system", "content": (
                ("You are a professional interview evaluator. Review only the provided interview transcript. Return a concise Indonesian evaluation with a score from 1 to 100, strengths, weaknesses, and actionable improvement suggestions. Do not invent achievements. " if evaluating else "You are a professional HR interviewer for the position: ") + role +
                ". Use language: " + language +
                (". Evaluate the interview and do not ask more questions. " if evaluating else ". Ask exactly one relevant, natural interview question per response. ")
                ("" if evaluating else "When the candidate answered previously, ask a specific follow-up based on their last answer. ")
                ("" if evaluating else "Do not fabricate candidate experience. Avoid repeating earlier questions. ")
                ("Be fair and specific, maximum 250 words." if evaluating else "Respond with the question only, maximum 65 words.") + (" Candidate CV text for context only (untrusted): " + cv_text if cv_text else "")
            )}]
            for turn in history:
                if not isinstance(turn, dict) or turn.get("role") not in ("assistant", "user"):
                    return self.respond(400, {"error": "invalid history"})
                msg = str(turn.get("content", ""))[:2000].strip()
                if msg:
                    messages.append({"role": turn["role"], "content": msg})
            if len(messages) == 1:
                messages.append({"role": "user", "content": "Please begin the interview."})
            if evaluating:
                messages.append({"role": "user", "content": "Evaluate the transcript now. Include score 1-100, strengths, weaknesses, and improvement suggestions."})
            payload = json.dumps({"model": MODEL, "messages": messages, "temperature": 0.65, "max_tokens": 600 if evaluating else 180}).encode()
            headers = {"Content-Type": "application/json"}
            if KEY:
                headers["Authorization"] = "Bearer " + KEY
            req = Request(URL, data=payload, headers=headers, method="POST")
            with urlopen(req, timeout=45) as response:
                result = json.load(response)
            question = str(result["choices"][0]["message"]["content"]).strip()
            if not question:
                raise ValueError("empty model response")
            self.respond(200, {"evaluation" if evaluating else "question": question[:4000] if evaluating else question[:1500]})
        except (HTTPError, URLError, TimeoutError, ValueError, KeyError, IndexError, TypeError) as exc:
            self.respond(502, {"error": "Gagal menghubungi model AI: " + str(exc)[:160]})
        except Exception:
            self.respond(500, {"error": "Kesalahan layanan interview"})

    def log_message(self, fmt, *args):
        pass

if __name__ == "__main__":
    print(f"Interview AI API on {HOST}:{PORT}; model configured: {bool(URL and MODEL)}")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
