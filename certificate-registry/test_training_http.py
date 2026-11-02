"""Real HTTP integration tests: auth -> questions -> exam -> registry verification."""
import json
import os
import tempfile
import threading
import unittest
from http.server import ThreadingHTTPServer
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

import server
from trusted_training import initialize, add_course_from_trusted_catalog, mark_module_complete

class TrainingHttpIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.original_db = server.DB
        server.DB = os.path.join(self.temp.name, "test.sqlite3")
        with server.connect() as conn:
            initialize(conn)
            add_course_from_trusted_catalog(conn, "warehouse", "Warehouse", ["lesson"], [
                {"prompt":f"Q{i}","options":["A","B"],"correct":0} for i in range(5)
            ])
            mark_module_complete(conn, "authenticated-42", "warehouse", 0, server_verified=True)
        self.httpd = ThreadingHTTPServer(("127.0.0.1", 0), server.Handler)
        self.worker = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.worker.start()
        self.base = "http://127.0.0.1:" + str(self.httpd.server_port)
        self.config = patch.dict(os.environ, {"KARIRMATE_AUTH_ME_URL":"https://auth.example.test/auth/me"})
        self.config.start()
        self.auth = patch("training_api.authenticated_identity", side_effect=lambda header: ("authenticated-42","Peserta") if header=="Bearer valid" else None)
        self.auth.start()

    def tearDown(self):
        self.auth.stop()
        self.config.stop()
        self.httpd.shutdown()
        self.httpd.server_close()
        self.worker.join(timeout=3)
        server.DB = self.original_db
        self.temp.cleanup()

    def call(self, path, payload=None, token=None):
        headers = {"Content-Type":"application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        req = Request(self.base + path, data=json.dumps(payload).encode() if payload is not None else None, headers=headers)
        try:
            with urlopen(req, timeout=3) as response:
                return response.status, json.load(response)
        except HTTPError as exc:
            return exc.code, json.load(exc)

    def test_requires_login(self):
        self.assertEqual(self.call("/training/courses/warehouse/questions")[0], 401)
        self.assertEqual(self.call("/training/exams", {"courseId":"warehouse","answers":[0]*5})[0], 401)

    def test_authenticated_exam_issues_verifiable_certificate(self):
        code, questions = self.call("/training/courses/warehouse/questions", token="valid")
        self.assertEqual(code, 200)
        self.assertEqual(len(questions["questions"]), 5)
        self.assertNotIn("correct", questions["questions"][0])
        payload = {"courseId":"warehouse","answers":[0]*5,"userId":"forged-id","score":100}
        code, result = self.call("/training/exams", payload, "valid")
        self.assertEqual(code, 200)
        self.assertEqual(result["score"], 100)
        cid = result["certificateId"]
        self.assertEqual(self.call("/verify?id="+cid)[1]["certificate"]["id"], cid)
        self.assertEqual(self.call("/training/exams", payload, "valid")[1]["certificateId"], cid)
        with server.connect() as conn:
            self.assertEqual(conn.execute("SELECT user_id FROM training_attempts WHERE certificate_id=?", (cid,)).fetchone()[0], "authenticated-42")

if __name__=="__main__":
    unittest.main()
