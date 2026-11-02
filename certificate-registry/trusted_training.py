"""Isolated server-side training workflow. NOT deployed or wired into Android.
Authentication must be supplied by the host server; never trust user_id from a request body.
The course bank and module progress are server-owned. No admin token in the app.
"""
import json
from pathlib import Path
import sqlite3
import uuid
from datetime import date
from trusted_issuance import setup, record_server_grade, issue_from_attempt

def initialize(conn):
    setup(conn)
    conn.executescript("""
    CREATE TABLE IF NOT EXISTS course_bank (
      course_id TEXT PRIMARY KEY, title TEXT NOT NULL, lessons TEXT NOT NULL,
      questions TEXT NOT NULL
    );
    CREATE TABLE IF NOT EXISTS module_progress (
      user_id TEXT NOT NULL, course_id TEXT NOT NULL, module_index INTEGER NOT NULL,
      PRIMARY KEY(user_id,course_id,module_index)
    );
    """)

def add_course_from_trusted_catalog(conn, course_id, title, lessons, questions):
    """Administrative import only; never expose as an unauthenticated public endpoint."""
    if not course_id or not title or not lessons or not questions:
        raise ValueError("incomplete course")
    if not all(isinstance(q,dict) and type(q.get("correct")) is int and isinstance(q.get("options"),list)
               and 0 <= q["correct"] < len(q["options"]) for q in questions):
        raise ValueError("invalid trusted questions")
    conn.execute("INSERT INTO course_bank VALUES(?,?,?,?)",
                 (course_id,title,json.dumps(lessons),json.dumps(questions)))

def mark_module_complete(conn, authenticated_user_id, course_id, module_index, *, server_verified=False):
    """Must be called only after an independent server-side lesson/practice validation."""
    if not server_verified:
        raise PermissionError("unverified client completion")
    row = conn.execute("SELECT lessons FROM course_bank WHERE course_id=?", (course_id,)).fetchone()
    if not row:
        raise ValueError("unknown course")
    lessons = json.loads(row[0])
    if type(module_index) is not int or module_index not in range(len(lessons)):
        raise ValueError("invalid module")
    if module_index:
        previous = conn.execute("SELECT 1 FROM module_progress WHERE user_id=? AND course_id=? AND module_index=?",
                                (authenticated_user_id,course_id,module_index-1)).fetchone()
        if not previous:
            raise ValueError("previous module incomplete")
    conn.execute("INSERT OR IGNORE INTO module_progress VALUES(?,?,?)",
                 (authenticated_user_id,course_id,module_index))

def public_questions(conn, course_id):
    row = conn.execute("SELECT questions FROM course_bank WHERE course_id=?", (course_id,)).fetchone()
    if not row:
        raise ValueError("unknown course")
    return [{"prompt":q["prompt"],"options":q["options"]} for q in json.loads(row[0])]

def submit_exam(conn, authenticated_user_id, course_id, answers, participant_name):
    """Atomic grading and issuance; only trusted identity, bank and progress are used."""
    if not authenticated_user_id or not isinstance(participant_name,str) or not 1 <= len(participant_name.strip()) <= 150:
        raise ValueError("authenticated identity and name required")
    row = conn.execute("SELECT title,lessons,questions FROM course_bank WHERE course_id=?", (course_id,)).fetchone()
    if not row:
        raise ValueError("unknown course")
    title, lessons, questions = row[0], json.loads(row[1]), json.loads(row[2])
    if not isinstance(answers,list) or len(answers) != len(questions) or any(type(a) is not int for a in answers):
        raise ValueError("invalid answers")
    completed = conn.execute("SELECT COUNT(*) FROM module_progress WHERE user_id=? AND course_id=?",
                             (authenticated_user_id,course_id)).fetchone()[0]
    if completed != len(lessons):
        raise ValueError("modules incomplete")
    existing = conn.execute("""SELECT certificate_id FROM training_attempts
                               WHERE user_id=? AND course_id=? AND certificate_id IS NOT NULL""",
                            (authenticated_user_id,course_id)).fetchone()
    if existing:
        return {"certificateId":existing[0],"alreadyIssued":True}
    attempt_id = str(uuid.uuid4())
    score = record_server_grade(conn,attempt_id=attempt_id,user_id=authenticated_user_id,
        course_id=course_id,questions=questions,answers=answers,
        completed_modules=completed,required_modules=len(lessons))
    certificate_id = issue_from_attempt(conn,attempt_id=attempt_id,name=participant_name.strip(),
                                         course_title=title,completion_date=date.today().isoformat())
    return {"certificateId":certificate_id,"score":score,"registered":True}

