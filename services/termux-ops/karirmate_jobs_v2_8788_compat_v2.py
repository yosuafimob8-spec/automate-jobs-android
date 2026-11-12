#!/usr/bin/env python3
"""KarirMate local Jobs V2 compatibility adapter (read-only).

Bridges Android GET /jobs on 127.0.0.1:8788 to the existing authenticated
KarirMate GET /jobs on 127.0.0.1:8787. It is NOT the original Jobs V2
search engine and does not discover live vacancies. No email, DB writes,
credential storage, scheduling, or outbound network access beyond loopback.

Only explicitly selected positions and locations are queried. If a selection
is missing, the result is empty with `empty_reason=filters_required`.
"""
from __future__ import annotations

import json
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlencode, urlsplit
from urllib.request import Request, urlopen

HOST = "127.0.0.1"
PORT = 8788
UPSTREAM = "http://127.0.0.1:8787"
MAX_BODY = 2_000_000


def response_object(jobs=(), *, reason=None, partial=True):
    return {
        "ok": True,
        "api_version": 2,
        "jobs": list(jobs),
        "total": len(jobs),
        "partial": partial,
        "empty_reason": reason,
        "pagination": {"page": 1, "has_more": False, "next_page": None},
        "cache": {"snapshot_id": None},
    }


def valid_external_url(value):
    try:
        parsed = urlsplit(value)
        return bool(parsed.scheme == "https" and parsed.hostname and not parsed.username and not parsed.password and parsed.port in (None, 443))
    except (TypeError, ValueError):
        return False


def fetch_local(path, authorization="", timeout=7):
    headers = {"Accept": "application/json"}
    if authorization.startswith("Bearer ") and "\n" not in authorization and "\r" not in authorization:
        headers["Authorization"] = authorization
    req = Request(UPSTREAM + path, headers=headers, method="GET")
    try:
        with urlopen(req, timeout=timeout) as res:
            body = res.read(MAX_BODY + 1)
            if len(body) > MAX_BODY:
                raise ValueError("upstream_too_large")
            return res.status, json.loads(body)
    except HTTPError as err:
        err.close()
        return err.code, None


# Defensive filtering: the legacy 8787 endpoint may return loosely matched jobs.
# Do not mark an unrelated title or a different city as a search match.
ROLE_PATTERNS = {
    "admin": (r"admin(?:istrasi|istrative|istrator|istration)?", r"clerk", r"clerical"),
    "staff": (r"staff", r"staf", r"officer", r"associate"),
    "supervisor": (r"supervisor", r"spv", r"team[ -]?lead(?:er)?", r"foreman", r"koordinator", r"coordinator"),
    "operator": (r"operator",),
    "warehouse": (r"warehouse", r"gudang", r"inventory", r"stock[ -]?(?:control|keeper)",
                  r"stockkeeper", r"storekeeper", r"pergudangan"),
    "logistic": (r"logistic(?:s)?", r"logistik", r"supply[ -]?chain", r"distribution",
                 r"distribusi", r"freight", r"shipping", r"transportasi"),
    "operational": (r"operational", r"operasional", r"operations?"),
    "production": (r"production", r"produksi", r"manufactur(?:e|ing)", r"pabrik"),
    "courier": (r"courier", r"kurir", r"delivery", r"driver"),
    "sales": (r"sales", r"penjualan"),
}
ROLE_ALIASES = {
    "administrasi": "admin", "administrator": "admin", "administrative": "admin",
    "staf": "staff", "spv": "supervisor", "gudang": "warehouse",
    "inventory": "warehouse", "logistik": "logistic", "logistics": "logistic",
    "operasional": "operational", "operations": "operational", "produksi": "production",
    "kurir": "courier", "pengiriman": "courier",
}
SENIOR_TITLES = re.compile(r"\b(?:supervisor|spv|manager|manajer|head|director|direktur|lead|leader|executive|chief|koordinator|coordinator)\b", re.I)
PLACE_PATTERNS = {
    "tangerang": (r"tangerang", r"serpong", r"bsd", r"cikupa", r"balaraja", r"karawaci",
                  r"alam[ -]?sutera", r"gading[ -]?serpong", r"pasar[ -]?kemis",
                  r"tigaraksa", r"kelapa[ -]?dua", r"karawaci", r"cipondoh"),
    "bogor": (r"bogor", r"cibinong", r"cileungsi", r"sentul", r"gunung[ -]?putri"),
    "depok": (r"depok", r"cinere", r"sawangan", r"beji"),
    "jakarta": (r"jakarta", r"jaksel", r"jakpus", r"jakbar", r"jaktim", r"jakut"),
    "bekasi": (r"bekasi", r"cikarang", r"cibitung", r"tambun"),
}


def has_any(text, patterns):
    return any(re.search(r"(?<![a-z0-9])(?:" + p + r")(?![a-z0-9])", text) for p in patterns)


