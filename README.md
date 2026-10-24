# Bee Audit

Offline Android evidence capture and an English desktop operations workspace for
pollination hive inspections. The desktop Agent interface is a local simulator,
not a deployed AI service.

## Repository contents

| Path | Contents |
| --- | --- |
| `app/` | Android Jetpack Compose source, camera, audio, GPS and visit records |
| `workbench/` | English browser frontend, local imports, review and Agent simulator |
| `deploy/` | Frontend container recipe and future Agent deployment handoff |
| `downloads/` | Existing Android debug APK and checksum |
| `docs/` | Android user guides, frontend plans and verification notes |
| `preview/` | Earlier Android browser mockup; not the current desktop workspace |

## Desktop quick start

Install Node.js 24, then run:

```sh
cd workbench
npm ci
npm run dev -- --port 4173 --strictPort
```

Open http://127.0.0.1:4173 in a browser on macOS or Windows. See the
[workbench guide](workbench/README.md) for workflows and tests.

The frontend includes visits, photographs, audio playback, reviewer assignments,
status management, analytics, reports, activity history and Agent run controls.
Demo data is labelled. Import real Android visit ZIPs to review actual evidence.

## Android installation and source

Download [the existing test APK](downloads/BeeAudit-debug-2026-06-25.apk).
Read [installation and checksum notes](downloads/README.md) before installing.
This is a debug package, not a Play Store/F-Droid release or a new October build.

To build from source, install JDK 17 and Android SDK 35. Configure the SDK through
Android Studio or a local, untracked `local.properties` file. Then run:

```sh
./gradlew lintDebug testDebugUnitTest assembleDebug
```

On Windows use `gradlew.bat`. Output: `app/build/outputs/apk/debug/app-debug.apk`.
The existing APK was not rebuilt as part of this repository handoff.

## Deployment and Agent status

[Frontend packaging instructions](deploy/README.md) include a Docker Compose
recipe restricted to localhost. [Agent deployment handoff](deploy/AGENT_DEPLOYMENT.md)
describes the proposed API/worker architecture and the outstanding implementation.

**No real Agent backend, cloud upload, transcription or model API integration is
implemented. No cloud resources have been deployed.** Agent runs simulate stage
progress and produce a local template draft from recorded fields. Human review
is required; a local Reviewed label is not a signature or certification.

Team roles are local directory labels, not security permissions. Browser storage
is not backup. Keep the original Android ZIPs separately. Do not publish this
preview as a production service without authentication, server-side validation
and access controls.

## Verification at handoff

- 15 frontend unit tests and the Vite production build pass.
- Chromium/WebKit operations flows and four viewport widths passed locally.
- Import tests cover real ZIP/media handling, persistence and invalid input.
- The APK copy has a recorded SHA-256 checksum; no new physical-phone test was run.
- Docker Compose configuration validation passes. The optional Docker recipe has
  not been container-built because the local Docker daemon is not running.

Historical Android specifications and TODO files are retained for context; the
current frontend scope is recorded in `docs/WORKBENCH_OPERATIONS_PLAN.md`.

## Distribution notes

No project-wide open-source license has been selected yet. Repository access does
not itself grant an open-source license. Third-party dependencies retain their own
licenses. The labelled demonstration photograph is attributed in the workbench
README; replace it with approved project photography before public distribution.
Never commit inspection exports, API credentials, signing keys or local SDK paths.
