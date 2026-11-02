"""Server catalog parity and startup seeding tests (no hosting required)."""
import json
import sqlite3
import tempfile
import unittest
from pathlib import Path
from trusted_training import initialize, seed_trusted_catalog, public_questions

class CatalogTests(unittest.TestCase):
    def setUp(self):
        self.conn = sqlite3.connect(":memory:")
        self.conn.execute("""CREATE TABLE certificates (
          id TEXT PRIMARY KEY, name TEXT NOT NULL, course TEXT NOT NULL,
          course_id TEXT NOT NULL, date TEXT NOT NULL, score INTEGER NOT NULL,
          issued_at TEXT NOT NULL, revoked INTEGER NOT NULL DEFAULT 0
        )""")
        initialize(self.conn)

    def tearDown(self):
        self.conn.close()

    def test_all_android_courses_are_server_owned(self):
        count = seed_trusted_catalog(self.conn)
        self.assertEqual(count, 240)
        rows = self.conn.execute("SELECT course_id, lessons, questions FROM course_bank").fetchall()
        self.assertEqual(len(rows), 240)
        for course_id, lessons, questions in rows:
            self.assertGreaterEqual(len(json.loads(lessons)), 3)
            self.assertEqual(len(json.loads(questions)), 20)
            self.assertEqual(len(public_questions(self.conn, course_id)), 20)
            self.assertTrue(all("correct" not in q for q in public_questions(self.conn, course_id)))
        self.assertEqual(seed_trusted_catalog(self.conn), 240)
        self.assertEqual(self.conn.execute("SELECT COUNT(*) FROM course_bank").fetchone()[0], 240)

    def test_updated_questions_replace_bank_without_erasing_progress(self):
        seed_trusted_catalog(self.conn)
        self.conn.execute("INSERT INTO module_progress VALUES (?,?,?)", ("user-1", "course-1", 0))
        original = self.conn.execute("SELECT course_id, questions FROM course_bank ORDER BY course_id LIMIT 1").fetchone()
        course_id, original_questions = original
        self.conn.execute("UPDATE course_bank SET questions=? WHERE course_id=?",
                          (json.dumps([{"prompt": "Old question", "options": ["yes", "no"], "correct": 0}]), course_id))
        self.assertEqual(seed_trusted_catalog(self.conn), 240)
        self.assertEqual(self.conn.execute("SELECT questions FROM course_bank WHERE course_id=?",
                                           (course_id,)).fetchone()[0], original_questions)
        self.assertEqual(self.conn.execute("SELECT COUNT(*) FROM module_progress WHERE user_id='user-1'").fetchone()[0], 1)

    def test_catalog_update_keeps_course_identity_and_lessons_locked(self):
        seed_trusted_catalog(self.conn)
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "modified.json"
            catalog = json.loads(Path(__file__).with_name("trusted_catalog.json").read_text(encoding="utf-8"))
            catalog[0]["lessons"] = ["changed lesson"]
            path.write_text(json.dumps(catalog), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "catalog version conflict"):
                seed_trusted_catalog(self.conn, path)

if __name__ == "__main__":
    unittest.main()
