# Privacy Notice — Public Test Preview

Last updated: 25 October 2026.

## Scope

This notice describes the code and local evaluation packages in this repository.
There is no hosted Bee Audit account service or cloud Agent operated by this
release. This is not a claim of certification or compliance with any privacy law.
Future hosted versions require their own operator-specific privacy terms before
collecting data. Use synthetic or properly de-identified test data only.

## Data handled locally

| Data | Purpose | Current storage |
| --- | --- | --- |
| PIN/KPIN, groups, visit dates and hive forms | Associate test records | Android app files; browser IndexedDB after import |
| Photos and voice recordings | Inspection evidence evaluation | Android app files, selected external copies/exports, browser IndexedDB |
| GPS coordinates and accuracy | Location reference | Associated Android records and imported browser records |
| Reviewer names, optional email and role labels | Local assignment demonstration | Browser localStorage; no invitation sent |
| Reviewer notes, statuses, routing preferences and simulation logs | Workflow evaluation | Browser localStorage |
| Exported reports, CSVs, JSON logs and ZIPs | User-requested handoff | User-selected download/share destinations |

Media can contain faces, voices, identifiers, location metadata or other sensitive
information. The importer does not automatically anonymize media or strip EXIF.
Do not import material you lack permission to process.

## Permissions and network boundaries

- Android requests camera, microphone and location permissions for capture.
  The project manifest does not declare INTERNET permission. The included build
  has no implemented Bee Audit cloud-upload or model-inference feature.
- The current frontend has no implemented advertising, analytics, telemetry or
  model API integration. Evidence is processed in the browser, not uploaded by
  the current application workflow.
- Opening map links shares the coordinates in the URL with the chosen map
  service. Attribution links, GitHub downloads and dependency installation use
  external services and their own policies.
- Android sharing/export, browser printing and file/folder selection can involve
  other apps, cloud-backed folders or device services chosen by the tester.
  Their behavior is outside this application's control.
- The optional web server may keep ordinary HTTP access logs. Those logs are
  distinct from the local inspection store. OS/browser backup, sync, extensions
  and device administration can also affect local data.

No information is sent to DeepSeek or OpenAI by the current simulator. Future
provider integrations must disclose what data is transmitted, where it is
processed, retention conditions and the controls available to users first.

## Retention, deletion and security limits

Browser records remain until website data is cleared, browser storage is evicted
or the browser profile is removed. Clearing site data removes both IndexedDB and
localStorage for that origin; it does not remove downloaded copies. Changing port
or browser creates a different storage context, not a shared backup.

Android data and external copies can have different lifecycles. Clear app storage
or uninstall only after securing any needed test data. Remove exported ZIPs,
removable-media copies, downloaded reports and shared copies separately.

There is no application-level encryption, authenticated access control, remote
deletion or backup guarantee in this preview. Anyone with access to the relevant
device/browser profile may be able to read its records. Team roles are labels.

## Public repository and feedback

Commits, issues and pull requests in this repository are public. Do not submit
real field evidence, names, phone numbers, private coordinates, tokens or logs
containing personal data. Public copies and forks may persist after deletion.

Use a minimal synthetic reproduction for ordinary feedback. Report a suspected
privacy exposure through the private channel in [SECURITY.md](SECURITY.md), not
by reposting the exposed data. The maintainer cannot erase copies already held
by third parties or guarantee a response deadline.
