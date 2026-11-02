import io
import json
import os
import unittest
from unittest.mock import patch
from training_api import authenticated_identity

class FakeResponse:
    status = 200
    def __init__(self, body):
        self.stream = io.BytesIO(json.dumps(body).encode())
    def __enter__(self): return self
    def __exit__(self, *args): self.stream.close()
    def read(self, n=-1): return self.stream.read(n)

class TrainingAuthTests(unittest.TestCase):
    def test_missing_auth_backend_fails_closed(self):
        with patch.dict(os.environ, {"KARIRMATE_AUTH_ME_URL":""}):
            with self.assertRaises(RuntimeError):
                authenticated_identity("Bearer abc")

    def test_rejects_unauthenticated_user(self):
        with patch.dict(os.environ, {"KARIRMATE_AUTH_ME_URL":"https://auth.example.test/auth/me"}):
            self.assertIsNone(authenticated_identity("", opener=lambda *args,**kwargs: None))

    def test_uses_server_identity_not_client_claim(self):
        def open_auth(req, timeout):
            self.assertEqual(req.get_header("Authorization"), "Bearer valid")
            return FakeResponse({"id":"server-user-123","email":"user@example.test"})
        with patch.dict(os.environ, {"KARIRMATE_AUTH_ME_URL":"https://auth.example.test/auth/me"}):
            self.assertEqual(authenticated_identity("Bearer valid", opener=open_auth),
                             ("server-user-123","Peserta KarirMate"))

    def test_rejects_missing_user_id(self):
        with patch.dict(os.environ, {"KARIRMATE_AUTH_ME_URL":"https://auth.example.test/auth/me"}):
            self.assertIsNone(authenticated_identity("Bearer valid", opener=lambda *a,**kw: FakeResponse({"email":"x"})))

if __name__=="__main__":
    unittest.main()
