# National directory, Auto Apply and interview integration

This branch is stacked on `fix/karirmate-restoration`, not a replacement for its pending review. No production state, cron, credential or APK is modified. Commands below are instructions for a later operator review, not actions executed against Termux.

## Owner workbook and non-destructive import

Required original input (not present in GitHub, the source ZIP, or this workspace):
`~/automate-jobs-v2/perusahaan_jabodetabek_gabungan.xlsx`.
The reported 135 companies / 142 unique emails are historical, not audited here.

From the backend repository, with Python dependencies installed, import to a **separate** directory DB. Replace the account ID with the already linked, verified Telegram candidate ID; do not invent an ID. Keep this workbook and DB outside Git.

```bash
python tools/import_company_contacts.py \
  "$HOME/automate-jobs-v2/perusahaan_jabodetabek_gabungan.xlsx" \
  --db "$HOME/automate-ai/data/company_directory.sqlite3" \
  --owner-uid ACTUAL_LINKED_TELEGRAM_ID \
  --backup-dir "$HOME/karirmate-backups/company-directory"
```

The placeholder ID must be replaced before running. The program requires a positive integer. Set `KARIRMATE_COMPANY_DB` to that same DB path when configuring the later runtime. No current environment file is edited by this change.

Supported: XLSX without executing formulas/macros; CSV UTF-8/BOM with comma/semicolon/tab; column aliases company/perusahaan/nama perusahaan, email/email HR/email 1/email 2, city/kota, province/provinsi, industry/industri, position/posisi, source/sumber/website. Multiple literal addresses per cell are split. Filename/sheet/row and file SHA-256 provide provenance when the workbook has no web source. Missing location is preserved and blocks location-specific Auto Apply. Existing application tables are read, not rewritten; pre-existing directory DB is backed up before additive imports. Review invalid rows and reconcile actual import counts before using the old 135/142 figures.

GET `/companies` supports province, city, industry, published position, offset (50 per page). `/companies/provinces` and `/companies/stats` are read-only authenticated routes. Owner rows are visible only through that Android account's saved Telegram mapping. Public national search never changes the user's application region preference. A published general recruitment contact has no invented job title; spontaneous applications take an actual matching role from the user's profile.

## Sources and periodic review

`data/national_company_contacts.json` contains public-source evidence, literal email excerpts, SHA-256 and check timestamps. Its statistics describe this development snapshot, not production or owner workbook counts. A contact is not proof of an active vacancy. Only explicitly application-accepting official contacts, or existing owner contacts, can enter spontaneous selection. Informational contacts and school-reference contacts awaiting company verification are searchable but not selected for Auto Apply.

```bash
python tools/refresh_company_directory.py \
  --db "$HOME/automate-ai/data/company_directory.sqlite3" \
  --output /tmp/karirmate-source-review.json
```

Dry-run is the default. `--force` reviews before the configured due date. A reviewed operator may later add `--apply --backup-dir PATH` to store the review, after checking the output. Changed/unavailable pages require manual review and lose automatic selection permission; no contacts or old evidence are deleted. A source refresh never discovers and enables new emails automatically. Only unchanged evidence can refresh the timestamp while retaining previous reviewed permission. Review interval (30 days), public evidence expiry (90 days), and company/position duplicate period (90 days) are reviewable in `auto_apply_policy.json`. These are engineering defaults, not a permanent product decision. No cron entry is created or modified.

`tools/company_research/verify_public_pages.py` accepts explicit candidate URLs and produces research evidence, never application queues. It enforces public HTTPS, DNS/private-address checks, no credentials, no redirects, text content and a 3 MB cap. Check site terms and human-review purpose/location before adding a source. Research results contain no invented address patterns. Revalidation is necessary because publication does not establish mailbox deliverability or vacancy status.

## Sending and integration boundaries

The existing authenticated Android API routes delegate directory/interview modules before legacy routes; bottom navigation is retained. Directory DB and interview DB are separate new files, not replacements for the personal application DB. SQLite queue migrations only add fields. Source policy and freshness are checked again at the final reservation, together with actual CV/profile location, consent, PRO, quota and duplicate claims.

