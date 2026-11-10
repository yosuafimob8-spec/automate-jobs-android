# KarirMate – Runtime source snapshots

- `otp-backend/`: Termux Python backend entrypoint, email verification handler, HTML mail template and inline images.
- `cloudflare-worker/`: Cloudflare Worker source and Wrangler configuration from dashboard.
- The Android application source remains under `app/src/` and the OTP UI updates are committed separately.
- This directory is for source control. Deploying the Worker, restarting the backend or building Android must be done explicitly.
- SMTP credentials, Cloudflare secrets, access tokens and live database files must **never** be committed.