def expand_exam_questions(questions, course_title):
    """Interim 20-question scenario bank based on five existing concepts."""
    if len(questions) == 20:
        return questions
    if len(questions) != 5:
        raise ValueError("expected five or twenty questions")
    variants = [
        "Dalam simulasi pekerjaan {course}, tindakan mana yang paling tepat? {prompt}",
        "Saat memeriksa hasil pekerjaan {course}, jawaban mana yang sesuai? {prompt}",
        "Dalam evaluasi praktik {course}, pilihan mana yang paling bertanggung jawab? {prompt}",
    ]
    result = list(questions)
    for variant_index, template in enumerate(variants):
        for index, question in enumerate(questions):
            shift = (variant_index + index + 1) % len(question["options"])
            options = question["options"][shift:] + question["options"][:shift]
            result.append({
                "prompt": template.format(course=course_title, prompt=question["prompt"]),
                "options": options,
                "correct": (question["correct"] - shift) % len(options),
            })
    return result


def seed_trusted_catalog(conn, catalog_path=None):
    """Idempotent startup import of server-controlled, reviewed course material."""
    source = Path(catalog_path) if catalog_path else Path(__file__).with_name("trusted_catalog.json")
    courses = json.loads(source.read_text(encoding="utf-8"))
    if catalog_path is None:
        for role_file in sorted(Path(__file__).with_name("role_catalog").glob("category_*.json")):
            courses.extend(json.loads(role_file.read_text(encoding="utf-8")))
    if not isinstance(courses, list) or not courses:
        raise ValueError("empty trusted catalog")
    if len({item["courseId"] for item in courses}) != len(courses):
        raise ValueError("duplicate course IDs")
    for item in courses:
        course_id = item["courseId"]
        title = item["title"]
        lessons = item["lessons"]
        questions = expand_exam_questions(item["questions"], title)
        if not isinstance(lessons, list) or not lessons or not isinstance(questions, list) or len(questions) < 5:
            raise ValueError("invalid course material")
        if any(not isinstance(q.get("prompt"), str) or len(q.get("options", [])) < 2 for q in questions):
            raise ValueError("invalid question content")
        existing = conn.execute("SELECT title, lessons, questions FROM course_bank WHERE course_id=?", (course_id,)).fetchone()
        values = (title, json.dumps(lessons, ensure_ascii=False), json.dumps(questions, ensure_ascii=False))
        if existing:
            stored_questions = json.loads(existing[2])
            if (existing[0], json.loads(existing[1])) != (title, lessons):
                raise ValueError("catalog version conflict: " + course_id)
            if stored_questions != questions:
                if (len(stored_questions) == 5 and questions[:5] == stored_questions) or (len(stored_questions) == 20 and stored_questions[:5] == questions[:5] and all(any(q["prompt"].startswith(prefix) for prefix in ("Dalam simulasi pekerjaan ", "Saat memeriksa hasil pekerjaan ", "Dalam evaluasi praktik ")) for q in stored_questions[5:])):
                    conn.execute("UPDATE course_bank SET questions=? WHERE course_id=?",
                                 (json.dumps(questions, ensure_ascii=False), course_id))
                else:
                    raise ValueError("catalog version conflict: " + course_id)
        else:
            add_course_from_trusted_catalog(conn, course_id, title, lessons, questions)
    return len(courses)
