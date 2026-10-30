# CV Upload Backend Integration Plan

Updated: 2026-10-29
Status: Android upload layers prepared. No backend endpoint exists or was verified. No fake endpoint or fake upload success was added.

## Investigation findings

### Android

- `app/src/main/java/com/example/automatesmartbot/data/network/ApiService.kt` exposes only health, auth, profile, jobs, apply/email, activities, and legacy profile calls.
- There is no `@Multipart`, `MultipartBody`, CV, resume, document, or upload endpoint in the current Retrofit interface.
- `CvSection.kt` currently uses `OpenDocument` and stores the selected PDF locally.
- `CvSelectionStore.kt` persists the content URI, display name, size, and a read permission. It does not copy or expose the file.
- `CvValidator.kt` enforces PDF extension, MIME, `%PDF-` header, and a 2 MB maximum.
- `SessionManager` stores the access token in encrypted preferences; `AuthInterceptor` attaches it as `Authorization: Bearer <token>` and expires the session on authenticated `401`.
- `CvRepository`, `CvRemoteStore`, and `CvViewModel` now provide upload, remote-status, replace, and delete boundaries, but no remote implementation is bound because there is no confirmed API.

### Backend

- No Android backend source is present in the workspace.
- The local API at `http://127.0.0.1:8787/` refused connections during this task, so `/openapi.json` could not be inspected.
- The configured release host `api.automatesmartbot.com` did not resolve during this task.
- The available Cloudflare worker snapshot under `Downloads/automate-phase512-production/worker.production.js` contains Gmail/OAuth routes only (`/api/oauth/start`, `/api/oauth/status`, `/api/gmail/test-send`, `/api/gmail/test-pdf`, `/api/gmail/send-application`). It has no CV upload route.
- Conclusion: authenticated CV upload is not confirmed to exist. The Android app must not call an invented endpoint.

## Proposed API contract

These endpoints are a proposal only. They are not implemented and must not be called until the backend implements and verifies them.

All endpoints require:

```http
Authorization: Bearer <access_token>
```

All responses must be scoped to the authenticated user. The server must not trust a user ID supplied by the client.

### 1. Get CV status

```http
GET /cv
```

Success when a CV exists:

```json
{
  "id": "cv_01J...",
  "file_name": "cv-joko.pdf",
  "size_bytes": 524288,
  "content_type": "application/pdf",
  "uploaded_at": "2026-10-29T10:00:00Z",
  "sha256": "hex-encoded-sha256"
}
```

Response codes:

- `200 OK`: CV metadata returned.
- `404 Not Found`: authenticated user has no CV.
- `401 Unauthorized`: missing, invalid, or expired token.

### 2. Upload or replace CV

```http
PUT /cv
Content-Type: multipart/form-data; boundary=...
Authorization: Bearer <access_token>
```

Multipart field:

- `file`: exactly one PDF file.
- Content type: `application/pdf`.
- Maximum size: `2,097,152` bytes.
- The server must verify the `%PDF-` header, not only the filename or client MIME type.

Success:

```http
200 OK
```

```json
{
  "id": "cv_01J...",
  "file_name": "cv-joko.pdf",
  "size_bytes": 524288,
  "content_type": "application/pdf",
  "uploaded_at": "2026-10-29T10:00:00Z",
  "sha256": "hex-encoded-sha256"
}
```

`PUT` provides create-or-replace semantics, so Android can use the same operation for first upload and replacement.

Response codes:

- `200 OK`: CV created or replaced.
- `400 Bad Request`: missing file or malformed multipart request.
- `401 Unauthorized`: missing, invalid, or expired token.
- `413 Payload Too Large`: file exceeds 2 MB.
- `415 Unsupported Media Type`: not a PDF.
- `422 Unprocessable Entity`: invalid PDF signature/content.
- `429 Too Many Requests`: upload rate limit exceeded.
- `500 Internal Server Error`: storage/database failure without exposing internal paths.

