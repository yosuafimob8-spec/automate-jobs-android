import os
import sqlite3
import tempfile
import unittest
from unittest.mock import patch
import server
from trusted_training import initialize, add_course_from_trusted_catalog, mark_module_complete, public_questions, submit_exam

class TrustedTrainingTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.old=server.DB
        server.DB=os.path.join(self.tmp.name,"certs.sqlite")
        self.conn=server.connect()
        initialize(self.conn)
        add_course_from_trusted_catalog(self.conn,"warehouse","Warehouse",["intro","safety"],
            [{"prompt":f"Question {i}","options":["yes","no"],"correct":0} for i in range(5)])
        self.conn.commit()

    def tearDown(self):
        self.conn.close()
        server.DB=self.old
        self.tmp.cleanup()

    def test_no_unverified_module_completion(self):
        with self.assertRaises(PermissionError):
            mark_module_complete(self.conn,"user-1","warehouse",0)

    def test_requires_ordered_modules(self):
        with self.assertRaises(ValueError):
            mark_module_complete(self.conn,"user-1","warehouse",1,server_verified=True)

    def test_no_early_exam(self):
        with self.assertRaisesRegex(ValueError,"modules incomplete"):
            submit_exam(self.conn,"user-1","warehouse",[0]*5,"Peserta")

    def test_questions_do_not_expose_key(self):
        self.assertNotIn("correct",public_questions(self.conn,"warehouse")[0])

    def test_failing_grade_never_issues_certificate(self):
        for i in range(2):
            mark_module_complete(self.conn,"user-1","warehouse",i,server_verified=True)
        with self.assertRaisesRegex(ValueError,"passing grade"):
            submit_exam(self.conn,"user-1","warehouse",[1]*5,"Peserta")
        self.assertEqual(self.conn.execute("SELECT COUNT(*) FROM certificates").fetchone()[0],0)

    def test_uncommitted_issuance_rolls_back_entirely(self):
        for i in range(2):
            mark_module_complete(self.conn,"user-1","warehouse",i,server_verified=True)
        self.conn.commit()
        with self.assertRaises(RuntimeError):
            with self.conn:
                submit_exam(self.conn,"user-1","warehouse",[0]*5,"Peserta")
                raise RuntimeError("simulate downstream failure")
        self.assertEqual(self.conn.execute("SELECT COUNT(*) FROM certificates").fetchone()[0],0)
        self.assertEqual(self.conn.execute("SELECT COUNT(*) FROM training_attempts").fetchone()[0],0)

    def test_success_issued_once_and_revocation_reflected(self):
        for i in range(2):
            mark_module_complete(self.conn,"user-1","warehouse",i,server_verified=True)
        with self.conn:
            issued=submit_exam(self.conn,"user-1","warehouse",[0]*5,"Peserta")
        self.assertEqual(issued["score"],100)
        self.assertTrue(server.verify(issued["certificateId"]))
        with self.conn:
            again=submit_exam(self.conn,"user-1","warehouse",[0]*5,"Peserta")
        self.assertEqual(issued["certificateId"],again["certificateId"])
        self.assertEqual(self.conn.execute("SELECT COUNT(*) FROM certificates").fetchone()[0],1)
        self.conn.execute("UPDATE certificates SET revoked=1 WHERE id=?",(issued["certificateId"],))
        self.conn.commit()
        self.assertEqual(server.verify(issued["certificateId"])["revoked"],1)

if __name__=="__main__":
    unittest.main()
