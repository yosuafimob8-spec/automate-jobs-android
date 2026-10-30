"""Loopback-only Android HTTP facade over the existing Jobs V2 search engine.

Uses only the V2-local company catalogue and an optional read-only identity
lookup against the existing Android API. It never imports production database
configuration or stores tokens, and never changes port 8787.
Run explicitly: python -B http_api.py  (127.0.0.1:8788 only).
"""
from collections import Counter, OrderedDict, deque
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FutureTimeout
import copy
from dataclasses import dataclass
from datetime import datetime, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import logging
import math
import re
import threading
import time
from urllib.parse import parse_qs, urlsplit
from uuid import uuid4

from application_methods import METHOD_LABELS, WEB_APPLICATION_METHODS
from company_catalog import CompanyStore, DEFAULT_DB
from company_search import CATALOG_SOURCE_ID, HybridCompanySearch, PersonalView
from job_catalog import (ALL_LOCATIONS, ALL_POSITIONS, ALL_SOURCES, SOURCE_CATEGORIES,
                         choices, expand_categories, location_matches, source_choices)
from job_validation import valid_url
from live_sources import UTC, deduplicate_live, timestamp
from search_engine import get_search_catalog, search_jobs_with_status
from search_identity import IdentityError, android_subject

HOST, PORT, API_VERSION = '127.0.0.1', 8788, 2
MAX_RESULTS, MAX_PAGE_SIZE = 1000, 100
LOG = logging.getLogger('automate.jobs.v2.http')


class ApiError(Exception):
    def __init__(self, status, code, message, retry_after=None):
        super().__init__(message)
        self.status, self.code, self.message, self.retry_after = status, code, message, retry_after


def bad_request(message):
    raise ApiError(400, 'INVALID_PARAMETER', message)


@dataclass(frozen=True)
class Query:
    positions: tuple = ()
    locations: tuple = ()
    sources: tuple = ()
    categories: tuple = ()
    source_ids: tuple = ()
    since: str | None = None
    until: str | None = None
    max_age_days: int | None = 30

    def engine_options(self):
        return {name: list(getattr(self, name)) or None for name in
                ('positions', 'locations', 'sources', 'categories', 'source_ids')} | {
            'since': self.since, 'until': self.until, 'max_age_days': self.max_age_days}


@dataclass(frozen=True)
class RequestOptions:
    query: Query
    page: int
    page_size: int
    offset: int
    offset_mode: bool
    format: str
    snapshot_id: str | None


ALIASES = {
    'positions': ('positions', 'position', 'jabatan', 'q', 'query', 'keyword'),
    'locations': ('locations', 'location', 'lokasi', 'city'),
    'sources': ('sources', 'source', 'source_category', 'source_categories'),
    'categories': ('categories', 'category'),
    'source_ids': ('source_ids', 'source_id'),
    'since': ('since', 'date_from'), 'until': ('until', 'date_to'),
    'max_age_days': ('max_age_days', 'days'), 'page_size': ('page_size', 'limit'),
    'page': ('page',), 'offset': ('offset',), 'format': ('format', 'compat'),
    'snapshot_id': ('snapshot_id',),
}
ALLOWED_PARAMS = {name for names in ALIASES.values() for name in names}


def values(params, field):
    return [value for name in ALIASES[field] for value in params.get(name, [])]


def scalar(params, field, default=None):
    found = list(dict.fromkeys(v.strip() for v in values(params, field)))
    if len(found) > 1:
        bad_request(f'Parameter {field} memiliki nilai yang bertentangan')
    return found[0] if found else default


def selections(params, field, all_label=None):
    result = []
    for value in values(params, field):
        if len(value) > 2048:
            bad_request(f'Pilihan {field} terlalu panjang')
        if value.lstrip().startswith('['):
            try:
                parts = json.loads(value)
            except ValueError:
                bad_request(f'Array JSON {field} tidak valid')
            if not isinstance(parts, list) or any(not isinstance(v, str) for v in parts):
                bad_request(f'{field} harus array teks')
        else:
            parts = value.split(',')
        for part in parts:
            part = part.strip()
            if not part:
                continue
            if len(part) > 200 or any(ord(c) < 32 for c in part):
                bad_request(f'Pilihan {field} tidak valid')
            result.append(part)
    if len(result) > 30:
        bad_request(f'Maksimum 30 pilihan {field}')
    if any(v.casefold() in ('all', '*') for v in result):
        return ()
    if all_label:
        result = choices(result, all_label)
    return tuple(sorted(set(result), key=str.casefold))


