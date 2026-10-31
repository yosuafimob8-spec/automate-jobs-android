# KarirMate restoration — Android review

Source is based on GitHub main `2d751b6ec584e3706f758826ce22fe3868f9dc3c`. Backend changes belong to the matching `fix/karirmate-restoration` branch in automate-jobs-backend. No APK or instrumented-test APK was built or installed.

## Changes

- Auto Apply displays structured server diagnosis, operational blockers, actual delivery counts, quota and a read-only planning simulation. The old static history page now loads paginated account-scoped reports with date filters, delivery details, Gmail message IDs and errors. Older servers without summaries are reported as incompatible instead of displaying inferred zero totals.
- CV uses a nine-step journey with a fixed Save & Continue action, progress, input validation, automatic draft saving and a resume checkpoint per document. Existing section editing, header/system back history, photo import, 14 designs, shared A4 preview/PDF rendering, letters and DOCX remain available.
- Product policy is in `app/src/main/assets/product_policy.json`: four ATS-compatible designs FREE, ten PRO, all eight learning fields FREE/PRO. The actual catalog had only two ATS labels. Existing single-column photo-free `minimal_01` and `minimal_02` are additionally classified as ATS by configuration; their IDs, original Minimalis category and designs are unchanged. No template was added, replaced or removed.
- CV Analyzer uses real text from either the active CV or a selected PDF. Inline section headings are recognized. Empty/scanned/unreadable PDFs produce extraction errors with no fabricated score. ATS checks are rule-based completeness checks, not a hiring guarantee. OCR and AI letter generation remain unavailable and are described honestly.
- Psychotest completion controls stay visible outside the question scroll area, including on small screens. Incomplete answers require confirmation. Score, motivation, category analysis and PRO explanations return; full sessions use the server's configured length, short training stays 10–20 questions. Server-verified access is also enforced by the backend response filter.
- Eight learning fields have a configurable two-module initial curriculum, per-module source metadata, persistent exercises/progress, job-case simulation, final exams, attempts, internal PDF certificates and optional inclusion in the active CV. Existing courses and certificate preferences are preserved under the legacy entry. The 80/100 passing threshold is configurable and reviewable, not a permanent product decision.
- Dashboard follows light/dark theme, uses clearer cards, hierarchy and responsive feature grids. Its stale unavailable-history message is replaced by the real report route. Existing bottom navigation is retained.
- Salary validation assigns errors to individual fields and preserves text. Overtime and THR bases are separate. Existing arithmetic is retained, with PP 35/2021 rest-day hour bounds validated. Sources and annual-tax-estimate assumptions are visible; this is not a monthly TER payroll implementation.

## Verification

Local verification ran `:app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:compileDebugAndroidTestKotlin`. Final results: 91 tests passed, zero failures; lint zero errors and 71 warnings. Python backend-contract suite: 13 tests passed. These include real PDF extraction/rendering with PDFBox, persisted CV/photo/learning state, Compose navigation and completion controls, contract HTTP tests against MockWebServer, and scoring/access tests.

Runtime: SDK 35, Robolectric 4.16.1 with native graphics, Java 21 test runtime. The managed environment uses a local Gradle init script for writable test home and dependency proxy settings; that environment-only file is not part of the application or this patch.

A supplemental attempt to capture Compose frames timed out in Robolectric's WindowCapture redraw. Capture-only calls were removed, behavioral UI assertions retained and the full suite rerun successfully. Real-device visual inspection, picker permissions across Android versions, PDF viewer interoperability, production login/Gmail and complete bank integration remain NOT TESTED. Instrumented test sources were compiled only.

Before rollout, review the matching backend PR and provide missing identity/membership/bank/worker sources without credentials or user databases. Keep operator live sending disabled and existing cron unchanged. Build/release remains on hold for user review and approval.
