"""Separate loopback-only HTTP API for Latihan Psikotes (127.0.0.1:8789)."""
from __future__ import annotations

from collections import deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import logging
import math
import re
import threading
import time
from urllib.parse import parse_qs, urlsplit

from psychotest_service import API_VERSION, PsychotestError, PsychotestService
from search_identity import IdentityError, android_subject

HOST, PORT = '127.0.0.1', 8789
LOG = logging.getLogger('automate.jobs.v2.psychotest')
SESSION_PATH = re.compile(r'^/psychotests/sessions/([0-9a-f]{32})(?:/(questions|answers|submit|result|retry-wrong))?$')
MAX_BODY = 262144
ALLOWED_QUERY = {'limit', 'offset', 'board', 'test_id'}


class ApiError(Exception):
    def __init__(self, status, code, message, details=None, retry_after=None):
        super().__init__(message)
        self.status, self.code, self.message = status, code, message
        self.details, self.retry_after = details, retry_after


class RateLimiter:
    def __init__(self, maximum=240, window=60, clock=time.monotonic):
        self.maximum, self.window, self.clock = maximum, window, clock
        self.events = deque()
        self.lock = threading.Lock()

    def check(self):
        with self.lock:
            now = self.clock()
            while self.events and self.events[0] <= now - self.window:
                self.events.popleft()
            if len(self.events) >= self.maximum:
                raise ApiError(429, 'RATE_LIMITED', 'Terlalu banyak permintaan; coba lagi',
                               retry_after=max(1, math.ceil(self.events[0] + self.window - now)))
            self.events.append(now)


class PsychotestApi:
    def __init__(self, service, *, identity_resolver=None, request_limit=240, clock=time.monotonic):
        self.service = service
        self.identity_resolver = identity_resolver or android_subject
        self.request_limiter = RateLimiter(request_limit, clock=clock)

    def identity(self, authorization):
        if not authorization:
            raise ApiError(401, 'AUTH_REQUIRED', 'Sesi akun Android diperlukan')
        try:
            subject, identity = self.identity_resolver(authorization)
        except IdentityError as exc:
            raise ApiError(exc.status, exc.code, exc.message, retry_after=2 if exc.status == 503 else None) from None
        if not subject.startswith('android:') or subject == 'android:':
            raise ApiError(401, 'AUTH_REQUIRED', 'Sesi akun Android diperlukan')
        return subject, identity

    def catalog(self):
        return self.service.catalog()

    def health(self, port):
        return self.service.health(port)

    def leaderboard(self, query):
        board = self._one(query, 'board') or 'all'
        test_id = self._one(query, 'test_id')
        limit = self._integer(query, 'limit', 10, 1, 50)
        return self.service.leaderboard(board, test_id, limit)

    def history(self, authorization, query):
        subject, _ = self.identity(authorization)
        limit = self._integer(query, 'limit', 20, 1, 100)
        offset = self._integer(query, 'offset', 0, 0, 100000)
        return self.service.history(subject, limit, offset)

    def stats(self, authorization):
        subject, _ = self.identity(authorization)
        return self.service.stats(subject)

    def start(self, authorization, body):
        subject, identity = self.identity(authorization)
        if not isinstance(body, dict) or not isinstance(body.get('test_id'), str):
            raise ApiError(400, 'INVALID_PARAMETERS', 'test_id wajib berupa teks')
        test_id = body['test_id'].strip()
        if not re.fullmatch(r'[a-z0-9_]{2,64}', test_id):
            raise ApiError(400, 'INVALID_PARAMETERS', 'test_id tidak valid')
        return self.service.start_session(test_id, subject, identity.get('account_id'))

    def sessions(self, authorization):
        subject, _ = self.identity(authorization)
        return self.service.active_sessions(subject)

    def session(self, authorization, session_id, action=None, body=None):
        subject, _ = self.identity(authorization)
        if action is None:
            return self.service.get_session(session_id, subject)
        if action == 'questions':
            return self.service.questions(session_id, subject)
        if action == 'answers':
            if not isinstance(body, dict) or not isinstance(body.get('answers'), list):
                raise ApiError(400, 'INVALID_PARAMETERS', 'answers wajib berupa array')
            revision = body.get('revision')
            if revision is not None and (type(revision) is not int or revision < 0):
                raise ApiError(400, 'INVALID_PARAMETERS', 'revision harus bilangan bulat positif atau nol')
            return self.service.save_answers(session_id, subject, body['answers'], revision)
        if action == 'submit':
            return self.service.submit(session_id, subject)
        if action == 'retry-wrong':
            return self.service.retry_wrong(session_id, subject)
        if action == 'result':
            return self.service.result(session_id, subject)
        raise ApiError(404, 'NOT_FOUND', 'Endpoint tidak ditemukan')

    @staticmethod
    def _one(query, name):
        values = query.get(name, [])
        if len(values) > 1:
            raise ApiError(400, 'INVALID_PARAMETERS', f'Parameter {name} hanya boleh satu nilai')
        if not values:
            return None
        value = values[0].strip()
        if not value or len(value) > 64 or any(ord(c) < 32 for c in value):
            raise ApiError(400, 'INVALID_PARAMETERS', f'Parameter {name} tidak valid')
        return value

    @classmethod
    def _integer(cls, query, name, default, minimum, maximum):
        value = cls._one(query, name)
        if value is None:
            return default
        if not re.fullmatch(r'[0-9]{1,9}', value):
            raise ApiError(400, 'INVALID_PARAMETERS', f'{name} harus bilangan bulat')
        number = int(value)
        if not minimum <= number <= maximum:
            raise ApiError(400, 'INVALID_PARAMETERS', f'{name} harus antara {minimum} dan {maximum}')
        return number


