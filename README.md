# Bee Audit

### Field evidence. Traceable review. Human-led intelligence.

An offline-first inspection toolkit exploring the journey from a hive in the
orchard to an evidence-linked review on the desktop.

**Android capture · English operations workspace · Agent workflow preview**

> [!WARNING]
> **PUBLIC TEST PREVIEW — NOT FOR OPERATIONAL USE.**
> This repository and its APK are experimental evaluation materials, not a
> production release. Do not rely on them for real audits, certification,
> contractual acceptance, payments, regulatory reporting or colony-health
> decisions. Use synthetic or properly de-identified test data only.
> Reports are unsigned drafts. Agent model stages are simulated.

[Quick start](#try-the-workspace) · [Test APK](downloads/README.md) ·
[Privacy](PRIVACY.md) · [Security](SECURITY.md) ·
[Test limitations](DISCLAIMER.md) · [Agent roadmap](docs/AGENT_ROADMAP.md)

## Built around the field workflow

Inspection work does not start at a desk. It starts beside a hive, often without
connectivity, with hands that should not be handling an expensive phone.

Bee Audit explores a deliberately simple capture workflow: identify the visit,
record the hive, take photographs, add a voice note, and keep the evidence
together. The desktop workspace turns that collection into a reviewable record
without pretending that a polished report is the same thing as a verified audit.

## Three connected layers

| Layer | What you can evaluate today | Boundary |
| --- | --- | --- |
| **Capture** | Android photos, M4A voice notes, GPS, visit dates, forms and ZIP export | Existing debug APK; phone validation still required |
| **Review** | English visit workspace, evidence playback, reviewer assignment, status, coverage analytics and draft exports | Browser-local storage; no account security or shared backend |
| **Orchestrate** | Agent run history, stage progress, logs, pause/resume, cancel, failure injection, retry and human review | Deterministic simulator; no transcription or model API calls |

```text
Android test capture
        |
        v
Visit ZIP -> Local evidence workspace -> Human review -> Unsigned draft
                         |
                         v
                  Agent simulator
           validate / transcribe* / structure* / review* / draft

                    * simulated stages only
```

### Evidence before conclusions

- Photographs and recordings remain linked to individual hive records.
- Local rules flag missing forms, GPS, insufficient photographs and recorded damage.
- Analytics measure documentation coverage, not colony health.
- Imported source notes are preserved, not silently rewritten or translated.
- Review status requires human action; it is not a signature or certification.
- Demo records, disconnected services and simulated execution are labelled.

## Try the workspace

Use Node.js 24. From a clone of this repository:

```sh
cd workbench
npm ci
npm run dev -- --port 4173 --strictPort
```

Open **http://127.0.0.1:4173** on macOS or Windows.

1. Explore the labelled demo visits, photographs and evidence flags.
2. Open **Agent runs**, choose a visit and select **Start simulated run**.
3. Follow the stages, pause a run, or test failure and retry through Settings.
4. Open the draft, add reviewer notes and export Markdown or print to PDF.
5. Test imports with synthetic/de-identified Android ZIPs; keep originals separately.

See the [English workspace guide](workbench/README.md) for detailed instructions.
No API key or cloud account is required. No Python environment is needed.

## Android evaluation package

[APK and installation notes](downloads/README.md) include the download, checksum,
permissions and data-loss precautions.

The included APK is the **25 June 2026 debug build**, not a new production build,
store release or certified F-Droid package. Install on a test device only. This
APK predates the public-preview notices added to the repository; it has not been
rebuilt to show an in-app warning. Read the notices before installing.

Build Android source with JDK 17 and Android SDK 35:

```sh
./gradlew lintDebug testDebugUnitTest assembleDebug
```

On Windows use `gradlew.bat`. Configure the local SDK through Android Studio or
an untracked `local.properties`. Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Agent direction

The next useful step is not unrestricted autonomy. It is reliable evidence
processing with explicit uncertainty and human review:

**Transcribe with timestamps -> link claims to evidence -> detect gaps -> compare
visits -> draft findings -> request human approval.**

The [Agent roadmap](docs/AGENT_ROADMAP.md) separates near-term priorities from
research ideas. The [backend handoff](deploy/AGENT_DEPLOYMENT.md) describes the
proposed API, worker, storage and queue boundaries. Neither is an implemented
backend or a promise of availability.

## Project map

| Directory | Purpose |
| --- | --- |
| `app/` | Android Kotlin / Jetpack Compose source and tests |
| `workbench/` | Current English browser frontend and Agent simulator |
| `deploy/` | Local frontend container recipe; future backend design |
| `downloads/` | Existing debug APK, checksum and evaluation instructions |
| `docs/` | User guides, verification notes and roadmap |
| `preview/` | Earlier Android browser mockup and browser-test dependency |

## Engineering status

- 15 frontend unit tests passed at the latest implementation checkpoint.
- Chromium and WebKit operations flows passed at four viewport widths.
- ZIP import, media playback, persistence and invalid-input paths were tested.
- The frontend production bundle builds; this does **not** make the product production-ready.
- Compose configuration validates. The optional container image has not been built
  on the handoff machine because its Docker daemon was not running.
- Physical-device acceptance, independent security review and recovery testing
  remain outstanding. No cloud service or real Agent has been deployed.

See [verification details](docs/WORKBENCH_OPERATIONS_PLAN.md).

## Privacy, security and responsible testing

Read [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md) and
[DISCLAIMER.md](DISCLAIMER.md) before testing. Never submit real inspection ZIPs,
personal data, precise private locations, API credentials or signing keys in
public issues, pull requests or commits. Use the private vulnerability reporting
channel for security-sensitive findings.

The localhost container recipe is for evaluation, not public service hosting.
There is no authentication, enforced role system, durable server job queue,
cloud backup, transcription service or model integration.

## Code and third-party rights

This is a public source repository. **A project-wide open-source license has not
yet been selected**; public visibility alone is not a blanket license grant.
Existing third-party notices and licenses remain applicable. Privacy notices and
test warnings do not replace a software license.

The demonstration photograph is third-party material, not project-owned evidence;
see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Its presence is not a grant of
redistribution rights. Replace it with appropriately licensed photography before
redistributing a packaged demo. Historical specifications describe intent and may
not reflect the current implementation.
