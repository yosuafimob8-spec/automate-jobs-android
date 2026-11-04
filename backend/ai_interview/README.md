# KarirMate AI Interview — Gemma 3 online (belum di-deploy)

Android hanya menghubungi backend HTTPS. Tidak ada model GGUF atau mode offline di layar AI Interview.

## VPS Ubuntu

1. Pasang Ollama dan unduh model: `ollama pull gemma3:4b`.
2. Pastikan Ollama hanya mendengarkan `127.0.0.1:11434`; jangan buka port Ollama ke internet.
3. Pasang dependensi: `pip install -r backend/ai_interview/requirements.txt`.
4. Set `AUTH_VERIFY_URL` ke endpoint HTTPS autentikasi KarirMate yang mengembalikan 200 hanya untuk token valid.
5. Jalankan `uvicorn backend.ai_interview.main:app --host 127.0.0.1 --port 8790` dan pasang reverse proxy HTTPS (misalnya Caddy/Nginx).
6. Saat build Android, set `INTERVIEW_BASE_URL` ke domain HTTPS backend (tanpa suffix /interview/turn).

Opsional: `GEMMA_MODEL=gemma3:4b`, `OLLAMA_URL=http://127.0.0.1:11434/api/chat`.

## Keamanan dan status

Autentikasi wajib. Tambahkan rate limiting, pembatasan konkurensi, TLS, monitoring dan uji end-to-end sebelum dipakai pengguna. Tidak ada VPS/domain yang dideploy dalam perubahan kode ini. Belum dilakukan build APK atau uji Android. Kecepatan model 4B di CPU harus diuji pada VPS yang dibeli.
