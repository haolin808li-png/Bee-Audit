# Bee workbench front-end milestone

Scope: build the desktop browser experience first. No cloud implementation or deployment. No Android changes needed; use existing Export ZIP.

- [x] Inspect Android ZIP and JSON contracts.
- [x] Build visits workspace, hive details, media and report views.
- [x] Add local ZIP import with explicit demo/local separation.
- [x] Add browser-local persistence and draft notes.
- [x] Add photo preview, native audio playback and template report export.
- [x] Verify import edge cases and responsive layouts.
- [x] Build production bundle and present local preview.

## Verification (2026-10-25)

- Eight import/data tests pass: actual media association, media without forms, mixed visits, malformed JSON, invalid dates, traversal paths, unsupported exports and HTML escaping.
- Chromium: real ZIP import, image rendering/navigation, native WAV audio playback, IndexedDB persistence after reload, duplicate/invalid ZIP feedback, draft notes persistence and Markdown download pass.
- Desktop (1440 px) and mobile (390 px) screenshots inspected. Visits, reports, imports and settings have no document-level horizontal overflow on mobile.
- WebKit: visits and report rendering pass. This is engine coverage, not a full test on physical Windows/macOS machines; Android M4A codec playback still needs a real exported sample.
- Production build passes. Development server: http://127.0.0.1:4173/.
- No Android files changed; no cloud service or model API called.

Screenshots: `workbench/test-results/desktop.png`, `mobile.png`, `report.png`. Run instructions and limitations: `workbench/README.md`.

Later milestones: cloud API, authenticated uploads, backup/recovery, task queue, transcription and model orchestration; then minimal Android upload entry and status. These are not part of this front-end milestone.