def integer(value, field, minimum, maximum):
    if not isinstance(value, str) or not re.fullmatch(r'[0-9]{1,9}', value):
        bad_request(f'{field} harus bilangan bulat')
    number = int(value)
    if not minimum <= number <= maximum:
        bad_request(f'{field} harus antara {minimum} dan {maximum}')
    return number


def parse_request(query_string, catalog, allow_company_catalog=False):
    try:
        params = parse_qs(query_string, keep_blank_values=True, max_num_fields=80, errors='strict')
    except (ValueError, UnicodeError):
        bad_request('Query string tidak valid atau terlalu banyak parameter')
    unknown = set(params) - ALLOWED_PARAMS
    if unknown:
        bad_request('Parameter tidak dikenal: ' + ', '.join(sorted(unknown)))
    positions = selections(params, 'positions', ALL_POSITIONS)
    locations = selections(params, 'locations', ALL_LOCATIONS)
    sources = selections(params, 'sources', ALL_SOURCES)
    categories = selections(params, 'categories', ALL_POSITIONS)
    source_ids = selections(params, 'source_ids', ALL_SOURCES)
    try:
        location_matches('', locations)
        sources = tuple(sorted(set(source_choices(sources))))
        expand_categories(categories)
    except ValueError as exc:
        bad_request(str(exc))
    known_ids = {source['id'] for source in catalog['live_source_registry']}
    if allow_company_catalog:
        known_ids.add(CATALOG_SOURCE_ID)
    if set(source_ids) - known_ids:
        bad_request('ID sumber tidak dikenal: ' + ', '.join(sorted(set(source_ids) - known_ids)))
    since, until = scalar(params, 'since'), scalar(params, 'until')
    for name, value in [('since', since), ('until', until)]:
        if value is not None:
            try:
                parsed_date = timestamp(value) if len(value) <= 40 else None
            except (ValueError, OverflowError):
                parsed_date = None
            if parsed_date is None:
                bad_request(f'{name} harus berupa tanggal ISO 8601')
    age = scalar(params, 'max_age_days', None)
    age = None if age == 'all' or age is None and since is not None else integer(age or '30', 'max_age_days', 0, 3650)
    upper = timestamp(until)
    if until and re.fullmatch(r'\d{4}-\d{2}-\d{2}', until):
        upper = upper.replace(hour=23, minute=59, second=59, microsecond=999999)
        # Pass an explicit end-of-day timestamp to the existing engine, including
        # at the datetime upper bound; don't add a day past year 9999.
        until = upper.isoformat()
    lower = timestamp(since)
    if age is not None:
        lower = max(lower or datetime.min.replace(tzinfo=UTC), datetime.now(UTC) - timedelta(days=age))
    if lower and upper and lower > upper:
        bad_request('Rentang tanggal terbalik; tentukan since/date_from untuk tanggal historis')
    page = integer(scalar(params, 'page', '1'), 'page', 1, 100000)
    size = integer(scalar(params, 'page_size', '20'), 'page_size', 1, MAX_PAGE_SIZE)
    offset_value = scalar(params, 'offset')
    offset_mode = offset_value is not None
    if offset_mode and 'page' in params:
        bad_request('Gunakan page atau offset, bukan keduanya')
    offset = integer(offset_value, 'offset', 0, 10000000) if offset_mode else (page - 1) * size
    format_name = scalar(params, 'format', 'v2')
    if format_name in ('v1', 'legacy_array'):
        format_name = 'legacy'
    if format_name not in ('v2', 'legacy'):
        bad_request('format harus v2 atau legacy')
    snapshot_id = scalar(params, 'snapshot_id')
    if snapshot_id is not None and not re.fullmatch(r'[0-9a-f]{32}', snapshot_id):
        bad_request('snapshot_id tidak valid')
    return RequestOptions(Query(positions, locations, sources, categories, source_ids, since, until, age),
                          page, size, offset, offset_mode, format_name, snapshot_id)


