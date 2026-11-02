import importlib.util
import pathlib
import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient

path = pathlib.Path(__file__).resolve().parents[1] / "interview_api.py"
spec = importlib.util.spec_from_file_location("interview_api", path)
api = importlib.util.module_from_spec(spec)
spec.loader.exec_module(api)


class FakeResponse:
    status_code = 200
    def raise_for_status(self):
        return None
    def json(self):
        return {"message": {"content": '{"question":"Ceritakan keputusan yang Anda ambil saat stok tidak sesuai.","feedback":"Jawaban menyebut selisih stok.","answer_score":82,"improvement":"Jelaskan langkah verifikasi.","awaiting_answer":true}'}}


class FakeClient:
    payloads = []
    async def __aenter__(self):
        return self
    async def __aexit__(self, *args):
        return None
    async def get(self, url, **kwargs):
        return FakeResponse()
    async def post(self, url, **kwargs):
        self.payloads.append(kwargs["json"])
        return FakeResponse()


class InterviewTests(unittest.TestCase):
    def setUp(self):
        FakeClient.payloads = []
        self.client = TestClient(api.app)
        self.auth = {"Authorization": "Bearer test-token"}
        self.base = {"role": "Supervisor Logistik", "language": "id", "action": "start", "history": []}

    def test_health(self):
        self.assertEqual(self.client.get("/health").status_code, 200)

    def test_missing_token(self):
        self.assertEqual(self.client.post("/interview/turn", json=self.base).status_code, 401)

    def test_start_returns_question(self):
        with patch.object(api, "AUTH_URL", "http://auth.test/verify"), patch.object(api.httpx, "AsyncClient", FakeClient):
            response = self.client.post("/interview/turn", headers=self.auth, json=self.base)
        self.assertEqual(response.status_code, 200, response.text)
        self.assertTrue(response.json()["awaiting_answer"])
        self.assertIn("stok", response.json()["question"])

    def test_follow_up_receives_answer_history(self):
        req = dict(self.base, action="reply", history=[
            {"question": "Apa pengalaman Anda mengelola gudang?", "answer": "Saya menangani stok 2000 unit dan selisih inventaris."}
        ])
        with patch.object(api, "AUTH_URL", "http://auth.test/verify"), patch.object(api.httpx, "AsyncClient", FakeClient):
            response = self.client.post("/interview/turn", headers=self.auth, json=req)
        self.assertEqual(response.status_code, 200, response.text)
        messages = FakeClient.payloads[-1]["messages"]
        self.assertTrue(any("2000 unit" in m["content"] for m in messages))
        self.assertTrue(any("follow-up" in m["content"] for m in messages))

    def test_reply_without_history_rejected(self):
        req = dict(self.base, action="reply")
        self.assertEqual(self.client.post("/interview/turn", headers=self.auth, json=req).status_code, 422)

    def test_invalid_model_json(self):
        with self.assertRaises(Exception):
            api.parse_json("invalid response")


if __name__ == "__main__":
    unittest.main()
