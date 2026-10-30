# AutoMateSmartBot Development Report

Updated: 2026-10-29
Status: Implemented, built, unit-tested, linted, and partially runtime-verified on `emulator-5554`.

## Scope

This work focused on six areas:

1. Remove developer-facing wording from user screens.
2. Add local-only PDF CV selection to Profile and onboarding.
3. Improve the Kirim Lamaran screen without pretending email sending works.
4. Replace the dashboard backend banner with a compact Online/Offline pill.
5. Harden startup, login persistence, logout, session expiry, and offline behavior.
6. Verify the result with unit tests, a debug build, lint, and safe emulator checks.

No backend, Termux, SSH, credential, production service, deployment, or publishing changes were made.

## Delivered

### 1. User-facing copy cleanup

- Dashboard no longer exposes backend, endpoint, or health-check terminology.
- Replaced the dashboard banner with a compact `Online`/`Offline` pill.
- Riwayat, Cari Lowongan, feature screens, and Kirim Lamaran use user-facing Indonesian copy.
- Removed stale references to `databaseConnected` and backend status banners.
- Feature availability is represented as `Belum tersedia` or `Segera Hadir`; no fabricated results are shown.

### 2. Local-only CV selection

- Added `CvValidator`:
  - Requires `.pdf`.
  - Accepts `application/pdf`, `application/x-pdf`, or generic octet-stream MIME.
  - Checks the `%PDF-` header.
  - Enforces a 2 MB maximum.
- Added `CvSelectionStore`:
  - Uses `OpenDocument` for the picker.
  - Persists the content URI and a read permission.
  - Stores only URI, display name, and size in local preferences.
  - Supplies replace and remove operations.
- Added reusable `CvSection` with file name, formatted size, replace, and delete controls.
- Integrated the CV section into `ProfilScreen` and `OnboardingProfileScreen`.
- The selected CV is not uploaded, copied into app storage, or sent automatically.
- Added `CvRemoteStore`, `CvRepository`, and `CvViewModel` boundaries for future upload, status, replace, and delete operations.
- No remote CV endpoint is bound because no authenticated backend contract exists; see `CV_BACKEND_PLAN.md`.

### 3. Kirim Lamaran

- Uses the real server-derived `isPro` value.
- FREE users see a PRO requirement and a link to check status in Profile.
- The send button remains disabled and explicitly says `Pengiriman Belum Tersedia`.
- Offline users see a connection notice.
- Removed fake credentials, logs, delivery status, and success states.
- The legacy `KirimLamaranEmailScreen` delegates to the honest screen.

### 4. Dashboard status

- `DashboardRepository` reads `/health` and reports online only when the response is successful and `status` is `online`.
- Failure, malformed response, or a non-online status produces an offline state.
- The dashboard shows a compact `Online`/`Offline` pill in the welcome card.
- Account stats remain honest when data is unavailable; unknown values are shown as unknown or `-`.
- Membership uses `is_pro` or `membership` when the server returns those fields. The current backend contract could not be verified while it was offline.

### 5. Session and navigation

- Added `SessionPolicy` for ISO-8601, offset, local date-time, epoch-second, and epoch-millisecond expiry values.
- If expiry is missing or malformed, a 24-hour fallback is stored. The app does not claim indefinite or refresh-token support.
- Added a startup splash while the stored session is validated.
- A valid stored token opens the Dashboard even when the backend is offline.
- A missing, expired, or cleared token opens Login.
- `AuthInterceptor` expires the local session on an authenticated `401`.
- Session expiry and explicit logout clear the token and local session state and return to Login.
- ON_RESUME revalidation ends a session that expired while the app was backgrounded.
- The onboarding gate redirects only when the server explicitly reports an incomplete profile. An offline/unknown result keeps the current session on the Dashboard.
- Login routes to onboarding or Dashboard based on the server profile check, never by inventing a completed profile.
- Removed the unused legacy onboarding repository/view-model that could mark a profile complete locally without a server confirmation.

### 6. Offline behavior

- Profile load failures show `Anda sedang offline` when the dashboard state is offline.
- Profile save is disabled offline and says an internet connection is required.
- The app keeps the valid session while offline and offers retry.
- No offline mode fabricates profile, membership, history, job, or email data.

## Verification

### Build and tests

Command:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Result: `BUILD SUCCESSFUL`

Unit tests: 35 passed, 0 failures, 0 errors.

