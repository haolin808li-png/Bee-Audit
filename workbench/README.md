# Bee Audit Operations & Agent Workspace

> **PUBLIC TEST PREVIEW — NOT FOR OPERATIONAL USE.** Use synthetic or
> de-identified test records only. Read the root [privacy notice](../PRIVACY.md)
> and [test disclaimer](../DISCLAIMER.md). This is not an operational audit system.

English-language, browser-only front end. Android code is unchanged. No cloud, account, upload, backup or AI API is implemented in this version. Nothing is deployed. Agent execution is an explicitly labelled local simulator, not a working AI backend.

## Run

Use Node.js 20.19+ or 22+.

```sh
cd workbench
npm ci
npm run dev -- --port 4173
```

Open the printed URL in a browser on Windows or macOS. The local server listens on loopback only. A static production bundle can be generated using `npm run build`.

## Features

- Overview with visit counts, pending human reviews, active simulations and evidence coverage.
- Agent workspace with visit selection, stage progress, execution logs, pause/resume, cancel, simulated failure and retry. Runs persist in localStorage and advance only while the page is open. Use one workspace tab at a time.
- Routing preferences for DeepSeek/OpenAI are simulation labels only. No model credentials, tokens or costs are used. Transcription and model stages do not produce AI output; the final draft uses recorded fields only.
- Review states: Unreviewed, In review, Follow-up required and Reviewed. Mark reviewed requires a human acknowledgement. It does not sign or certify the report.
- Local team directory with names, optional email and role labels. Assign visits to people; assigned people cannot be removed until their visits are reassigned. No invitations are sent and roles do not enforce permissions.
- Analytics with evidence coverage bars, visit-date comparisons, coordinate listings, external OpenStreetMap links and summary CSV export. Filter imported records separately from demo records. These metrics measure documentation, not colony health.
- Browser-local activity history (last 300 events), with JSON export. This is not a server or tamper-proof audit trail.
- Demonstration visits are explicitly labelled and separate from imported records.
- Import the Android app's Export ZIP for one PIN and visit date.
- View hive forms and GPS, navigate photos, play imported audio with browser controls.
- Filter visits and inspect missing forms, GPS or insufficient photos.
- Produce an evidence-based template draft, save review notes, download Markdown, or print/save PDF using the browser.
- Imported records and media persist in IndexedDB in this browser. Notes persist in localStorage. Changing browser, private mode, clearing site data or browser eviction can remove these records. Keep the original ZIP elsewhere.
- Duplicate PIN/date imports are rejected to avoid silently replacing previous material.
- Import limits: 100 MB archive, 200 MB expanded, 40 MB per file, 1 MB per JSON, 1500 files.

## Not implemented

Cloud uploads/deployment, authentication and enforced permissions, cross-device sync, backups, transcription, model analysis, report sign-off, DOCX export, embedded geographic basemaps and source-file editing/deletion. Connection settings deliberately show services as disconnected. Do not use local review states as proof of audit approval.

Old exports without visit dates are rejected with an actionable message; they are not silently assigned a date. Media-only hive folders remain visible and are flagged as lacking a form. A session file or at least one valid form is needed to identify the visit.

## Verification

```sh
npm test
npm run build
node tests/ui.cjs
node tests/operations.cjs
```

Browser tests use the existing Playwright dependency in `../preview/node_modules`, against the development server at port 4173. `tests/ui.cjs` covers real ZIP import, persistence, duplicate and invalid input, photo preview, WAV playback and report notes/download. `tests/operations.cjs` covers assignment, status, agent pause/reload/failure/retry/cancel, human review acknowledgement, exports, English UI and 1440/768/390/360 px layouts in Chromium and WebKit. It asserts no external requests occur during those flows. Browser printing, native M4A codecs and Safari/Windows hardware still require real-device checks.

On a fresh clone, prepare those browser-test dependencies from the repository root:

```sh
npm ci --prefix preview
node preview/node_modules/playwright/cli.js install chromium webkit
```

Keep the workbench dev server running in another terminal before running the browser tests.

## Try the Agent preview

1. Open Agent runs, choose a visit and select Start simulated run.
2. Use Pause/Resume or Cancel while the run is active. Repeated start on the same visit opens the active run instead of duplicating it.
3. When the run reaches review, select Review draft. Audio is still untranscribed and all model stages are labelled simulated.
4. Enter reviewer notes and export Markdown or use Print / PDF. To mark the visit reviewed, acknowledge that you checked the source evidence.
5. To exercise failure handling, enable Simulate provider failure in Settings and start a new run. Retry resumes from the failed step with failure injection disabled for that run.

Interface text and demo records are in English. Imported source notes are preserved verbatim, never automatically translated or replaced.

## Demo image

`public/assets/orchard.jpg`: orchard apiary photo from https://grocycle.com/wp-content/uploads/2021/08/beehives-in-an-orchard-1024x631.jpg (source: https://grocycle.com/beekeeping-for-profit/). Used as a clearly labelled local interface demonstration asset, not as inspection evidence. Replace with approved project photography before public distribution.

## Cloud integration boundary

The future cloud service should accept visit manifests and original files, return server-verified receipt status, and expose report jobs. Front-end demo records are not server records. Client-side validation is for local usability and does not replace server-side archive checks, auth or authorization. Keep model credentials on the server.
