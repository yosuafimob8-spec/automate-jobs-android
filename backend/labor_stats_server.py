"""KarirMate BPS labor-statistics proxy. Python 3.10+, standard library only.

Set LABOR_BPS_FEED_URL to a trusted HTTPS endpoint returning the normalized
BPS JSON contract documented in docs/BPS_LABOR_DASHBOARD_INTEGRATION.md.
This service does not scrape BPS or invent data. The BPS WebAPI adapter / API
key and official indicator mapping must be configured separately.
"""
import json
import os
import threading
import time
import urllib.request
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

HOST = os.getenv("LABOR_HOST", "127.0.0.1")
PORT = int(os.getenv("LABOR_PORT", "8793"))
FEED = os.getenv("LABOR_BPS_FEED_URL", "")
CACHE = Path(os.getenv("LABOR_CACHE_FILE", str(Path.home() / ".karirmate_bps_labor_cache.json")))
TTL = max(3600, int(os.getenv("LABOR_REFRESH_SECONDS", "86400")))
LOCK = threading.Lock()
_last_attempt = 0.0


def validate(payload):
    if not isinstance(payload, dict) or payload.get("source") != "BPS":
        raise ValueError("Expected attributed BPS data")
    if not isinstance(payload.get("period"), str) or not payload["period"].strip():
        raise ValueError("Missing reference period")
    metrics = payload.get("metrics")
    required = ("unemployment", "tpt", "poverty", "povertyRate", "workforce", "employed", "wages")
    if not isinstance(metrics, dict) or any(not isinstance(metrics.get(k), str) or not metrics[k].strip() for k in required):
        raise ValueError("Incomplete BPS metrics")
    for key in ("unemploymentSeries", "povertySeries"):
        series = payload.get(key, [])
        if not isinstance(series, list):
            raise ValueError("Invalid time series")
        previous = None
        for point in series:
            if not isinstance(point, dict) or not isinstance(point.get("period"), str) or not isinstance(point.get("value"), (int, float)):
                raise ValueError("Invalid time-series observation")
            if previous is not None and point["period"] <= previous:
                raise ValueError("Series must be chronologically ordered")
            previous = point["period"]
    return payload


def refresh():
    global _last_attempt
    with LOCK:
        if time.monotonic() - _last_attempt < TTL:
            return
        _last_attempt = time.monotonic()
        parsed = urlparse(FEED)
        if parsed.scheme != "https" or not parsed.hostname:
            return
        try:
            request = urllib.request.Request(FEED, headers={"Accept": "application/json", "User-Agent": "KarirMate-Labor-Stats/1.0"})
            with urllib.request.urlopen(request, timeout=12) as response:
                body = response.read(256_001)
            if len(body) > 256_000:
                raise ValueError("Oversized feed")
            data = validate(json.loads(body))
            data["fetchedAt"] = datetime.now(timezone.utc).isoformat()
            CACHE.parent.mkdir(parents=True, exist_ok=True)
            temporary = CACHE.with_suffix(".tmp")
            temporary.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
            temporary.replace(CACHE)
        except (OSError, ValueError, json.JSONDecodeError):
            pass  # Retain previously validated cache; never fabricate a value.


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            return self.respond(200, {"ok": True, "feedConfigured": bool(FEED)})
        if self.path != "/public/labor-stats":
            return self.respond(404, {"error": "not_found"})
        refresh()
        try:
            data = validate(json.loads(CACHE.read_text(encoding="utf-8")))
        except (OSError, ValueError, json.JSONDecodeError):
            return self.respond(503, {"error": "bps_data_unavailable", "message": "No verified BPS data has been loaded"})
        return self.respond(200, data)

    def respond(self, code, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "public, max-age=300" if code == 200 else "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    print(f"Labor proxy listening on {HOST}:{PORT}; feed configured: {bool(FEED)}")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