Active verified vacancies precede spontaneous owner contacts, then reviewed public contacts. The sender uses truthful Indonesian spontaneous content and never describes an unverified company as hiring. The five/day limit counts reservations and uncertain delivery in WIB under `BEGIN IMMEDIATE`. Existing lifetime recipient claims remain more conservative than the reviewable company/position window; uncertain deliveries are never automatically retried.

`POST /auto-apply/test-email-preview` prepares one message only: actual backend CV fingerprint, actual profile name, exact proposed recipient/subject/body and review ID. There is **no send action** in that endpoint or Android preview. No recipient or content has been approved in this session. After approval, a separate, reviewed operation must use the authorized Gmail worker and compare the exact preview/CV fingerprint. Real Gmail send, token refresh and spontaneous-content handling cannot be certified without that external worker source/service and the user's approved test.

Internal `sent` requires Gmail message ID and Android labels it “Diterima Gmail API”; it is not evidence of delivery to, or reading by, HR. Reports retain prepared, queued, processing, confirmed, failed, uncertain, cancelled and configuration-blocked states. Operator block and unverified scheduling remain visible. No environment switch is enabled, no cron is edited.

The account-specific cause of “Kandidat Tidak Tersedia” remains unverified without that account's runtime data. Code-level causes previously collapsed identity/profile/CV/recipient failures into an empty list; the diagnostic API and Android preserve exact codes and actions. This branch additionally reports directory totals, missing location and unreviewed email purpose. No fake candidate or bypass is introduced.

## Interview runtime

Text mode works on CPU using explicit structured questions and scoring when no model is configured. This is labeled `structured_rules` / `structured_text_rules`, not represented as an AI model result. Both languages, role, difficulty and interviewer persona affect the questions. PRO allows adaptive questions, deeper per-answer feedback and stored progression. Scoring only uses answer text, job relevance and factual CV context; no camera/appearance/protected-attribute assessment. English checks are basic text patterns, not pronunciation or a validated proficiency assessment.

Optional real Ollama `/api/chat` adapter uses the operator's existing self-hosted model, never downloads a model or calls a paid API. Configure `KARIRMATE_OLLAMA_URL` and `KARIRMATE_INTERVIEW_MODEL` outside Git only after review. Choose model/weights with compatible usage rights. CV contacts are omitted from structured CV context; backend PDF input receives basic email/phone masking, which is not complete PII anonymisation. CV/answers are private stored account data; set filesystem permissions/backups and retention through a reviewed deployment plan.

Separate optional STT `/v1/audio/transcriptions` (multipart WAV/M4A) and TTS `/synthesize` (JSON text/language → WAV) adapters require health `/health` returning `ready: true` or `status: ok/ready`. Configure `KARIRMATE_STT_URL`/`KARIRMATE_TTS_URL` outside Git. No voice service implementation/model is bundled; these protocols must be supplied by compatible self-hosted gateways. Android prefers on-device speech recognition (API31+) and installed TTS; otherwise uses the configured adapters. Microphone permission is explicit. Transcript remains editable before submission; failed extraction never becomes a bad score. Physical-device voice is not tested here.

Session IDs and access always use authenticated Android identity. PRO is read from existing membership policy, not the request body. Answers use optimistic revisions and idempotency IDs; sync acknowledges stored request IDs without double-advancing questions. Reopening history is account-scoped; no fake history. GPU failure leaves text mode available.

## Dependencies still missing from supplied production snapshot

Existing auth/PRO runtime: `android_auth.py`, `account_access_policy.py`, `karirmate_integrasi_final.py`, `search_identity.py`; existing psychotest banks: `psychotest_bank.py`, `psychotest_skb.py`. The Telegram/job application also references `application_methods`, `company_catalog`, `company_search`, `job_catalog`, `job_validation`, `live_sources`, `search_engine`, `idjobs_feed`, `github_search_engine`, `kalibrr_client`, `local_jobs`, `phase4_watch`, `storage`, and a sanitized `config` interface. Existing external Gmail `/api/gmail/send-application` worker source, refresh/error contract and spontaneous-body support are not provided. Do not upload live tokens, private databases or the owner workbook into GitHub to resolve this.

Live account/PRO, Gmail OAuth, owner import totals, deployment, hardware voice, real model output and actual WebRTC rendering remain blocked or untested until the corresponding sources/services/devices and approvals are available.