| Suite | Tests |
| --- | ---: |
| `JobFiltersTest` | 4 |
| `ProfileSavePolicyTest` | 4 |
| `SessionPolicyTest` | 7 |
| `ExampleUnitTest` | 1 |
| `NavigationPolicyTest` | 7 |
| `CvValidatorTest` | 9 |
| `UrlSafetyTest` | 3 |

Lint: `BUILD SUCCESSFUL`, 0 errors. Remaining warnings are dependency/target-SDK upgrade notices, KTX suggestions, unused template colors, the intentional debug cleartext configuration, and deprecation notices for `EncryptedSharedPreferences`/`MasterKey`.

Debug artifact:

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Size: 18,807,606 bytes
- SHA-256: `66F56C17039ED3F942AA32B34454C37B60530222CEB95EC6975708420B2B0060`
- Runtime checks used the previously installed APK; the latest CV backend-readiness build was not reinstalled because its upload layer is not wired into the UI.

### Runtime checks

The local backend remained unavailable at `http://127.0.0.1:8787/`, so authenticated online checks could not be repeated.

Verified on the emulator:

- The app relaunched with its existing session and opened the Dashboard instead of Login.
- The Dashboard showed the compact `Offline` pill.
- The Dashboard did not use the old backend banner wording.
- Kirim Lamaran showed the FREE gate, `Pengiriman Belum Tersedia`, the offline notice, and the PDF/2 MB preparation text.
- Profile showed the offline state with retry instead of exposing raw connection errors.

Not runtime-verified because the backend was offline:

- Online health/membership response shape.
- Fresh profile load and profile save.
- CV picker, replace, delete, and permission persistence on a live profile.
- Central 401 expiry against a real server response.
- Login, register, and onboarding end-to-end.

## Known limitations

- The backend is currently unavailable; `/auth/me` membership fields could not be confirmed.
- There is no verified CV upload/storage endpoint, so the CV remains local-only. The Android upload repository and ViewModel are prepared but intentionally unbound.
- Email sending has no verified authorization or endpoint and remains disabled.
- `Riwayat`, AI features, psychotests, document templates, and CV generation remain unavailable.
- No physical device was attached for final validation.
- No Compose instrumentation tests cover the file picker or session restore flow.

## Recommended next steps

1. Start the verified backend and confirm `/health`, `/auth/me`, and `/profile` response shapes.
2. Implement and verify the authenticated CV contract proposed in `CV_BACKEND_PLAN.md` before binding the Android upload layer.
3. Confirm whether `/auth/me` returns `is_pro` or `membership`, then re-test the FREE/PRO UI.
4. Exercise CV picker, replace, delete, and permission persistence with a real PDF on the emulator.
5. Test login, profile completion, 401 expiry, and logout against the live backend.
6. Add Compose instrumentation tests for session restore and the CV picker flow.

## Main changed files

### New

- `app/src/main/java/com/example/automatesmartbot/data/cv/CvSelectionStore.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvRemoteStore.kt`
- `app/src/main/java/com/example/automatesmartbot/data/cv/CvRepository.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/viewmodel/CvViewModel.kt`
- `app/src/main/java/com/example/automatesmartbot/data/security/SessionPolicy.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/components/CvSection.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/navigation/NavigationPolicy.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/StartupSplashScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/util/CvValidator.kt`
- `app/src/test/java/com/example/automatesmartbot/data/security/SessionPolicyTest.kt`
- `app/src/test/java/com/example/automatesmartbot/ui/navigation/NavigationPolicyTest.kt`
- `app/src/test/java/com/example/automatesmartbot/util/CvValidatorTest.kt`
- `app/src/test/java/com/example/automatesmartbot/data/repository/ProfileSavePolicyTest.kt`

### Updated

- `app/src/main/java/com/example/automatesmartbot/data/security/SessionManager.kt`
- `app/src/main/java/com/example/automatesmartbot/data/network/AuthInterceptor.kt`
- `app/src/main/java/com/example/automatesmartbot/data/model/Models.kt`
- `app/src/main/java/com/example/automatesmartbot/data/model/AuthModels.kt`
- `app/src/main/java/com/example/automatesmartbot/data/repository/Repositories.kt`
- `app/src/main/java/com/example/automatesmartbot/data/repository/ProfileSetupRepository.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/MainDashboardScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/DashboardScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/ProfilScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/OnboardingProfileScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/KirimLamaranScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/KirimLamaranEmailScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/RiwayatScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/CariLowonganScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/screens/FeatureComingSoonScreen.kt`
- `app/src/main/java/com/example/automatesmartbot/ui/viewmodel/AuthViewModel.kt`
- `CV_BACKEND_PLAN.md`
