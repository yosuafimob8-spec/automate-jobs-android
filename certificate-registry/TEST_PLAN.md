# KarirMate — acceptance checks (no APK build)

## Backend automated checks
Run `cd certificate-registry && python3 -m unittest -v test_server.py` in a Python 3 environment. Expect all tests to pass. Tests cover registry validation, duplicate IDs, authenticated creation, public API, public HTML page, unauthorized access, and revocation. Check the GitHub Actions **Training Unit Tests Only** run on this feature branch; do not infer a pass from workflow configuration alone.

## Android automated checks
Run `./gradlew :app:testDebugUnitTest --stacktrace`. This compiles the debug test target but **does not run assembleDebug or produce an APK**. Check `app/build/reports/tests/` for actual pass/fail results. Tests include 78 roles × 3 levels, practice assessment, quiz scoring, certificate identity, and HTTPS URL format.

## Android device acceptance (requires a runnable installed app; deferred by no-build constraint)
1. Select each career family, select a role, open Basic; verify Intermediate and Advanced remain locked.
2. Open all five lessons in order; confirm visual slides contain actual lesson text and navigation works.
3. Enter insufficient practice response; verify rejected. Enter a substantive response; verify next lesson unlocks.
4. Attempt final exam before modules complete; verify blocked. Complete modules; verify five questions appear.
5. Answer incorrectly; verify no certificate issued below 80. Answer correctly; verify certificate issued once and stored in history.
6. Export PDF, inspect text, name, score, date, signatories, QR readability, and PDF file integrity on device.
7. Paste offline QR payload into local history checker; verify local match and mismatch; confirm no claim of public authenticity.
8. Close and reopen app; verify progress and history persistence. Test revoked, unknown and duplicate IDs on server.

## Critical trust boundary
The current Android app evaluates and stores completions locally. A modified client can forge that state. The registry **must not automatically trust it**. Public issuance requires authenticated server-side exam grading, module completion tracking, and a trusted user identity before enabling any online-verified QR in production. A registry admin token must never be embedded in an APK.

## Launch blocker
Public HTTPS domain/hosting and trusted issuance are not configured. Public verification QR should remain disabled until the backend controls are implemented and deployment has been tested. This test plan is not evidence that tests have passed.