def position_matches(job_title, requested):
    title = str(job_title or "").casefold()
    if not title.strip():
        return False
    tokens = re.findall(r"[a-z0-9]+", str(requested).casefold())
    if not tokens:
        return False
    categories = list(dict.fromkeys(ROLE_ALIASES.get(token, token) for token in tokens))
    if "admin" in categories or "staff" in categories or "operator" in categories:
        if SENIOR_TITLES.search(title):
            return False
    for word in categories:
        patterns = ROLE_PATTERNS.get(word, (re.escape(word),))
        if not has_any(title, patterns):
            return False
    return True


def location_matches(job_location, requested):
    actual = str(job_location or "").casefold()
    key = str(requested or "").strip().casefold()
    if not actual.strip() or not key:
        return False
    patterns = PLACE_PATTERNS.get(key, (re.escape(key),))
    return has_any(actual, patterns)


class Handler(BaseHTTPRequestHandler):
    server_version = "KarirMateJobsCompat/1.1"

    def log_message(self, fmt, *args):
        # Never include request query or authorization in logs.
        return

    def json(self, status, data):
        body = json.dumps(data, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        parsed = urlsplit(self.path)
        if parsed.scheme or parsed.netloc or parsed.fragment:
            return self.json(400, {"ok": False, "error": {"code": "invalid_path"}})
        if parsed.path == "/health":
            try:
                status, data = fetch_local("/health", timeout=3)
            except (URLError, OSError, ValueError, json.JSONDecodeError):
                status, data = 503, None
            if status == 200 and isinstance(data, dict):
                return self.json(200, {"ok": True, "status": "online", "mode": "read_only_compat", "upstream": "reachable"})
            return self.json(503, {"ok": False, "status": "upstream_unavailable"})
        if parsed.path != "/jobs":
            return self.json(404, {"ok": False, "error": {"code": "not_found"}})
        if len(self.path) > 4096:
            return self.json(414, {"ok": False, "error": {"code": "query_too_long"}})
        params = parse_qs(parsed.query, keep_blank_values=False)
        try:
            page = int(params.get("page", ["1"])[0])
            page_size = int(params.get("page_size", ["20"])[0])
        except (TypeError, ValueError):
            return self.json(400, {"ok": False, "error": {"code": "invalid_pagination"}})
        if page < 1 or not 1 <= page_size <= 100:
            return self.json(400, {"ok": False, "error": {"code": "invalid_pagination"}})
        if page != 1:
            return self.json(200, response_object(reason="compatibility_only_first_page"))
        # These filters cannot be applied by the legacy source: do not silently ignore.
        if params.get("sources") or params.get("categories"):
            return self.json(200, response_object(reason="source_filters_not_supported"))
        positions = list(dict.fromkeys(p.strip() for p in params.get("positions", []) if 0 < len(p.strip()) <= 100))[:2]
        locations = list(dict.fromkeys(p.strip() for p in params.get("locations", []) if 0 < len(p.strip()) <= 100))[:2]
        if not positions or not locations:
            return self.json(200, response_object(reason="filters_required"))
        auth = self.headers.get("Authorization", "")
        if not auth.startswith("Bearer "):
            return self.json(401, {"ok": False, "error": {"code": "authentication_required"}})

        jobs_by_link = {}
        success = False
        for position in positions:
            for location in locations:
                path = "/jobs?" + urlencode({"position": position, "location": location})
                try:
                    status, data = fetch_local(path, auth)
                except (URLError, OSError, ValueError, json.JSONDecodeError):
                    continue
                if status == 401:
                    return self.json(401, {"ok": False, "error": {"code": "session_expired"}})
                if status != 200 or not isinstance(data, dict) or data.get("success") is not True:
                    continue
                success = True
                for job in data.get("jobs") or []:
                    if not isinstance(job, dict):
                        continue
                    # Enforce both requested filters on actual job fields,
                    # even if the upstream legacy endpoint returned it.
                    if not position_matches(job.get("title"), position):
                        continue
                    if not location_matches(job.get("location"), location):
                        continue
                    link = job.get("link")
                    if not valid_external_url(link):
                        continue
                    jobs_by_link.setdefault(link, {
                        "title": job.get("title"),
                        "company": job.get("company"),
                        "location": job.get("location"),
                        "link": link,
                        "source": job.get("source"),
                        "source_id": None,
                        "published_at": None,
                        "result_type": "external",
                        "opportunity_label": job.get("match_label"),
                        "vacancy_verified": False,
                        "application_actions": [],
                    })
        if not success:
            return self.json(503, {"ok": False, "error": {"code": "legacy_jobs_source_unavailable"}})
        jobs = list(jobs_by_link.values())[:page_size]
        return self.json(200, response_object(jobs, reason=("no_local_matches" if not jobs else None)))

    def do_POST(self):
        return self.json(405, {"ok": False, "error": {"code": "read_only"}})

    do_PUT = do_DELETE = do_PATCH = do_POST


def main():
    with ThreadingHTTPServer((HOST, PORT), Handler) as server:
        print(f"KarirMate Jobs compatibility (read-only): http://{HOST}:{PORT}", flush=True)
        server.serve_forever(poll_interval=0.25)


if __name__ == "__main__":
    main()
