# Dashboard and Session Progress

Updated: 2026-10-29
Status: Build, lint, and 35 unit tests pass. Offline session retention is runtime-verified on `emulator-5554`. Online backend flows remain unverified because the local backend is unavailable.

## Current behavior

### Dashboard

- Shows a compact `Online`/`Offline` pill based on a successful `/health` response with `status = online`.
- Removes the old backend status banner and endpoint/health terminology.
- Shows the server display name when available, otherwise `SmartBot User`.
- Shows profile and email status as unknown rather than inventing values.
- Uses server-derived `FREE`/`PRO` state when `is_pro` or `membership` is returned.
- Keeps job count empty when the jobs request fails.
- Keeps the nine feature cards and five bottom-nav tabs.
- Activity and unsupported features remain explicitly unavailable.

### Navigation and session

- Valid stored token: start at Dashboard, even offline.
- Missing or expired token: start at Login.
- Startup displays a short branded validation splash.
- Authenticated `401`: clear the local session and return to Login.
- Logout: clear the token and local session state, then return to Login.
- Expiry is parsed from ISO-8601, offset, local date-time, epoch seconds, or epoch milliseconds.
- Missing/malformed expiry uses a 24-hour fallback.
- Background resume checks local token validity.
- Onboarding is entered only when the server explicitly reports an incomplete profile.
- Unknown/offline profile checks keep the user on the Dashboard instead of forcing onboarding.
- Login routes to Onboarding only for `profileCompleted == false`.

### Profile and CV

- Profile shows offline wording and a retry action instead of raw connection errors.
- Profile save is disabled offline.
- Profile and onboarding include a local CV section.
- CV rules: PDF only, `%PDF-` header, maximum 2 MB.
- The selected URI, display name, size, and read permission are persisted locally.
- Replace and delete are supported.
- The CV is never uploaded, copied into app storage, or sent automatically.
- `CvRepository`/`CvViewModel` are prepared for upload, status, replace, and delete, but no remote endpoint is bound until the contract in `CV_BACKEND_PLAN.md` is implemented and verified.

### Kirim Lamaran

- Uses the real server-derived `isPro` value.
- FREE users see the PRO requirement.
- The send action is disabled and labeled `Pengiriman Belum Tersedia`.
- Offline users see a connection notice.
- No fake email credentials, logs, delivery status, or success state.

## Verification

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

- Build: `BUILD SUCCESSFUL`
- Unit tests: 35 passed, 0 failures, 0 errors
- Lint: `BUILD SUCCESSFUL`, 0 errors
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `66F56C17039ED3F942AA32B34454C37B60530222CEB95EC6975708420B2B0060`
- Size: 18,807,606 bytes
- Runtime checks used the previously installed APK; the latest build was not reinstalled because the CV upload layer is UI-unbound

Runtime checks:

- Existing session survived reinstall and app restart.
- App opened the Dashboard while the backend was offline.
- Dashboard showed the `Offline` pill.
- Kirim Lamaran showed the FREE gate, disabled send action, and offline notice.
- Profile showed the offline state and retry action.

Not verified:

- Online health and membership response parsing.
- Live profile load/save.
- CV picker and permission persistence on a live profile.
- Server-issued 401 expiry.
- Login/register/onboarding end-to-end.
- Physical-device behavior.

## Remaining work

1. Confirm backend `/health`, `/auth/me`, and `/profile` response contracts.
2. Re-test FREE/PRO UI against real membership data.
3. Exercise CV picker, replace, delete, and permission persistence with a real PDF.
4. Test login, logout, profile completion, and 401 expiry against the live backend.
5. Add Compose instrumentation tests for session restore, onboarding gating, and CV selection.
6. Implement and verify the authenticated CV contract in `CV_BACKEND_PLAN.md`, then bind `CvRemoteStore`.
7. Confirm backend support before enabling email sending, history, AI features, psychotests, or document templates.

See `DEVELOPMENT_REPORT.md` for the full implementation and verification report.
