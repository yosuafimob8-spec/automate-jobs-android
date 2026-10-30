"""Local psychotest persistence, server-authoritative exam engine, and scoring."""
from __future__ import annotations

from collections import OrderedDict, defaultdict
from contextlib import contextmanager
import hashlib
import json
import math
import os
from pathlib import Path
import random
import re
import secrets
import sqlite3
import time

from psychotest_bank import (CATEGORY_LABELS, FAMILY_LABELS, FEATURES, PERSONALITY_DIMENSIONS,
                             TESTS, TEST_ORDER, build_questions)
from psychotest_skb import SKB_FORMATIONS, SKB_SOURCE, SKB_VALIDATION

ROOT = Path(__file__).resolve().parent
TEMP = Path('/data/data/com.termux/files/usr/tmp/opencode')
DEFAULT_DB = ROOT / 'data' / 'psychotest_v2.sqlite3'
SESSION_ID = re.compile(r'^[0-9a-f]{32}$')
QUESTION_ID = re.compile(r'^[a-z0-9][a-z0-9-]{2,79}$')
API_VERSION = 1

SCHEMA = '''
CREATE TABLE IF NOT EXISTS meta(key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS questions(
 id TEXT PRIMARY KEY, family TEXT NOT NULL, category TEXT NOT NULL, subcategory TEXT,
 difficulty TEXT NOT NULL, kind TEXT NOT NULL, prompt TEXT NOT NULL,
 options_json TEXT NOT NULL, answer_key_json TEXT NOT NULL, explanation TEXT NOT NULL,
 rubric_json TEXT, reference_json TEXT, active INTEGER NOT NULL DEFAULT 1);
CREATE INDEX IF NOT EXISTS idx_questions_pool ON questions(family,category,active);
CREATE TABLE IF NOT EXISTS sessions(
 id TEXT PRIMARY KEY, subject TEXT NOT NULL, account_id INTEGER, test_id TEXT NOT NULL,
 family TEXT NOT NULL, kind TEXT NOT NULL, title TEXT NOT NULL,
 started_at INTEGER NOT NULL, deadline_at INTEGER NOT NULL, duration_seconds INTEGER NOT NULL,
 status TEXT NOT NULL, submitted_at INTEGER, auto_submitted INTEGER NOT NULL DEFAULT 0,
 questions_json TEXT NOT NULL, answers_json TEXT NOT NULL, retry_of TEXT,
 revision INTEGER NOT NULL DEFAULT 0, last_activity_at INTEGER NOT NULL,
 score REAL, max_score REAL, percent REAL, correct_count INTEGER, wrong_count INTEGER,
 blank_count INTEGER, duration_used INTEGER, result_json TEXT);
CREATE INDEX IF NOT EXISTS idx_sessions_subject ON sessions(subject,status,submitted_at);
CREATE INDEX IF NOT EXISTS idx_sessions_board ON sessions(test_id,status,score);
'''


class PsychotestError(Exception):
    def __init__(self, status, code, message, details=None):
        super().__init__(message)
        self.status, self.code, self.message, self.details = status, code, message, details