Error shape should not leak storage keys:

```json
{
  "detail": "CV harus berupa file PDF maksimal 2 MB."
}
```

### 3. Delete CV

```http
DELETE /cv
```

Response codes:

- `204 No Content`: CV deleted.
- `204 No Content` when no CV exists: keep the operation idempotent.
- `401 Unauthorized`: missing, invalid, or expired token.
- `500 Internal Server Error`: deletion failed.

### Optional authenticated download

Do not add this until it is needed by a verified flow:

```http
GET /cv/content
Authorization: Bearer <access_token>
```

- Returns `application/pdf` with `Content-Disposition: attachment`.
- Must require authentication and ownership.
- Must not return a public bucket URL, permanent URL, or a path controlled by the client.

## Server-side requirements

1. Authenticate every request through the existing token mechanism.
2. Resolve the user ID from the authenticated session; ignore client-supplied ownership fields.
3. Enforce the 2 MB limit at the HTTP layer and again while streaming.
4. Validate PDF magic bytes and reject mismatched content.
5. Store files outside the web root with a random opaque key.
6. Keep the original filename only as metadata; never use it as a path.
7. Store one active CV per user, replacing the previous record atomically.
8. Delete the old object only after the new upload and database update succeed.
9. Delete both metadata and stored object on `DELETE /cv`.
10. Do not expose storage paths, bucket names, or signed public URLs in responses.
11. Rate-limit uploads and log failures without logging file contents or tokens.
12. Never send email automatically from the upload endpoint.
13. Consider encryption at rest and malware scanning before enabling production uploads.

Suggested metadata table:

```sql
CREATE TABLE user_cv (
    user_id        TEXT PRIMARY KEY,
    storage_key    TEXT NOT NULL,
    file_name      TEXT NOT NULL,
    size_bytes     INTEGER NOT NULL,
    content_type   TEXT NOT NULL,
    sha256         TEXT NOT NULL,
    uploaded_at    TEXT NOT NULL
);
```

## Android activation steps after backend deployment

1. Obtain the real base URL and confirm `/health`.
2. Confirm `GET /cv`, `PUT /cv`, and `DELETE /cv` through authenticated requests.
3. Implement `CvRemoteStore` with Retrofit/OkHttp using the confirmed contract.
4. Build the multipart request from the selected content URI without copying the PDF into public storage.
5. Keep `CvValidator` checks on the client; the server must repeat them.
6. Pass the implemented `CvRemoteStore` into `CvRepository` and expose it to `CvViewModel`.
7. Map `401` through the existing `AuthInterceptor` and session-expiry flow.
8. Show upload status, replace, and delete only after real responses are available.
9. Test PDF, oversized file, wrong type, expired token, replace, and delete against the deployed backend.
10. Do not enable email sending in this phase.

## Android work completed

- `data/cv/CvRemoteStore.kt`: remote-store contract and result types, with no endpoint implementation.
- `data/cv/CvRepository.kt`: local validation, upload, status, replace, remote delete, local delete, and authenticated-session guard.
- `ui/viewmodel/CvViewModel.kt`: state for local selection, server status, upload, replace, and delete.
- Existing picker, local persistence, PDF validation, and 2 MB limit remain intact.
- Existing UI and navigation were not changed.

## Verification

- `.\gradlew.bat :app:assembleDebug` completed successfully after adding the upload layers.
- `.\gradlew.bat :app:testDebugUnitTest` completed with 35 tests, 0 failures, 0 errors.
- `.\gradlew.bat :app:lintDebug` completed with 0 errors and 33 warnings.
- No upload was attempted and no upload success was reported because no verified endpoint exists.

## Dependency

CV upload is blocked on the backend implementing and confirming the proposed endpoints, authentication behavior, private storage, and 2 MB enforcement. Until then, the app must continue to describe the CV as stored on the device only.
