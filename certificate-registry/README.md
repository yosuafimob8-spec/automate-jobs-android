# KarirMate certificate verification — backend prototype

No APK build. Python standard library only.

## Local test
```sh
cd certificate-registry
python3 -m unittest -v test_server.py
REGISTRY_ADMIN_TOKEN='choose-a-long-random-secret' python3 server.py
```

Service listens on 127.0.0.1:8792. Deploy behind an HTTPS reverse proxy before making it public.

## Register a completed certificate (trusted backend only)
```sh
curl -X POST http://127.0.0.1:8792/certificates \
 -H 'Authorization: Bearer choose-a-long-random-secret' \
 -H 'Content-Type: application/json' \
 -d '{"id":"123e4567-e89b-12d3-a456-426614174000","name":"Peserta Contoh","course":"Admin Warehouse - Basic","courseId":"role_admin_warehouse_basic","date":"2026-11-02","score":90}'
```

## Verify
```sh
curl 'http://127.0.0.1:8792/verify?id=123e4567-e89b-12d3-a456-426614174000'
```

**Important:** The Android app currently stores completion in local SharedPreferences. A public registry **cannot securely trust a client-generated certificate**. Before public launch, move final exam grading, course completion validation, and certificate issuance to an authenticated backend; register only server-verified completions. Never put the admin token in Android. Current QR contains offline identity fields, not an HTTPS verification URL; the QR format must be updated after the public HTTPS verification domain is configured. No live verification site has been deployed.
