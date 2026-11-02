import os
import tempfile
import unittest
import json
import threading
from http.server import ThreadingHTTPServer
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from unittest.mock import patch
import server

class RegistryTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.old = server.DB
        server.DB = os.path.join(self.tmp.name, "test.sqlite3")
        server.connect().close()
        self.record = {
            "id": "123e4567-e89b-12d3-a456-426614174000",
            "name": "Peserta Contoh",
            "course": "Admin Warehouse - Basic",
            "courseId": "role_admin_warehouse_basic",
            "date": "2026-11-02",
            "score": 90,
        }

    def tearDown(self):
        server.DB = self.old
        self.tmp.cleanup()

    def test_registration_and_public_verification(self):
        self.assertIsNone(server.verify(self.record["id"]))
        self.assertEqual(server.register(self.record), self.record["id"])
        verified = server.verify(self.record["id"])
        self.assertEqual(verified["name"], "Peserta Contoh")
        self.assertEqual(verified["score"], 90)
        self.assertEqual(verified["revoked"], 0)

    def test_duplicate_rejected(self):
        import sqlite3
        server.register(self.record)
        with self.assertRaises(sqlite3.IntegrityError):
            server.register(self.record)

    def test_forged_or_invalid_certificate_rejected(self):
        for change in ({"score": 79}, {"score": True}, {"id": "fake"}, {"date": "2026-99-99"}, {"name": ""}):
            with self.assertRaises(ValueError):
                server.register({**self.record, **change})
        self.assertIsNone(server.verify("not-an-id"))

    def test_public_api_and_authorization(self):
        old_token = server.TOKEN
        server.TOKEN = "test-secret-only"
        httpd = ThreadingHTTPServer(("127.0.0.1", 0), server.Handler)
        thread = threading.Thread(target=httpd.serve_forever, daemon=True)
        thread.start()
        base = "http://127.0.0.1:" + str(httpd.server_port)
        def request(path, data=None, token=None):
            headers = {"Content-Type": "application/json"}
            if token:
                headers["Authorization"] = "Bearer " + token
            req = Request(base + path, data=json.dumps(data).encode() if data is not None else None, headers=headers)
            try:
                with urlopen(req, timeout=3) as response:
                    return response.status, json.load(response)
            except HTTPError as exc:
                return exc.code, json.load(exc)
        try:
            self.assertEqual(request("/health")[0], 200)
            self.assertEqual(request("/verify?id=" + self.record["id"])[0], 404)
            self.assertEqual(request("/certificates", self.record)[0], 401)
            self.assertEqual(request("/certificates", self.record, "wrong")[0], 401)
            self.assertEqual(request("/certificates", self.record, "test-secret-only")[0], 201)
            self.assertEqual(request("/certificates", self.record, "test-secret-only")[0], 409)
            status, result = request("/verify?id=" + self.record["id"])
            self.assertEqual(status, 200)
            self.assertTrue(result["valid"])
            self.assertEqual(result["certificate"]["name"], self.record["name"])
            with server.connect() as conn:
                conn.execute("UPDATE certificates SET revoked=1 WHERE id=?", (self.record["id"],))
            self.assertFalse(request("/verify?id=" + self.record["id"])[1]["valid"])
        finally:
            httpd.shutdown()
            httpd.server_close()
            thread.join(timeout=3)
            server.TOKEN = old_token

    def test_revocation(self):
        server.register(self.record)
        with server.connect() as conn:
            conn.execute("UPDATE certificates SET revoked=1 WHERE id=?", (self.record["id"],))
        self.assertEqual(server.verify(self.record["id"])["revoked"], 1)

if __name__ == "__main__":
    unittest.main()