class PsychotestHandler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'
    server_version, sys_version = 'AutoMatePsychotestV2/1', ''

    def setup(self):
        self.request.settimeout(10)
        super().setup()

    def log_message(self, format, *args):
        pass

    def _send(self, status, body, headers=None):
        raw = json.dumps(body, ensure_ascii=False, allow_nan=False, separators=(',', ':')).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(raw)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-API-Version', str(API_VERSION))
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Connection', 'close')
        for key, value in (headers or {}).items():
            self.send_header(key, value)
        self.end_headers()
        self.close_connection = True
        if self.command != 'HEAD':
            self.wfile.write(raw)

    def _error(self, exc):
        body = {'ok': False, 'api_version': API_VERSION, 'error': {'code': exc.code, 'message': exc.message}}
        if exc.details is not None:
            body['error']['details'] = exc.details
        headers = {'Retry-After': str(exc.retry_after)} if exc.retry_after else {}
        self._send(exc.status, body, headers)

    def send_error(self, code, message=None, explain=None):
        self._error(ApiError(code, 'HTTP_ERROR', 'Permintaan HTTP tidak valid'))

    def _query(self, parsed):
        try:
            params = parse_qs(parsed.query, keep_blank_values=True, max_num_fields=20, errors='strict')
        except (ValueError, UnicodeError):
            raise ApiError(400, 'INVALID_PARAMETERS', 'Query string tidak valid') from None
        unknown = set(params) - ALLOWED_QUERY
        if unknown:
            raise ApiError(400, 'INVALID_PARAMETERS', 'Parameter tidak dikenal: ' + ', '.join(sorted(unknown)))
        return params

    def _body(self):
        raw_length = self.headers.get('Content-Length')
        if raw_length is None:
            return {}
        if not re.fullmatch(r'[0-9]{1,9}', raw_length):
            raise ApiError(400, 'INVALID_BODY', 'Content-Length tidak valid')
        length = int(raw_length)
        if length > MAX_BODY:
            raise ApiError(413, 'BODY_TOO_LARGE', 'Body permintaan terlalu besar')
        raw = self.rfile.read(length)
        if not raw:
            return {}
        content_type = (self.headers.get('Content-Type') or '').split(';')[0].strip().lower()
        if content_type != 'application/json':
            raise ApiError(415, 'UNSUPPORTED_MEDIA_TYPE', 'Gunakan Content-Type application/json')
        try:
            body = json.loads(raw)
        except (ValueError, UnicodeError):
            raise ApiError(400, 'INVALID_JSON', 'Body JSON tidak valid') from None
        if not isinstance(body, dict):
            raise ApiError(400, 'INVALID_JSON', 'Body harus berupa objek JSON')
        return body

    def do_GET(self):
        try:
            self.server.api.request_limiter.check()
            if len(self.path) > 4096:
                raise ApiError(414, 'URI_TOO_LONG', 'URL terlalu panjang')
            parsed = urlsplit(self.path)
            if parsed.scheme or parsed.netloc or parsed.fragment:
                raise ApiError(400, 'INVALID_PARAMETERS', 'Target HTTP harus path lokal')
            query = self._query(parsed)
            path = parsed.path.rstrip('/') or '/'
            api = self.server.api
            if path == '/health':
                if query:
                    raise ApiError(400, 'INVALID_PARAMETERS', 'Health tidak menerima parameter')
                body = api.health(self.server.server_address[1])
            elif path == '/psychotests/catalog':
                if query:
                    raise ApiError(400, 'INVALID_PARAMETERS', 'Katalog tidak menerima parameter')
                body = api.catalog()
            elif path == '/psychotests/leaderboard':
                body = api.leaderboard(query)
            elif path == '/psychotests/history':
                body = api.history(self.headers.get('Authorization'), query)
            elif path == '/psychotests/stats':
                if query:
                    raise ApiError(400, 'INVALID_PARAMETERS', 'Statistik tidak menerima parameter')
                body = api.stats(self.headers.get('Authorization'))
            elif path == '/psychotests/sessions':
                if query:
                    raise ApiError(400, 'INVALID_PARAMETERS', 'Daftar sesi tidak menerima parameter')
                body = api.sessions(self.headers.get('Authorization'))
            else:
                match = SESSION_PATH.fullmatch(path)
                if not match:
                    raise ApiError(404, 'NOT_FOUND', 'Endpoint tidak ditemukan')
                if query:
                    raise ApiError(400, 'INVALID_PARAMETERS', 'Endpoint sesi tidak menerima parameter')
                session_id, action = match.groups()
                if action in ('answers', 'submit', 'retry-wrong'):
                    raise ApiError(405, 'METHOD_NOT_ALLOWED', 'Gunakan metode yang sesuai')
                body = api.session(self.headers.get('Authorization'), session_id, action)
            self._send(200, body)
        except ApiError as exc:
            self._error(exc)
        except PsychotestError as exc:
            self._error(ApiError(exc.status, exc.code, exc.message, exc.details))
        except (BrokenPipeError, ConnectionResetError, TimeoutError):
            self.close_connection = True
        except Exception:
            LOG.exception('Psychotest request failed')
            self._error(ApiError(500, 'INTERNAL_ERROR', 'API belum dapat memproses permintaan'))

    def do_POST(self):
        try:
            self.server.api.request_limiter.check()
            parsed = urlsplit(self.path)
            if parsed.query:
                raise ApiError(400, 'INVALID_PARAMETERS', 'Endpoint POST tidak menerima query')
            api = self.server.api
            path = parsed.path.rstrip('/') or '/'
            if path == '/psychotests/sessions':
                body = api.start(self.headers.get('Authorization'), self._body())
            else:
                match = SESSION_PATH.fullmatch(path)
                if not match:
                    raise ApiError(404, 'NOT_FOUND', 'Endpoint tidak ditemukan')
                session_id, action = match.groups()
                if action not in ('submit', 'retry-wrong'):
                    raise ApiError(405, 'METHOD_NOT_ALLOWED', 'Gunakan PUT untuk menyimpan jawaban')
                self._body()
                body = api.session(self.headers.get('Authorization'), session_id, action)
            self._send(200, body)
        except ApiError as exc:
            self._error(exc)
        except PsychotestError as exc:
            self._error(ApiError(exc.status, exc.code, exc.message, exc.details))
        except (BrokenPipeError, ConnectionResetError, TimeoutError):
            self.close_connection = True
        except Exception:
            LOG.exception('Psychotest request failed')
            self._error(ApiError(500, 'INTERNAL_ERROR', 'API belum dapat memproses permintaan'))

    def do_PUT(self):
        try:
            self.server.api.request_limiter.check()
            parsed = urlsplit(self.path)
            if parsed.query:
                raise ApiError(400, 'INVALID_PARAMETERS', 'Endpoint PUT tidak menerima query')
            match = SESSION_PATH.fullmatch(parsed.path.rstrip('/') or '/')
            if not match:
                raise ApiError(404, 'NOT_FOUND', 'Endpoint tidak ditemukan')
            session_id, action = match.groups()
            if action != 'answers':
                raise ApiError(405, 'METHOD_NOT_ALLOWED', 'Gunakan POST untuk submit')
            body = self.server.api.session(self.headers.get('Authorization'), session_id, action, self._body())
            self._send(200, body)
        except ApiError as exc:
            self._error(exc)
        except PsychotestError as exc:
            self._error(ApiError(exc.status, exc.code, exc.message, exc.details))
        except (BrokenPipeError, ConnectionResetError, TimeoutError):
            self.close_connection = True
        except Exception:
            LOG.exception('Psychotest request failed')
            self._error(ApiError(500, 'INTERNAL_ERROR', 'API belum dapat memproses permintaan'))

    def _method_not_allowed(self):
        self._send(405, {'ok': False, 'api_version': API_VERSION,
                         'error': {'code': 'METHOD_NOT_ALLOWED', 'message': 'Metode tidak didukung'}},
                   {'Allow': 'GET, POST, PUT'})

    do_DELETE = do_PATCH = do_OPTIONS = do_HEAD = _method_not_allowed


