"""Server-owned completion checks. No client-supplied score or certificate identity."""
import hashlib
import hmac
import json
import sqlite3
import uuid
from datetime import datetime, timezone

def setup(conn):
    conn.executescript("""
    CREATE TABLE IF NOT EXISTS training_attempts (
      attempt_id TEXT PRIMARY KEY, user_id TEXT NOT NULL, course_id TEXT NOT NULL,
      question_set_hash TEXT NOT NULL, answers_hash TEXT NOT NULL,
      score INTEGER NOT NULL CHECK(score BETWEEN 0 AND 100),
      completed_modules INTEGER NOT NULL, required_modules INTEGER NOT NULL,
      graded_at TEXT NOT NULL, certificate_id TEXT UNIQUE
    );
    CREATE UNIQUE INDEX IF NOT EXISTS one_certificate_per_course
      ON training_attempts(user_id,course_id) WHERE certificate_id IS NOT NULL;
    """)

def record_server_grade(conn, *, attempt_id, user_id, course_id, questions, answers, completed_modules, required_modules):
    """Call only from authenticated server-side grading after server-side module checks.
    Never accept a score, answer key, or module count from the Android client.
    """
    if not questions or len(questions) != len(answers):
        raise ValueError("invalid assessment")
    if not isinstance(completed_modules, int) or not isinstance(required_modules, int) or required_modules < 1 or completed_modules != required_modules:
        raise ValueError("modules incomplete")
    if any(not isinstance(q, dict) or type(q.get("correct")) is not int or not isinstance(q.get("options"), list) or q["correct"] not in range(len(q["options"])) for q in questions):
        raise ValueError("invalid question bank")
    score = sum(type(a) is int and a == q["correct"] for a, q in zip(answers, questions)) * 100 // len(questions)
    if score < 80:
        raise ValueError("passing grade required")
    now = datetime.now(timezone.utc).isoformat()
    question_hash = hashlib.sha256(json.dumps(questions,sort_keys=True).encode()).hexdigest()
    answers_hash = hashlib.sha256(json.dumps(answers).encode()).hexdigest()
    conn.execute("INSERT INTO training_attempts VALUES (?,?,?,?,?,?,?,?,?,NULL)",
                 (attempt_id,user_id,course_id,question_hash,answers_hash,score,completed_modules,required_modules,now))
    return score

def issue_from_attempt(conn, *, attempt_id, name, course_title, completion_date):
    """Atomic issue from a recorded server-graded attempt, not a client claim."""
    setup(conn)
    row = conn.execute("SELECT user_id,course_id,score,completed_modules,required_modules,certificate_id FROM training_attempts WHERE attempt_id=?", (attempt_id,)).fetchone()
    if row is None or row[2] < 80 or row[3] != row[4]:
        raise ValueError("no eligible server-graded attempt")
    if row[5]:
        return row[5]
    certificate_id = str(uuid.uuid4())
    conn.execute("""INSERT INTO certificates(id,name,course,course_id,date,score,issued_at)
                    VALUES(?,?,?,?,?,?,?)""",
                 (certificate_id,name,course_title,row[1],completion_date,row[2],datetime.now(timezone.utc).isoformat()))
    conn.execute("UPDATE training_attempts SET certificate_id=? WHERE attempt_id=? AND certificate_id IS NULL", (certificate_id,attempt_id))
    return certificate_id
