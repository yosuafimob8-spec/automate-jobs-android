"""Regression checks for 240 course exam banks. Run: python3 -m unittest test_exam_bank.py"""
import json
import sqlite3
import unittest
from pathlib import Path

from trusted_training import initialize, seed_trusted_catalog, expand_exam_questions


class ExamBankTests(unittest.TestCase):
    def test_all_courses_have_twenty_distinct_prompts_and_valid_keys(self):
        conn = sqlite3.connect(":memory:")
        initialize(conn)
        self.assertEqual(seed_trusted_catalog(conn), 240)
        rows = conn.execute("SELECT course_id,questions FROM course_bank").fetchall()
        self.assertEqual(len(rows), 240)
        for course_id, payload in rows:
            questions = json.loads(payload)
            with self.subTest(course_id=course_id):
                self.assertEqual(len(questions), 20)
                self.assertEqual(len({q["prompt"] for q in questions}), 20)
                for q in questions:
                    self.assertEqual(len(q["options"]), 3)
                    self.assertEqual(len(set(q["options"])), 3)
                    self.assertIs(type(q["correct"]), int)
                    self.assertTrue(0 <= q["correct"] < len(q["options"]))

    def test_legacy_synthetic_twenty_question_bank_migrates_without_losing_other_tables(self):
        conn = sqlite3.connect(":memory:")
        initialize(conn)
        catalog = json.loads(Path(__file__).with_name("trusted_catalog.json").read_text(encoding="utf-8"))
        course = catalog[0]
        old_five = course["questions"][:5]
        old_twenty = old_five + [
            dict(q, prompt=prefix + q["prompt"])
            for prefix in (
                "Dalam simulasi pekerjaan ",
                "Saat memeriksa hasil pekerjaan ",
                "Dalam evaluasi praktik ",
            )
            for q in old_five
        ]
        conn.execute("INSERT INTO course_bank VALUES(?,?,?,?)", (
            course["courseId"], course["title"],
            json.dumps(course["lessons"]), json.dumps(old_twenty)))
        conn.execute("INSERT INTO module_progress VALUES(?,?,?)", ("existing-user", course["courseId"], 0))
        self.assertEqual(seed_trusted_catalog(conn), 240)
        self.assertEqual(conn.execute("SELECT COUNT(*) FROM module_progress").fetchone()[0], 1)
        updated = json.loads(conn.execute("SELECT questions FROM course_bank WHERE course_id=?", (course["courseId"],)).fetchone()[0])
        self.assertEqual(updated, course["questions"])


if __name__ == "__main__":
    unittest.main()