class RateLimiter:
    def __init__(self, maximum=120, window=60, clock=time.monotonic):
        self.maximum, self.window, self.clock = maximum, window, clock
        self.events = deque()
        self.lock = threading.Lock()

    def check(self):
        with self.lock:
            now = self.clock()
            while self.events and self.events[0] <= now - self.window:
                self.events.popleft()
            if len(self.events) >= self.maximum:
                raise ApiError(429, 'RATE_LIMITED', 'Terlalu banyak permintaan; coba lagi setelah jeda',
                               max(1, math.ceil(self.events[0] + self.window - now)))
            self.events.append(now)


@dataclass
class Snapshot:
    id: str
    query: Query
    report: dict
    created_at: str
    expires_at: str
    expires_mono: float


def legacy_job(job):
    result = copy.deepcopy(job)
    first_action = next(iter(job.get('application_actions') or []), {})
    result.update(url=job['link'], job_url=job.get('job_url') or job['link'],
                  job_title=job.get('title'), company_name=job.get('company'),
                  job_location=job.get('location'), date_posted=job.get('published_at'),
                  apply_url=job.get('application_url'), action_label=first_action.get('label', 'Lihat Sumber Resmi'),
                  action_url=first_action.get('url', job['link']),
                  can_apply=job.get('application_method') in WEB_APPLICATION_METHODS | {'email_hr'})
    return result


