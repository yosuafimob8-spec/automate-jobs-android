# KarirMate AI Interview backend (belum di-deploy)

FastAPI proxy untuk OpenRouter. **Jangan simpan API key di APK atau GitHub.**

Environment variables (secrets di hosting):
- `OPENROUTER_API_KEY`: API key OpenRouter milik operator
- `OPENROUTER_MODEL`: ID model OpenRouter yang tersedia dan mendukung output JSON
- `AUTH_VERIFY_URL`: endpoint HTTPS autentikasi KarirMate yang memvalidasi bearer token dan mengembalikan HTTP 200 hanya untuk sesi valid

Jalankan: `pip install -r backend/ai_interview/requirements.txt` lalu `uvicorn backend.ai_interview.main:app --host 0.0.0.0 --port 8790`.

Untuk Android release, set `INTERVIEW_BASE_URL` ke alamat HTTPS backend ini saat build (periksa Gradle project). Backend **tidak siap produksi** sebelum autentikasi, rate limiting, batas biaya, hosting HTTPS, dan uji end-to-end selesai. Tidak ada API key yang disertakan.
