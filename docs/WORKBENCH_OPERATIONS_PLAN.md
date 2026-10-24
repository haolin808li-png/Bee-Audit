# Operations workspace milestone / 2026-10-25

Scope: English desktop frontend for evidence management, review status and Agent operations. Local-only implementation. No deployment, paid APIs or Android changes.

## Completed

- [x] Replace Chinese UI, demo records, importer errors and generated template text with English.
- [x] Preserve ZIP import, IndexedDB persistence, photo navigation and audio playback.
- [x] Overview: visits, hives, pending reviews, active simulations and evidence coverage.
- [x] Visit assignment and persisted review statuses.
- [x] Agent run history, selection, progress, logs and routing snapshots.
- [x] Pause, resume, cancel, duplicate-active-run protection and retry.
- [x] Settings-controlled provider failure simulation.
- [x] Human-review gate before marking a visit reviewed.
- [x] English evidence-based template reports, notes, Markdown and browser PDF export.
- [x] Analytics scope filter, coverage bars, visit comparisons, GPS listings and CSV export.
- [x] Local personnel directory, assignments and protection against deleting assigned entries.
- [x] Bounded local activity log and JSON export.
- [x] Hash navigation and help.
- [x] Responsive layout and keyboard/native-control support.
- [x] English README with explicit production gaps.

## Verification

- 15 unit tests: ZIP safety and associations, English reports, workflow transitions, cancellation, retry, persistence representation and analytics counts.
- Production build succeeds.
- Chromium: existing import/media/report tests pass, including WAV playback and source HTML escaping.
- WebKit: existing visit/report rendering passes.
- Chromium and WebKit: full operations flows pass, including assignment persistence, pause after reload, simulated failure/retry, cancellation, review acknowledgement and CSV/JSON downloads.
- Both engines: all nine pages checked at 1440, 768, 390 and 360 px, no document-level horizontal overflow; no Chinese demo/interface text and no external service requests in tested flows.
- Desktop overview and mobile Agent screenshots visually inspected. Screenshots saved under workbench/test-results.

## Explicitly not deployed or implemented

- Backend Agent worker, queue, real transcription or model inference.
- Cloud files, authentication, authorization, multi-user sync and resilient backup.
- Actual invitation emails, immutable audit trail and signed reports.
- Embedded map tiles, historical colony-health conclusions and bulk source editing/deletion.
- Physical Windows/macOS/Android verification and real M4A sample compatibility remain separate acceptance checks.

## Future backend handoff

Keep the browser views but replace simulation transitions with server-owned job states. Store API credentials only on the backend. Model output must cite source records and media, preserve unknowns, and stay a draft until a human reviews it. Validate archives again on the server; local checks are not a security boundary. Use server-enforced accounts and permissions before exposing any real inspection evidence outside localhost.