class JobsApi:
    def __init__(self, engine=None, *, catalog=None, cache_ttl=300, cache_entries=16,
                 wait_timeout=190, request_limit=120, miss_limit=6, clock=time.monotonic,
                 company_store=None, identity_resolver=None):
        self.engine = engine or search_jobs_with_status
        self.catalog = catalog if catalog is not None else get_search_catalog()
        self.company_store = company_store
        self.identity_resolver = identity_resolver or android_subject
        if company_store is not None and not isinstance(self.engine, HybridCompanySearch):
            self.engine = HybridCompanySearch(self.engine, company_store,
                                              self.catalog['live_source_registry'])
        self.personal_views, self.personal_lock = {}, threading.Lock()
        self.cache_ttl, self.cache_entries, self.wait_timeout = cache_ttl, cache_entries, wait_timeout
        self.clock, self.started = clock, clock()
        self.request_limiter = RateLimiter(request_limit, clock=clock)
        self.miss_limiter = RateLimiter(miss_limit, clock=clock)
        self.cache, self.pending = OrderedDict(), {}
        self.lock = threading.RLock()
        self.executor = ThreadPoolExecutor(max_workers=1, thread_name_prefix='jobs-v2-search')

    def close(self):
        self.executor.shutdown(wait=False, cancel_futures=True)

    def _purge(self):
        live_ids = set()
        for key, snapshot in list(self.cache.items()):
            if snapshot.expires_mono <= self.clock():
                del self.cache[key]
            else:
                live_ids.add(snapshot.id)
        with self.personal_lock:
            for key in list(self.personal_views):
                if key[0] not in live_ids:
                    del self.personal_views[key]

    def _personal_view(self, snapshot, subject):
        key = (snapshot.id, subject)
        with self.personal_lock:
            view = self.personal_views.get(key)
            if view is None:
                view = self.personal_views[key] = PersonalView(snapshot, subject)
            return view

    def _load(self, query):
        try:
            report = self.engine(**query.engine_options(), limit=MAX_RESULTS,
                                 max_requests=100, max_seconds=180, max_details=40, requests_per_source=40)
            if not isinstance(report, dict) or not isinstance(report.get('jobs'), list):
                raise ValueError('Search engine returned an invalid response')
            report = copy.deepcopy(report)
            original = report['jobs']
            candidates = [j for j in original if isinstance(j, dict) and (
                valid_url(j.get('link')) or
                (j.get('result_type') == 'spontaneous' and j.get('candidate_id')))]
            live = deduplicate_live([j for j in candidates if j.get('result_type') != 'spontaneous'])
            references, seen_references = [], set()
            for job in candidates:
                if job.get('result_type') == 'spontaneous':
                    if job['candidate_id'] not in seen_references:
                        seen_references.add(job['candidate_id'])
                        references.append(job)
            jobs = live + references
            summary = report.setdefault('summary', {})
            summary['http_rejected_records'] = len(original) - len(jobs)
            report['jobs'] = jobs[:MAX_RESULTS]
            report.setdefault('source_status', [])
            report.setdefault('fallback_links', [])
            created = datetime.now(UTC)
            ttl = self.cache_ttl if summary.get('complete', True) else min(self.cache_ttl, 30)
            expires = [j.get('expiry_ts') for j in jobs if isinstance(j.get('expiry_ts'), (int, float))
                       and j['expiry_ts'] > created.timestamp()]
            if expires:
                ttl = min(ttl, max(1, min(expires) - created.timestamp()))
            snapshot = Snapshot(uuid4().hex, query, report, created.isoformat(),
                                (created + timedelta(seconds=ttl)).isoformat(), self.clock() + ttl)
            with self.lock:
                self.cache[query] = snapshot
                self.cache.move_to_end(query)
                while len(self.cache) > self.cache_entries:
                    self.cache.popitem(last=False)
            return snapshot
        finally:
            with self.lock:
                self.pending.pop(query, None)

    def snapshot(self, options):
        query = options.query
        with self.lock:
            self._purge()
            if options.snapshot_id:
                snapshot = next((s for s in self.cache.values() if s.id == options.snapshot_id), None)
                if snapshot is None:
                    raise ApiError(410, 'SNAPSHOT_EXPIRED', 'Snapshot kedaluwarsa/tidak tersedia; mulai lagi dari halaman pertama')
                if snapshot.query != query:
                    raise ApiError(409, 'SNAPSHOT_QUERY_MISMATCH', 'Filter harus sama dengan pencarian snapshot')
                self.cache.move_to_end(query)
                return snapshot, True
            cached = self.cache.get(query)
            if cached:
                self.cache.move_to_end(query)
                return cached, True
            future = self.pending.get(query)
            if future is None:
                if self.pending:
                    raise ApiError(503, 'SEARCH_BUSY', 'Pencarian lain masih berlangsung; coba lagi setelah jeda', 2)
                self.miss_limiter.check()
                future = self.executor.submit(self._load, query)
                self.pending[query] = future
        try:
            return future.result(timeout=self.wait_timeout), False
        except FutureTimeout:
            raise ApiError(504, 'SEARCH_TIMEOUT', 'Pencarian melewati batas waktu; coba lagi setelah jeda', 2) from None
        except Exception:
            # No provider URLs, credentials, or stack traces in public errors.
            raise ApiError(502, 'SEARCH_FAILED', 'Mesin pencarian belum dapat menyediakan respons yang valid') from None

    def jobs(self, options, authorization=None):
        snapshot, hit = self.snapshot(options)
        report = snapshot.report
        identity = None
        subject = None
        if authorization:
            try:
                subject, identity = self.identity_resolver(authorization)
            except IdentityError as exc:
                raise ApiError(exc.status, exc.code, exc.message, 2 if exc.status == 503 else None) from None
        if self.company_store is not None and subject and subject != 'anonymous':
            view = self._personal_view(snapshot, subject)
            jobs, available = view.page(self.company_store, options.offset, options.page_size)
            jobs = copy.deepcopy(jobs)
            total = len(available)
        else:
            total = len(report['jobs'])
            jobs = copy.deepcopy(report['jobs'][options.offset:options.offset + options.page_size])
        more = options.offset + len(jobs) < total
        summary = copy.deepcopy(report['summary'])
        statuses = copy.deepcopy(report['source_status'])
        failures = [s for s in statuses if s.get('status') in ('unavailable', 'partial', 'invalid_connection')]
        auth = [s for s in statuses if s.get('status') == 'authorization_required']
        empty_reason = None
        if not total:
            empty_reason = ('authorization_required' if auth and len(auth) == len(statuses)
                            else 'sources_unavailable' if failures
                            else 'no_live_connector' if statuses and all(s.get('status') == 'link_only' for s in statuses)
                            else 'no_matching_jobs')
        total_found = max(total, summary.get('real_jobs_found', total) - summary.get('http_rejected_records', 0))
        pagination = {'page': None if options.offset_mode else options.page,
            'page_size': options.page_size, 'offset': options.offset, 'total': total,
            'total_pages': math.ceil(total / options.page_size), 'has_more': more,
            'next_page': options.page + 1 if more and not options.offset_mode else None,
            'next_offset': options.offset + options.page_size if more else None}
        body = {'ok': True, 'api_version': API_VERSION, 'jobs': jobs, 'count': len(jobs), 'total': total,
            'total_found': total_found, 'pagination': pagination,
            'partial': bool(failures) or not summary.get('complete', True), 'empty_reason': empty_reason,
            'snapshot_truncated': total_found > total,
            'summary': summary, 'source_status': statuses,
            'fallback_links': copy.deepcopy(report['fallback_links']),
            'cache': {'hit': hit, 'snapshot_id': snapshot.id, 'created_at': snapshot.created_at,
                      'expires_at': snapshot.expires_at},
            'warnings': [{'code': 'SOURCE_PARTIAL_OR_UNAVAILABLE', 'source_id': s.get('source_id')} for s in failures]
                      + [{'code': 'PORTAL_AUTHORIZATION_REQUIRED', 'source_id': s.get('source_id')} for s in auth]}
        headers = {'X-Search-Snapshot': snapshot.id, 'X-Cache': 'HIT' if hit else 'MISS',
                   'X-Total-Count': str(total), 'X-Has-More': str(more).lower(),
                   'X-Next-Offset': str(pagination['next_offset']) if more else ''}
        if self.company_store is not None:
            body['personalization'] = {
                'enabled': bool(subject and subject != 'anonymous'),
                'identity': identity or {'kind': 'anonymous_shared', 'account_id': None},
                'limit_per_company': 2, 'application_sending_enabled': False}
            if body['personalization']['enabled']:
                headers['X-Personalized'] = '1'
        elif authorization:
            body['personalization'] = {
                'enabled': False, 'identity': identity,
                'limit_per_company': 2, 'application_sending_enabled': False,
                'reason': 'catalog_unavailable'}
        return ([legacy_job(j) for j in jobs] if options.format == 'legacy' else body), headers

    def health(self, port):
        registry = self.catalog['live_source_registry']
        return {'ok': True, 'api_version': API_VERSION, 'status': 'ok', 'service': 'automate-jobs-v2',
            'bind': {'host': HOST, 'port': port}, 'uptime_seconds': round(self.clock() - self.started, 3),
            'live_sources_configured': sum(s['adapter'] != 'link_only' for s in registry),
            'registered_sources': len(registry), 'portal_integrations': self.catalog['portal_integrations'],
            'searches_running': len(self.pending), 'upstream_network_check': False,
            'company_catalog': self.company_store.stats() if self.company_store is not None else None,
            'identity_bridge': 'existing_android_api_read_only'}

    def job_categories(self):
        categories = copy.deepcopy(self.catalog['job_categories'])
        return {'ok': True, 'api_version': API_VERSION, 'job_categories': categories,
                'role_categories': copy.deepcopy(self.catalog['role_categories']),
                'all_positions': ALL_POSITIONS,
                'counts': {'categories': len(categories), 'references': sum(map(len, categories.values()))}}

    def locations(self):
        return {'ok': True, 'api_version': API_VERSION, 'locations': copy.deepcopy(self.catalog['locations']),
                'all_locations': ALL_LOCATIONS, 'counts': {'regions': len(self.catalog['locations'])}}

    def job_sources(self):
        sources = []
        for source in self.catalog['live_source_registry']:
            portal = source.get('portal_adapter')
            sources.append({key: source[key] for key in ('id', 'name', 'category', 'adapter', 'official_url', 'search_url')}
                           | {'status': 'authorization_required' if portal else 'live_configured' if source['adapter'] != 'link_only' else 'link_only',
                              'supports_live_fetch': source['adapter'] != 'link_only',
                              'live_verified_this_request': False,
                              'reason': source.get('reason'), 'portal_adapter': portal})
        company_catalog = None
        if self.company_store is not None:
            company_catalog = {'source_id': CATALOG_SOURCE_ID, 'name': 'Katalog perusahaan lokal V2',
                'status': 'local_catalog', 'supports_live_fetch': False,
                'vacancy_verified': False, 'email_sending_enabled': False,
                **self.company_store.stats()}
        return {'ok': True, 'api_version': API_VERSION, 'source_categories': dict(SOURCE_CATEGORIES),
                'all_sources': ALL_SOURCES, 'sources': sources, 'company_catalog': company_catalog,
                'portal_integrations': copy.deepcopy(self.catalog['portal_integrations']),
                'application_methods': dict(METHOD_LABELS),
                'counts': {'registered': len(sources), 'live_configured': sum(s['supports_live_fetch'] for s in sources),
                           'authorization_required': sum(s['status'] == 'authorization_required' for s in sources)}}