def _json(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'), allow_nan=False)


def _loads(value, default=None):
    if value is None:
        return default
    return json.loads(value)


def validate_questions(questions):
    if not isinstance(questions, list) or not questions:
        raise ValueError('Question bank must be a non-empty list')
    seen = set()
    for question in questions:
        if not isinstance(question, dict):
            raise ValueError('Question must be an object')
        qid = question.get('id')
        if not isinstance(qid, str) or not QUESTION_ID.fullmatch(qid) or qid in seen:
            raise ValueError(f'Invalid or duplicate question id: {qid!r}')
        seen.add(qid)
        if question.get('kind') not in ('objective', 'tkp', 'personality'):
            raise ValueError(f'{qid}: invalid kind')
        if question.get('family') not in FAMILY_LABELS or question.get('category') not in CATEGORY_LABELS:
            raise ValueError(f'{qid}: invalid family/category')
        if question.get('difficulty') not in ('mudah', 'sedang', 'sulit'):
            raise ValueError(f'{qid}: invalid difficulty')
        if not isinstance(question.get('prompt'), str) or len(question['prompt'].strip()) < 10:
            raise ValueError(f'{qid}: prompt is too short')
        if not isinstance(question.get('explanation'), str) or len(question['explanation'].strip()) < 10:
            raise ValueError(f'{qid}: explanation is too short')
        reference = question.get('reference')
        if question.get('family') == 'skb':
            if not isinstance(reference, dict) or not reference.get('source') or not reference.get('year'):
                raise ValueError(f'{qid}: SKB question requires source and year reference')
        if reference is not None:
            if not isinstance(reference, dict):
                raise ValueError(f'{qid}: reference must be an object')
            if not isinstance(reference.get('source'), str) or not reference['source'].strip():
                raise ValueError(f'{qid}: reference source is missing')
            if type(reference.get('year')) is not int or reference['year'] < 2000:
                raise ValueError(f'{qid}: reference year is invalid')
        options = question.get('options')
        if not isinstance(options, list) or len(options) < 4 or len(options) > 5:
            raise ValueError(f'{qid}: options must contain 4-5 entries')
        ids = [o.get('id') for o in options if isinstance(o, dict)]
        texts = [o.get('text') for o in options if isinstance(o, dict)]
        if len(ids) != len(options) or len(set(ids)) != len(options) or len(set(texts)) != len(options):
            raise ValueError(f'{qid}: option ids/text must be unique')
        if any(not isinstance(t, str) or not t.strip() for t in texts):
            raise ValueError(f'{qid}: empty option text')
        key = question.get('answer_key')
        if not isinstance(key, dict):
            raise ValueError(f'{qid}: missing answer key')
        if question['kind'] == 'objective':
            if key.get('correct') not in ids:
                raise ValueError(f'{qid}: correct option is not present')
        elif question['kind'] == 'tkp':
            scores = key.get('scores')
            if not isinstance(scores, dict) or set(scores) != set(ids) or set(scores.values()) != {1, 2, 3, 4, 5}:
                raise ValueError(f'{qid}: TKP must score every option 1-5 exactly once')
            rubric = question.get('rubric')
            if not isinstance(rubric, dict) or set(rubric) != {'1', '2', '3', '4', '5'}:
                raise ValueError(f'{qid}: TKP rubric is incomplete')
        else:
            if key.get('dimension') not in PERSONALITY_DIMENSIONS or not isinstance(key.get('reverse'), bool):
                raise ValueError(f'{qid}: invalid personality dimension')
            if key.get('scale') != [1, 2, 3, 4, 5]:
                raise ValueError(f'{qid}: invalid personality scale')
    return len(seen)


class PsychotestStore:
    def __init__(self, path=DEFAULT_DB):
        self.path = Path(path).resolve()
        if self.path.name == 'automate.db' or not (self.path.is_relative_to(ROOT) or self.path.is_relative_to(TEMP)):
            raise ValueError('Psychotest storage must be V2-local or an approved disposable test path')
        self.path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
        new = not self.path.exists()
        with self.connect() as db:
            db.executescript(SCHEMA)
            columns = {row['name'] for row in db.execute('PRAGMA table_info(questions)')}
            if 'reference_json' not in columns:
                db.execute('ALTER TABLE questions ADD COLUMN reference_json TEXT')
            session_columns = {row['name'] for row in db.execute('PRAGMA table_info(sessions)')}
            if 'retry_of' not in session_columns:
                db.execute('ALTER TABLE sessions ADD COLUMN retry_of TEXT')
        if new:
            os.chmod(self.path, 0o600)

    @contextmanager
    def connect(self):
        db = sqlite3.connect(self.path, timeout=10)
        db.row_factory = sqlite3.Row
        db.execute('PRAGMA foreign_keys=ON')
        db.execute('PRAGMA busy_timeout=10000')
        try:
            yield db
            db.commit()
        except Exception:
            db.rollback()
            raise
        finally:
            db.close()

    def seed(self, questions):
        validate_questions(questions)
        fields = ('id', 'family', 'category', 'subcategory', 'difficulty', 'kind', 'prompt',
                  'options', 'answer_key', 'explanation', 'rubric', 'reference')
        digest = hashlib.sha256(_json([
            {key: q.get(key) for key in fields} for q in questions
        ]).encode()).hexdigest()
        with self.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            db.execute('UPDATE questions SET active=0')
            for q in questions:
                db.execute('''INSERT INTO questions(id,family,category,subcategory,difficulty,kind,prompt,
                    options_json,answer_key_json,explanation,rubric_json,reference_json,active)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,1)
                    ON CONFLICT(id) DO UPDATE SET family=excluded.family,category=excluded.category,
                    subcategory=excluded.subcategory,difficulty=excluded.difficulty,kind=excluded.kind,
                    prompt=excluded.prompt,options_json=excluded.options_json,answer_key_json=excluded.answer_key_json,
                    explanation=excluded.explanation,rubric_json=excluded.rubric_json,
                    reference_json=excluded.reference_json,active=1''',
                    (q['id'], q['family'], q['category'], q.get('subcategory'), q['difficulty'], q['kind'],
                     q['prompt'], _json(q['options']), _json(q['answer_key']), q['explanation'],
                     _json(q.get('rubric')) if q.get('rubric') is not None else None,
                     _json(q.get('reference')) if q.get('reference') is not None else None))
            db.execute("INSERT INTO meta(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value",
                       ('bank_version', digest))
        return digest

    def bank_version(self):
        with self.connect() as db:
            row = db.execute("SELECT value FROM meta WHERE key=?", ('bank_version',)).fetchone()
        return row[0] if row else None

    def counts(self):
        with self.connect() as db:
            rows = db.execute('SELECT family,category,COUNT(*) AS n FROM questions WHERE active=1 GROUP BY family,category').fetchall()
        return {(r['family'], r['category']): r['n'] for r in rows}

    def active_questions(self, family, category):
        with self.connect() as db:
            return [dict(r) for r in db.execute(
                'SELECT * FROM questions WHERE active=1 AND family=? AND category=? ORDER BY id', (family, category))]

    def questions_by_ids(self, ids):
        if not ids:
            return {}
        placeholders = ','.join('?' for _ in ids)
        with self.connect() as db:
            rows = db.execute('SELECT * FROM questions WHERE id IN (' + placeholders + ')', tuple(ids)).fetchall()
        return {r['id']: dict(r) for r in rows}

    def session_row(self, session_id, subject=None, db=None):
        if not isinstance(session_id, str) or not SESSION_ID.fullmatch(session_id):
            raise PsychotestError(400, 'INVALID_SESSION_ID', 'ID sesi tidak valid')
        if db is None:
            with self.connect() as own:
                row = own.execute('SELECT * FROM sessions WHERE id=?', (session_id,)).fetchone()
        else:
            row = db.execute('SELECT * FROM sessions WHERE id=?', (session_id,)).fetchone()
        if row is None or (subject is not None and row['subject'] != subject):
            raise PsychotestError(404, 'SESSION_NOT_FOUND', 'Sesi tidak ditemukan')
        return row


class PsychotestService:
    def __init__(self, store=None, *, seed=True, clock=time.time, rng=None):
        self.store = store or PsychotestStore(DEFAULT_DB)
        if seed:
            self.store.seed(build_questions())
        self.clock = clock
        self.rng = rng or random.SystemRandom()
        self.started = clock()

    def close(self):
        pass

    @staticmethod
    def alias(subject):
        return 'Peserta-' + hashlib.sha256(subject.encode()).hexdigest()[:6].upper()

    def _now(self):
        return int(self.clock())

    def _availability(self, test_id):
        cfg = TESTS[test_id]
        counts = self.store.counts()
        missing = {}
        for category, needed in cfg['categories'].items():
            have = counts.get((cfg['family'], category), 0)
            if have < needed:
                missing[category] = {'needed': needed, 'available': have, 'missing': needed - have}
        return missing

    def catalog(self):
        counts = self.store.counts()
        modes = []
        for test_id in TEST_ORDER:
            cfg = TESTS[test_id]
            missing = self._availability(test_id)
            mode_categories = {}
            for category, needed in cfg['categories'].items():
                mode_categories[category] = {'label': CATEGORY_LABELS[category],
                                             'questions': needed,
                                             'bank': counts.get((cfg['family'], category), 0)}
            mode = {'test_id': test_id, 'title': cfg['title'], 'family': cfg['family'],
                    'family_label': FAMILY_LABELS[cfg['family']], 'kind': cfg['kind'],
                    'duration_seconds': cfg['duration'], 'question_count': sum(cfg['categories'].values()),
                    'categories': mode_categories, 'available': not missing, 'missing': missing,
                    'score_scale': cfg['score_scale'], 'leaderboard': cfg['leaderboard'],
                    'official_reference': cfg.get('official_reference')}
            for key in ('formation', 'formation_label', 'mode_group', 'simulation', 'simulation_label',
                        'official_format', 'source'):
                if cfg.get(key) is not None:
                    mode[key] = cfg[key]
            modes.append(mode)
        categories = []
        for family in FAMILY_LABELS:
            for category, label in CATEGORY_LABELS.items():
                n = counts.get((family, category), 0)
                if n:
                    categories.append({'family': family, 'family_label': FAMILY_LABELS[family],
                                       'category': category, 'label': label, 'questions': n})
        return {'ok': True, 'api_version': API_VERSION, 'service': 'automate-jobs-v2-psychotest',
                'features': [dict(x) for x in FEATURES], 'families': dict(FAMILY_LABELS),
                'categories': categories, 'modes': modes, 'bank_version': self.store.bank_version(),
                'skb': {'source': SKB_SOURCE, 'verification': SKB_VALIDATION,
                        'formations': SKB_FORMATIONS,
                        'mode_note': 'Mode SKB memakai label Simulasi Latihan karena jumlah soal, durasi, dan penilaian resmi per formasi tidak tercantum pada sumber materi 2024.'},
                'auth': {'required_for_exam': True, 'bridge': 'existing /auth/me read-only',
                         'identity_header': 'Authorization: Bearer <sesi Android>'},
                'timer': {'source': 'server', 'answer_batch_max': 110, 'history_limit_max': 100},
                'privacy': {'leaderboard_alias': 'anonymous hash', 'email_exposed': False},
                'email_sending_enabled': False}

    def _public_session(self, row):
        cfg = TESTS[row['test_id']]
        now = self._now()
        status = row['status']
        summary = {'id': row['id'], 'test_id': row['test_id'], 'title': row['title'],
                   'family': row['family'], 'family_label': FAMILY_LABELS[row['family']],
                   'kind': row['kind'], 'status': status, 'started_at': row['started_at'],
                   'deadline_at': row['deadline_at'], 'duration_seconds': row['duration_seconds'],
                   'submitted_at': row['submitted_at'], 'auto_submitted': bool(row['auto_submitted']),
                   'revision': row['revision'], 'question_count': len(_loads(row['questions_json'], [])),
                   'answered_count': len(_loads(row['answers_json'], {})),
                   'remaining_seconds': max(0, row['deadline_at'] - now) if status == 'active' else 0,
                   'score': row['score'], 'max_score': row['max_score'], 'percent': row['percent'],
                   'correct_count': row['correct_count'], 'wrong_count': row['wrong_count'],
                   'blank_count': row['blank_count'], 'duration_used': row['duration_used'],
                   'retry_of': row['retry_of']}
        for key in ('formation', 'formation_label', 'mode_group', 'simulation', 'simulation_label',
                    'official_format', 'source'):
            if cfg.get(key) is not None:
                summary[key] = cfg[key]
        if status == 'submitted':
            result = _loads(row['result_json'], {})
            summary['result_label'] = result.get('result_label')
            threshold = result.get('threshold')
            summary['threshold_met'] = threshold.get('met') if threshold else None
        else:
            summary['result_label'] = None
            summary['threshold_met'] = None
        summary['category'] = row['title']
        return summary

    def start_session(self, test_id, subject, account_id=None):
        return self._create_session(test_id, subject, account_id)

    def _create_session(self, test_id, subject, account_id=None, *, question_ids=None,
                        source_entries=None, duration=None, title=None, retry_of=None):
        if test_id not in TESTS:
            raise PsychotestError(404, 'TEST_NOT_FOUND', 'Tes tidak ditemukan')
        cfg = TESTS[test_id]
        if question_ids is None:
            missing = self._availability(test_id)
            if missing:
                raise PsychotestError(409, 'TEST_UNAVAILABLE', 'Bank soal belum mencukupi untuk mode ini',
                                      {'test_id': test_id, 'missing': missing})
            selected = []
            for category, needed in cfg['categories'].items():
                pool = self.store.active_questions(cfg['family'], category)
                if len(pool) < needed:
                    raise PsychotestError(409, 'TEST_UNAVAILABLE', 'Bank soal berubah dan tidak lagi mencukupi')
                selected.extend(self.rng.sample(pool, needed))
            self.rng.shuffle(selected)
            source_by_id = {}
        else:
            if not isinstance(question_ids, list) or not question_ids or len(set(question_ids)) != len(question_ids):
                raise PsychotestError(400, 'INVALID_QUESTION_SET', 'Daftar soal ulang tidak valid')
            qmap = self.store.questions_by_ids(question_ids)
            if len(qmap) != len(question_ids):
                raise PsychotestError(409, 'BANK_INCOMPLETE', 'Sebagian soal ulang tidak tersedia')
            selected = [qmap[qid] for qid in question_ids]
            source_by_id = {e['question_id']: e for e in (source_entries or [])}
        entries = []
        for row in selected:
            source = source_by_id.get(row['id'])
            if source and isinstance(source.get('option_ids'), list):
                option_ids = list(source['option_ids'])
            else:
                option_ids = [o['id'] for o in _loads(row['options_json'], [])]
                if row['kind'] != 'personality':
                    self.rng.shuffle(option_ids)
            entries.append({'question_id': row['id'], 'option_ids': option_ids})
        now = self._now()
        session_id = secrets.token_hex(16)
        final_duration = duration if type(duration) is int and duration > 0 else cfg['duration']
        final_title = title if isinstance(title, str) and title.strip() else cfg['title']
        deadline = now + final_duration
        with self.store.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            db.execute('''INSERT INTO sessions(id,subject,account_id,test_id,family,kind,title,started_at,
                deadline_at,duration_seconds,status,questions_json,answers_json,retry_of,revision,last_activity_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,?)''',
                (session_id, subject, account_id, test_id, cfg['family'], cfg['kind'], final_title,
                 now, deadline, final_duration, 'active', _json(entries), '{}', retry_of, now))
            row = db.execute('SELECT * FROM sessions WHERE id=?', (session_id,)).fetchone()
        return {'ok': True, 'api_version': API_VERSION, 'session': self._public_session(row),
                'questions_url': f'/psychotests/sessions/{session_id}/questions'}

    def retry_wrong(self, session_id, subject):
        row = self._owned(session_id, subject)
        if row['status'] != 'submitted':
            raise PsychotestError(409, 'SESSION_ACTIVE', 'Selesaikan ujian sebelum mengulang soal yang salah')
        cfg = TESTS[row['test_id']]
        if cfg['family'] != 'skb':
            raise PsychotestError(400, 'RETRY_NOT_SUPPORTED', 'Latihan ulang soal salah tersedia untuk SKB')
        result = _loads(row['result_json'], {})
        wrong_ids = [item['question_id'] for item in result.get('questions', [])
                     if item.get('kind', 'objective') == 'objective'
                     and (item.get('selected_option_id') is None
                          or item.get('selected_option_id') != item.get('correct_option'))]
        if not wrong_ids:
            raise PsychotestError(409, 'NO_WRONG_ANSWERS', 'Tidak ada soal salah atau kosong untuk diulang')
        source_entries = _loads(row['questions_json'], [])
        by_id = {entry['question_id']: entry for entry in source_entries}
        ordered = [by_id[qid] for qid in wrong_ids if qid in by_id]
        created = self._create_session(row['test_id'], subject, row['account_id'],
                                       question_ids=[entry['question_id'] for entry in ordered],
                                       source_entries=ordered, duration=600,
                                       title='Latihan Ulang: ' + row['title'], retry_of=session_id)
        created['retry_of'] = session_id
        return created

    def _owned(self, session_id, subject):
        return self.store.session_row(session_id, subject)

    def _ensure_not_expired(self, row, subject):
        if row['status'] == 'active' and self._now() >= row['deadline_at']:
            return self._finalize(row['id'], subject, self._now(), auto=True)
        return row

    def get_session(self, session_id, subject):
        row = self._ensure_not_expired(self._owned(session_id, subject), subject)
        return {'ok': True, 'api_version': API_VERSION, 'session': self._public_session(row)}

    def active_sessions(self, subject):
        self._expire_due(subject)
        with self.store.connect() as db:
            rows = db.execute("SELECT * FROM sessions WHERE subject=? AND status='active' ORDER BY started_at DESC LIMIT 20",
                              (subject,)).fetchall()
        return {'ok': True, 'api_version': API_VERSION, 'sessions': [self._public_session(r) for r in rows]}

    def questions(self, session_id, subject):
        row = self._ensure_not_expired(self._owned(session_id, subject), subject)
        if row['status'] == 'submitted':
            raise PsychotestError(409, 'SESSION_FINISHED', 'Ujian sudah selesai; buka hasil',
                                  {'result_url': f'/psychotests/sessions/{row["id"]}/result'})
        entries = _loads(row['questions_json'], [])
        qmap = self.store.questions_by_ids([e['question_id'] for e in entries])
        if len(qmap) != len(entries):
            raise PsychotestError(500, 'BANK_INCOMPLETE', 'Bank soal untuk sesi ini tidak lengkap')
        answers = _loads(row['answers_json'], {})
        questions = []
        for position, entry in enumerate(entries, 1):
            q = qmap[entry['question_id']]
            options = {o['id']: o['text'] for o in _loads(q['options_json'], [])}
            questions.append({'position': position, 'question_id': q['id'], 'category': q['category'],
                              'category_label': CATEGORY_LABELS[q['category']], 'subcategory': q['subcategory'],
                              'difficulty': q['difficulty'], 'prompt': q['prompt'],
                              'options': [{'id': oid, 'text': options[oid]} for oid in entry['option_ids'] if oid in options],
                              'selected_option_id': answers.get(q['id'])})
        return {'ok': True, 'api_version': API_VERSION, 'session': self._public_session(row),
                'questions': questions}

    def _score(self, row, now, auto):
        cfg = TESTS[row['test_id']]
        entries = _loads(row['questions_json'], [])
        answers = _loads(row['answers_json'], {})
        qmap = self.store.questions_by_ids([e['question_id'] for e in entries])
        if len(qmap) != len(entries):
            raise PsychotestError(500, 'BANK_INCOMPLETE', 'Bank soal untuk sesi ini tidak lengkap')
        for q in qmap.values():
            q['options'] = _loads(q['options_json'], [])
            q['answer_key'] = _loads(q['answer_key_json'], {})
            q['rubric'] = _loads(q['rubric_json'], {}) or {}
            q['reference'] = _loads(q.get('reference_json'))
        stats = OrderedDict()
        for category in cfg['categories']:
            sample_kind = next((qmap[e['question_id']]['kind'] for e in entries
                                if qmap[e['question_id']]['category'] == category), 'objective')
            stats[category] = {'category': category, 'label': CATEGORY_LABELS[category], 'total': 0,
                               'answered': 0, 'blank': 0,
                               'correct': 0 if sample_kind == 'objective' else None,
                               'wrong': 0 if sample_kind == 'objective' else None,
                               'earned': 0, 'max': 0, 'kind': sample_kind}
        correct_count = wrong_count = blank_count = 0
        earned_total = max_total = 0
        personality_sums = defaultdict(int)
        personality_counts = defaultdict(int)
        result_questions = []
        for position, entry in enumerate(entries, 1):
            q = qmap[entry['question_id']]
            options = {o['id']: o for o in q['options']}
            selected = answers.get(q['id'])
            cs = stats[q['category']]
            cs['total'] += 1
            if q['kind'] == 'objective':
                unit = 1 if cfg['family'] in ('private', 'skb') else 5
                cs['max'] += unit
                max_total += unit
                if selected is None or selected not in options:
                    cs['blank'] += 1
                    blank_count += 1
                elif selected == q['answer_key'].get('correct'):
                    cs['correct'] += 1
                    cs['earned'] += unit
                    correct_count += 1
                    earned_total += unit
                else:
                    cs['wrong'] += 1
                    wrong_count += 1
            elif q['kind'] == 'tkp':
                cs['max'] += 5
                max_total += 5
                if selected is None or selected not in options:
                    cs['blank'] += 1
                    blank_count += 1
                else:
                    cs['answered'] += 1
                    score = q['answer_key']['scores'].get(selected, 0)
                    cs['earned'] += score
                    earned_total += score
            else:
                if selected is None or selected not in options:
                    cs['blank'] += 1
                    blank_count += 1
                else:
                    cs['answered'] += 1
                    value = q['answer_key']['scale'][list(options).index(selected)]
                    if q['answer_key']['reverse']:
                        value = 6 - value
                    dimension = q['answer_key']['dimension']
                    personality_sums[dimension] += value
                    personality_counts[dimension] += 1
            option_rows = []
            for oid in entry['option_ids']:
                if oid not in options:
                    continue
                item = {'id': oid, 'text': options[oid]['text'], 'selected': oid == selected}
                if q['kind'] == 'tkp':
                    value = q['answer_key']['scores'][oid]
                    item['score'] = value
                    item['rationale'] = q['rubric'][str(value)]
                elif q['kind'] == 'personality':
                    item['value'] = q['answer_key']['scale'][list(options).index(oid)]
                else:
                    item['is_correct'] = oid == q['answer_key'].get('correct')
                option_rows.append(item)
            result_questions.append({
                'position': position, 'question_id': q['id'], 'category': q['category'],
                'category_label': CATEGORY_LABELS[q['category']], 'subcategory': q['subcategory'],
                'difficulty': q['difficulty'], 'kind': q['kind'], 'prompt': q['prompt'],
                'options': option_rows,
                'selected_option_id': selected if selected in options else None,
                'correct_option': q['answer_key'].get('correct') if q['kind'] == 'objective' else None,
                'explanation': q['explanation'], 'reference': q.get('reference')})
        categories = []
        for cs in stats.values():
            if not cs['total']:
                continue
            cs['percent'] = round(cs['earned'] / cs['max'] * 100, 2) if cs['max'] else None
            categories.append(cs)
        if cfg['kind'] == 'personality':
            dimensions = []
            for code, meta in PERSONALITY_DIMENSIONS.items():
                count = personality_counts.get(code, 0)
                raw = personality_sums.get(code, 0)
                pct = round((raw - count) / (count * 4) * 100, 2) if count else None
                level = 'rendah' if pct is None or pct < 40 else 'sedang' if pct < 70 else 'tinggi'
                description = meta[{'rendah': 'low', 'sedang': 'medium', 'tinggi': 'high'}[level]]
                dimensions.append({'dimension': code, 'label': meta['label'], 'answered': count,
                                   'raw_score': raw if count else None, 'percent': pct, 'level': level,
                                   'description': description})
            ranked = [d for d in dimensions if d['percent'] is not None]
            ranked.sort(key=lambda d: (-d['percent'], d['dimension']))
            personality = {'interpretation_only': True, 'dimensions': dimensions,
                           'pattern': [d['label'] for d in ranked[:2]],
                           'disclaimer': 'Interpretasi kecenderungan, bukan diagnosis psikologis, bukan penilaian benar/salah, dan tidak menggantikan asesmen profesional.'}
            score = max_score = percent = None
            label = 'Interpretasi kepribadian'
        else:
            personality = None
            score = round(earned_total, 2)
            max_score = round(max_total, 2)
            percent = round(earned_total / max_total * 100, 2) if max_total else 0
            label = 'Selesai'
        threshold = None
        if cfg.get('official_reference') and cfg['kind'] == 'mixed_cat':
            ref = cfg['official_reference']
            parts = {}
            met = True
            for category, minimum in ref['thresholds'].items():
                value = stats[category]['earned']
                ok = value >= minimum
                met = met and ok
                parts[category] = {'score': value, 'minimum': minimum, 'max': ref['max_scores'][category], 'met': ok}
            threshold = {'reference_year': ref['year'], 'regulation': ref['regulation'],
                         'source_url': ref['source_url'], 'categories': parts, 'met': met,
                         'note': ref['note']}
            label = 'Ambang batas terpenuhi' if met else 'Belum memenuhi ambang batas'
        analysis = None
        if cfg['family'] == 'skb':
            ranked = [cat for cat in categories if cat['percent'] is not None]
            ranked.sort(key=lambda item: (item['percent'], item['category']))
            weak = [cat for cat in ranked if cat['percent'] < 70]
            if not weak and ranked and ranked[0]['percent'] < 100:
                weak = ranked[:1]
            weak_items = [{'category': item['category'], 'label': item['label'],
                           'percent': item['percent'],
                           'recommendation': f"Tinjau kembali materi {item['label']} dan kerjakan latihan ulang soal yang salah."}
                          for item in weak]
            review_ids = [item['question_id'] for item in result_questions
                          if item['kind'] == 'objective'
                          and (item['selected_option_id'] is None
                               or item['selected_option_id'] != item['correct_option'])]
            analysis = {
                'weakest_competencies': weak_items,
                'review_question_ids': review_ids,
                'review_question_count': len(review_ids),
                'retry_wrong': {
                    'method': 'POST',
                    'url': f'/psychotests/sessions/{row["id"]}/retry-wrong',
                    'question_count': len(review_ids),
                } if review_ids else None,
                'note': 'Analisis dihitung dari capaian per materi. Latihan ulang memuat soal yang salah atau kosong pada sesi ini.',
            }
        if cfg['family'] == 'private':
            scoring = {'method': 'persentase', 'correct_points': 1, 'wrong_points': 0, 'blank_points': 0,
                       'max_score': max_score}
        elif cfg['family'] == 'skb':
            scoring = {'method': 'persentase', 'correct_points': 1, 'wrong_points': 0, 'blank_points': 0,
                       'max_score': max_score, 'official_format': False,
                       'note': 'Penilaian latihan internal; bukan format resmi SKB.'}
        else:
            scoring = {'method': 'CAT', 'twk_tiu_correct_points': 5, 'twk_tiu_wrong_points': 0,
                       'tkp_min': 1, 'tkp_max': 5, 'blank_points': 0, 'max_score': max_score}
        duration_used = max(0, min(now, row['deadline_at']) - row['started_at'])
        result = {'family': cfg['family'], 'test_id': row['test_id'], 'title': row['title'],
                  'score': score, 'max_score': max_score, 'percent': percent,
                  'correct_count': None if cfg['kind'] == 'personality' else correct_count,
                  'wrong_count': None if cfg['kind'] == 'personality' else wrong_count,
                  'blank_count': blank_count, 'answered_count': len(entries) - blank_count,
                  'duration_used': duration_used, 'auto_submitted': auto,
                  'result_label': label, 'categories': categories, 'threshold': threshold,
                  'scoring': scoring, 'personality': personality, 'analysis': analysis,
                  'questions': result_questions}
        for key in ('formation', 'formation_label', 'mode_group', 'simulation', 'simulation_label',
                    'official_format', 'source'):
            if cfg.get(key) is not None:
                result[key] = cfg[key]
        return result

    def _finalize_row(self, db, row, now, reject_submitted=False):
        if row['status'] == 'submitted':
            if reject_submitted:
                raise PsychotestError(409, 'ALREADY_SUBMITTED', 'Ujian ini sudah dikirim',
                                      {'result_url': f'/psychotests/sessions/{row["id"]}/result'})
            return row
        auto = now >= row['deadline_at']
        result = self._score(row, now, auto)
        duration_used = max(0, min(now, row['deadline_at']) - row['started_at'])
        db.execute('''UPDATE sessions SET status='submitted',submitted_at=?,auto_submitted=?,score=?,max_score=?,
            percent=?,correct_count=?,wrong_count=?,blank_count=?,duration_used=?,result_json=?,last_activity_at=?
            WHERE id=? AND status='active' ''',
            (now, int(auto), result['score'], result['max_score'], result['percent'],
             result['correct_count'], result['wrong_count'], result['blank_count'], duration_used,
             _json(result), now, row['id']))
        return db.execute('SELECT * FROM sessions WHERE id=?', (row['id'],)).fetchone()

    def _finalize(self, session_id, subject, now, auto=True, reject_submitted=False):
        with self.store.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            row = self.store.session_row(session_id, subject, db)
            return self._finalize_row(db, row, now, reject_submitted=reject_submitted)

    def save_answers(self, session_id, subject, answers, expected_revision=None):
        row = self._owned(session_id, subject)
        if row['status'] == 'submitted':
            raise PsychotestError(409, 'ALREADY_SUBMITTED', 'Ujian ini sudah dikirim',
                                  {'result_url': f'/psychotests/sessions/{row["id"]}/result'})
        if not isinstance(answers, list) or not 1 <= len(answers) <= 110:
            raise PsychotestError(400, 'INVALID_ANSWERS', 'answers harus berisi 1-110 jawaban')
        entries = _loads(row['questions_json'], [])
        allowed = {e['question_id']: set(e['option_ids']) for e in entries}
        clean, seen = {}, set()
        for item in answers:
            if not isinstance(item, dict):
                raise PsychotestError(400, 'INVALID_ANSWERS', 'Setiap jawaban harus berupa objek')
            qid, oid = item.get('question_id'), item.get('option_id')
            if not isinstance(qid, str) or qid not in allowed:
                raise PsychotestError(400, 'INVALID_ANSWERS', 'Soal tidak termasuk dalam sesi ini')
            if qid in seen:
                raise PsychotestError(400, 'DUPLICATE_ANSWER', 'Satu soal hanya boleh dikirim sekali per permintaan')
            if not isinstance(oid, str) or oid not in allowed[qid]:
                raise PsychotestError(400, 'INVALID_OPTION', 'Pilihan jawaban tidak valid untuk soal ini')
            seen.add(qid)
            clean[qid] = oid
        now = self._now()
        expired = None
        with self.store.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            row = self.store.session_row(session_id, subject, db)
            if row['status'] == 'submitted':
                raise PsychotestError(409, 'ALREADY_SUBMITTED', 'Ujian ini sudah dikirim',
                                      {'result_url': f'/psychotests/sessions/{row["id"]}/result'})
            if now >= row['deadline_at']:
                expired = self._finalize_row(db, row, now)
            else:
                if expected_revision is not None and expected_revision != row['revision']:
                    raise PsychotestError(409, 'REVISION_CONFLICT', 'Progres jawaban sudah berubah',
                                          {'current_revision': row['revision']})
                saved = _loads(row['answers_json'], {})
                saved.update(clean)
                revision = row['revision'] + 1
                db.execute('UPDATE sessions SET answers_json=?,revision=?,last_activity_at=? WHERE id=?',
                           (_json(saved), revision, now, session_id))
                row = db.execute('SELECT * FROM sessions WHERE id=?', (session_id,)).fetchone()
        if expired is not None:
            raise PsychotestError(409, 'SESSION_EXPIRED', 'Waktu ujian telah habis; jawaban tidak disimpan',
                                  {'result_url': f'/psychotests/sessions/{session_id}/result',
                                   'score': expired['score'], 'auto_submitted': True})
        return {'ok': True, 'api_version': API_VERSION, 'session': self._public_session(row),
                'saved': len(clean), 'revision': row['revision']}

    def submit(self, session_id, subject):
        row = self._owned(session_id, subject)
        if row['status'] == 'submitted':
            raise PsychotestError(409, 'ALREADY_SUBMITTED', 'Ujian ini sudah dikirim',
                                  {'result_url': f'/psychotests/sessions/{row["id"]}/result'})
        row = self._finalize(session_id, subject, self._now(), reject_submitted=True)
        return self._result_response(row)

    def _result_response(self, row):
        return {'ok': True, 'api_version': API_VERSION, 'session': self._public_session(row),
                'result': _loads(row['result_json'], {})}

    def result(self, session_id, subject):
        row = self._ensure_not_expired(self._owned(session_id, subject), subject)
        if row['status'] != 'submitted':
            raise PsychotestError(409, 'SESSION_ACTIVE', 'Ujian belum selesai')
        return self._result_response(row)

    def _expire_due(self, subject=None):
        now = self._now()
        with self.store.connect() as db:
            if subject is None:
                ids = [r[0] for r in db.execute(
                    "SELECT id FROM sessions WHERE status='active' AND deadline_at<=? LIMIT 100", (now,))]
            else:
                ids = [r[0] for r in db.execute(
                    "SELECT id FROM sessions WHERE status='active' AND subject=? AND deadline_at<=? LIMIT 100",
                    (subject, now))]
        for session_id in ids:
            try:
                self._finalize(session_id, subject, now, auto=True)
            except PsychotestError:
                continue

    def history(self, subject, limit=20, offset=0):
        self._expire_due(subject)
        with self.store.connect() as db:
            rows = db.execute('''SELECT * FROM sessions WHERE subject=? AND status='submitted'
                ORDER BY submitted_at DESC,id DESC LIMIT ? OFFSET ?''', (subject, limit, offset)).fetchall()
            total = db.execute("SELECT COUNT(*) FROM sessions WHERE subject=? AND status='submitted'",
                               (subject,)).fetchone()[0]
        items = []
        for row in rows:
            result = _loads(row['result_json'], {})
            cfg = TESTS[row['test_id']]
            item = {'id': row['id'], 'test_id': row['test_id'], 'title': row['title'],
                    'family': row['family'], 'family_label': FAMILY_LABELS[row['family']],
                    'category': row['title'], 'submitted_at': row['submitted_at'],
                    'submitted_at_iso': self._iso(row['submitted_at']),
                    'duration_seconds': row['duration_seconds'], 'duration_used': row['duration_used'],
                    'auto_submitted': bool(row['auto_submitted']), 'status': row['status'],
                    'score': row['score'], 'max_score': row['max_score'], 'percent': row['percent'],
                    'result_label': result.get('result_label'),
                    'threshold_met': result.get('threshold', {}).get('met') if result.get('threshold') else None}
            for key in ('formation', 'formation_label', 'mode_group', 'simulation', 'simulation_label'):
                if cfg.get(key) is not None:
                    item[key] = cfg[key]
            analysis = result.get('analysis')
            if analysis:
                item['weakest_competencies'] = analysis.get('weakest_competencies', [])
            items.append(item)
        return {'ok': True, 'api_version': API_VERSION, 'items': items, 'count': len(items),
                'total': total, 'limit': limit, 'offset': offset}

    @staticmethod
    def _iso(epoch):
        if epoch is None:
            return None
        from datetime import datetime, timezone
        return datetime.fromtimestamp(epoch, timezone.utc).isoformat()

    def stats(self, subject):
        self._expire_due(subject)
        with self.store.connect() as db:
            rows = db.execute("SELECT * FROM sessions WHERE subject=? AND status='submitted' ORDER BY submitted_at,id",
                              (subject,)).fetchall()
        scored, categories, by_family = [], defaultdict(lambda: {
            'family': None, 'category': None, 'label': None, 'attempts': 0, 'earned': 0,
            'max': 0, 'correct': 0, 'wrong': 0, 'blank': 0}), defaultdict(lambda: {'tests': 0, 'percent_sum': 0, 'scored': 0})
        by_formation = defaultdict(lambda: {'formation': None, 'label': None, 'tests': 0,
                                            'percent_sum': 0, 'scored': 0})
        total_blank = 0
        personality_attempts = 0
        for row in rows:
            result = _loads(row['result_json'], {})
            if row['kind'] == 'personality':
                personality_attempts += 1
            if result.get('percent') is not None:
                scored.append({'session_id': row['id'], 'test_id': row['test_id'], 'title': row['title'],
                               'family': row['family'], 'score': row['score'], 'max_score': row['max_score'],
                               'percent': row['percent'], 'submitted_at': row['submitted_at'],
                               'submitted_at_iso': self._iso(row['submitted_at']),
                               'duration_used': row['duration_used']})
                fam = by_family[row['family']]
                fam['tests'] += 1
                fam['percent_sum'] += row['percent']
                fam['scored'] += 1
                cfg = TESTS[row['test_id']]
                if cfg.get('formation'):
                    formation = by_formation[cfg['formation']]
                    formation.update(formation=cfg['formation'], label=cfg['formation_label'])
                    formation['tests'] += 1
                    formation['percent_sum'] += row['percent']
                    formation['scored'] += 1
            total_blank += row['blank_count'] or 0
            for cat in result.get('categories', []):
                key = (row['family'], cat['category'])
                item = categories[key]
                item.update(family=row['family'], category=cat['category'], label=cat['label'])
                item['attempts'] += 1
                item['earned'] += cat.get('earned') or 0
                item['max'] += cat.get('max') or 0
                item['correct'] += cat.get('correct') or 0
                item['wrong'] += cat.get('wrong') or 0
                item['blank'] += cat.get('blank') or 0
        cat_items = []
        for item in categories.values():
            if item['max']:
                item['percent'] = round(item['earned'] / item['max'] * 100, 2)
                cat_items.append(item)
        cat_items.sort(key=lambda x: (-x['percent'], x['family'], x['category']))
        strongest = cat_items[0] if cat_items else None
        weakest = cat_items[-1] if cat_items else None
        objective_correct = sum(x['correct'] or 0 for x in cat_items)
        objective_wrong = sum(x['wrong'] or 0 for x in cat_items)
        objective_answered = objective_correct + objective_wrong
        percentages = [x['percent'] for x in scored]
        trend = scored[-30:]
        averages = {family: round(item['percent_sum'] / item['scored'], 2)
                    for family, item in by_family.items() if item['scored'] and family != 'skb'}
        formation_items = []
        for item in by_formation.values():
            if item['scored']:
                formation_items.append({'formation': item['formation'], 'label': item['label'],
                                        'tests': item['tests'],
                                        'average_percent': round(item['percent_sum'] / item['scored'], 2)})
        formation_items.sort(key=lambda x: (x['label'], x['formation']))
        return {'ok': True, 'api_version': API_VERSION,
                'total_tests': len(rows), 'scored_tests': len(scored),
                'personality_attempts': personality_attempts,
                'average_percent': round(sum(percentages) / len(percentages), 2) if percentages else None,
                'best_percent': max(percentages) if percentages else None,
                'latest_percent': percentages[-1] if percentages else None,
                'first_percent': percentages[0] if percentages else None,
                'improvement_percent': round(percentages[-1] - percentages[0], 2) if len(percentages) > 1 else None,
                'accuracy_percent': round(objective_correct / objective_answered * 100, 2) if objective_answered else None,
                'accuracy_counts': {'correct': objective_correct, 'wrong': objective_wrong, 'blank': total_blank},
                'by_family': averages, 'by_formation': formation_items, 'categories': cat_items,
                'strongest': strongest, 'weakest': weakest, 'trend': trend,
                'note': 'Statistik hanya mencakup riwayat akun yang meminta; TKP dan kepribadian tidak dihitung sebagai benar/salah. Skor SKB tidak digabung antarfomasi; lihat by_formation.'}

    def leaderboard(self, board='all', test_id=None, limit=10):
        if board not in ('private', 'cpns', 'skb', 'all'):
            raise PsychotestError(400, 'INVALID_BOARD', 'board harus private, cpns, skb, atau all')
        self._expire_due()
        if test_id is not None:
            if test_id not in TESTS:
                raise PsychotestError(404, 'TEST_NOT_FOUND', 'Tes tidak ditemukan')
            cfg = TESTS[test_id]
            if not cfg['leaderboard']:
                raise PsychotestError(400, 'LEADERBOARD_UNAVAILABLE', 'Kepribadian tidak memiliki peringkat objektif')
            if board != 'all' and cfg['family'] != board:
                raise PsychotestError(400, 'BOARD_MISMATCH', 'Tes tidak termasuk papan peringkat yang diminta')
            selected = [test_id]
        else:
            selected = [tid for tid in TEST_ORDER if TESTS[tid]['leaderboard']
                        and (board == 'all' or TESTS[tid]['family'] == board)]
        if not selected:
            raise PsychotestError(404, 'LEADERBOARD_EMPTY', 'Belum ada tes yang dapat diperingkat')
        placeholders = ','.join('?' for _ in selected)
        with self.store.connect() as db:
            rows = db.execute("SELECT * FROM sessions WHERE status='submitted' AND retry_of IS NULL AND test_id IN ("
                              + placeholders + ")", tuple(selected)).fetchall()
        boards = []
        for tid in selected:
            best = {}
            for row in rows:
                if row['test_id'] != tid or row['score'] is None:
                    continue
                candidate = (row['score'], -(row['duration_used'] or 0), -(row['submitted_at'] or 0))
                current = best.get(row['subject'])
                if current is None or candidate > current[0]:
                    best[row['subject']] = (candidate, row)
            entries = []
            for subject, (_, row) in best.items():
                entries.append({'alias': self.alias(subject), 'score': row['score'],
                                'max_score': row['max_score'], 'percent': row['percent'],
                                'duration_seconds': row['duration_used'], 'submitted_at': row['submitted_at'],
                                'submitted_at_iso': self._iso(row['submitted_at'])})
            entries.sort(key=lambda x: (-x['score'], x['duration_seconds'], x['submitted_at'], x['alias']))
            for rank, entry in enumerate(entries, 1):
                entry['rank'] = rank
            cfg = TESTS[tid]
            board_item = {'test_id': tid, 'title': cfg['title'], 'family': cfg['family'],
                          'family_label': FAMILY_LABELS[cfg['family']], 'score_scale': cfg['score_scale'],
                          'total_participants': len(entries), 'entries': entries[:limit]}
            for key in ('formation', 'formation_label', 'mode_group', 'simulation', 'simulation_label'):
                if cfg.get(key) is not None:
                    board_item[key] = cfg[key]
            boards.append(board_item)
        return {'ok': True, 'api_version': API_VERSION, 'board': board, 'limit': limit,
                'tie_breaker': 'skor tertinggi, durasi terpendek, pengiriman paling awal, alias',
                'privacy': 'alias anonim; tidak ada email, account_id, atau data pribadi',
                'boards': boards}

    def health(self, port):
        counts = self.store.counts()
        modes = [tid for tid in TEST_ORDER if not self._availability(tid)]
        return {'ok': True, 'api_version': API_VERSION, 'status': 'ok',
                'service': 'automate-jobs-v2-psychotest', 'bind': {'host': '127.0.0.1', 'port': port},
                'uptime_seconds': round(self.clock() - self.started, 3),
                'bank_version': self.store.bank_version(),
                'questions': sum(counts.values()), 'categories': len(counts),
                'modes_available': modes, 'modes_total': len(TEST_ORDER),
                'auth_bridge': 'existing /auth/me read-only', 'identity_storage': False,
                'email_sending_enabled': False, 'upstream_network_check': False}
