import os
import tempfile
import unittest
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

    def test_revocation(self):
        server.register(self.record)
        with server.connect() as conn:
            conn.execute("UPDATE certificates SET revoked=1 WHERE id=?", (self.record["id"],))
        self.assertEqual(server.verify(self.record["id"])["revoked"], 1)

if __name__ == "__main__":
    unittest.main()