class JobsHandler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'
    server_version, sys_version = 'AutoMateJobsV2/2', ''

    def setup(self):
        self.request.settimeout(10)
        super().setup()

    def log_message(self, format, *args):
        # Query strings and headers are deliberately excluded from access logs.
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
        headers = {'Retry-After': str(exc.retry_after)} if exc.retry_after else {}
        self._send(exc.status, {'ok': False, 'api_version': API_VERSION,
                              'error': {'code': exc.code, 'message': exc.message}}, headers)

    def send_error(self, code, message=None, explain=None):
        # Override stdlib HTML errors: the API always returns JSON error objects.
        self._error(ApiError(code, 'HTTP_ERROR', 'Permintaan HTTP tidak valid'))

    def do_GET(self):
        try:
            if len(self.path) > 8192:
                raise ApiError(414, 'URI_TOO_LONG', 'URL permintaan terlalu panjang')
            parsed = urlsplit(self.path)
            if parsed.scheme or parsed.netloc or parsed.fragment:
                bad_request('Target HTTP harus path lokal')
            self.server.api.request_limiter.check()
            path = parsed.path.rstrip('/') or '/'
            api = self.server.api
            if path == '/jobs':
                options = parse_request(parsed.query, api.catalog, api.company_store is not None)
                body, headers = api.jobs(options, self.headers.get('Authorization'))
            else:
                if parsed.query:
                    bad_request('Endpoint katalog/health tidak menerima query parameter')
                routes = {'/health': lambda: api.health(self.server.server_address[1]),
                          '/job-categories': api.job_categories, '/locations': api.locations,
                          '/job-sources': api.job_sources}
                if path not in routes:
                    raise ApiError(404, 'NOT_FOUND', 'Endpoint tidak ditemukan')
                body, headers = routes[path](), {}
            self._send(200, body, headers)
        except ApiError as exc:
            self._error(exc)
        except (BrokenPipeError, ConnectionResetError, TimeoutError):
            self.close_connection = True
        except Exception:
            self._error(ApiError(500, 'INTERNAL_ERROR', 'API belum dapat memproses permintaan'))

    def _method_not_allowed(self):
        self._send(405, {'ok': False, 'api_version': API_VERSION,
                        'error': {'code': 'METHOD_NOT_ALLOWED', 'message': 'Gunakan GET'}}, {'Allow': 'GET'})

    do_POST = do_PUT = do_PATCH = do_DELETE = do_OPTIONS = do_HEAD = _method_not_allowed


class LoopbackJobsServer(ThreadingHTTPServer):
    daemon_threads, allow_reuse_address = True, True

    def __init__(self, api=None, port=PORT):
        if port == 8787:
            raise ValueError('Port 8787 milik API lama dan tidak digunakan V2')
        self.api = api or JobsApi()
        self.slots = threading.BoundedSemaphore(16)
        super().__init__((HOST, port), JobsHandler)

    def process_request(self, request, client_address):
        if not self.slots.acquire(blocking=False):
            raw = b'{"ok":false,"api_version":2,"error":{"code":"SERVER_BUSY","message":"Server sibuk"}}'
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
        self.api.close()


def main():
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')
    store = None
    if DEFAULT_DB.exists():
        try:
            store = CompanyStore(DEFAULT_DB)
        except Exception:
            LOG.exception('Katalog perusahaan lokal tidak dapat dibuka; /jobs tetap berjalan tanpa katalog')
    server = LoopbackJobsServer(JobsApi(company_store=store))
    LOG.info('AutoMate Jobs V2 listening at http://%s:%s (loopback only)', HOST, PORT)
    try:
        server.serve_forever(poll_interval=0.25)
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