class LoopbackPsychotestServer(ThreadingHTTPServer):
    daemon_threads, allow_reuse_address = True, True

    def __init__(self, api=None, port=PORT):
        if port in (8787, 8788):
            raise ValueError('Port 8787/8788 milik layanan lain')
        self.api = api or PsychotestApi(PsychotestService())
        self.slots = threading.BoundedSemaphore(32)
        super().__init__((HOST, port), PsychotestHandler)

    def process_request(self, request, client_address):
        if not self.slots.acquire(blocking=False):
            raw = b'{"ok":false,"api_version":1,"error":{"code":"SERVER_BUSY","message":"Server sibuk"}}'
            try:
                request.settimeout(1)
                request.sendall(b'HTTP/1.1 503 Service Unavailable\r\nContent-Type: application/json\r\nConnection: close\r\nRetry-After: 2\r\nContent-Length: '
                                + str(len(raw)).encode() + b'\r\n\r\n' + raw)
            finally:
                self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except Exception:
            self.slots.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self.slots.release()

    def server_close(self):
        super().server_close()
        self.api.service.close()


def main():
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')
    service = PsychotestService()
    server = LoopbackPsychotestServer(PsychotestApi(service))
    LOG.info('AutoMate Psychotest V2 listening at http://%s:%s (loopback only)', HOST, PORT)
    try:
        server.serve_forever(poll_interval=0.25)
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
