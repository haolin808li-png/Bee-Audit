# Bee Audit Development TODO

## Current Status

- [x] Product requirements captured in `HIVE_AUDIT_APP_SPEC.md`.
- [x] Jetpack Compose Android project scaffolded.
- [x] Local Android SDK installed at `~/Library/Android/sdk`.
- [x] Gradle wrapper generated.
- [x] AndroidX project flag enabled.
- [x] Debug unit tests pass.
- [x] Android lint passes with 0 errors under JDK 17.
- [x] Debug APK builds successfully.
- [x] Local web preview exists in `preview/`.
- [x] App can write real audit files into app external storage.
- [x] App can request GPS/camera/audio runtime permissions.
- [x] App can capture CameraX JPEG photos.
- [x] App can record `.m4a` voice notes.
- [x] App restores PIN/group/sequence/flash state.

Latest verified command:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew lintDebug testDebugUnitTest assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Phase 1: Buildable MVP Foundation

- [x] Create Android app module.
- [x] Configure Kotlin + Compose + Material 3.
- [x] Add camera, microphone, and location permissions.
- [x] Add local app icon.
- [x] Add PIN / KPIN input.
- [x] Add group input.
- [x] Add hive sequence controls.
- [x] Add forced flash UI toggle.
- [x] Add photo and voice counters.
- [x] Add basic hive form.
- [x] Add sortable file naming helpers.
- [x] Add CSV row generation helpers.
- [x] Add unit tests for file naming and CSV output.

## Phase 2: Local Storage

- [x] Create an `AuditStorage` for writing audit files.
- [x] Choose or create the audit root folder.
- [x] Support default app external storage for first build.
- [ ] Add Storage Access Framework folder picker for SD card / shared folder.
- [ ] Create directory tree:

```text
/audit/
  PIN/
    session.json
    index.csv
    form/
    photos/
    voice/
```

- [x] Write `session.json` when a PIN audit starts.
- [x] Append one row per hive to `index.csv`.
- [x] Save per-hive form JSON to `/audit/PIN/form/`.
- [ ] Add tests for directory and filename generation.
- [ ] Add tests for CSV append behavior.
- [x] Show current save location in the UI.
- [x] Show a visible error if storage write fails.
- [x] Keep app usable without a manually selected folder.

## Phase 3: GPS

- [x] Add Android location provider implementation without Google Play Services.
- [x] Request runtime location permission.
- [x] Capture latitude, longitude, accuracy, and timestamp.
- [x] Mark GPS fix as fresh or stale in model.
- [x] Store GPS data in `index.csv`.
- [x] Store GPS data in per-hive JSON.
- [x] Display `No GPS fix` when unavailable.
- [x] Allow saving records without GPS.
- [x] Add manual refresh button for GPS.
- [ ] Add tests around stale/fresh GPS state.

## Phase 4: Camera

- [x] Add CameraX dependencies.
- [x] Add runtime camera permission handling.
- [x] Add camera preview panel.
- [x] Capture JPEG photos.
- [x] Save photos to `/audit/PIN/photos/`.
- [x] Use naming format `{PIN}_{Group}_{SeqNo}_{PhotoNo}.jpg`.
- [x] Prefer 4:3 capture through CameraX aspect ratio.
- [x] Prefer `1280x960` for default photo capture.
- [ ] Support higher resolution option where available.
- [x] Wire forced flash toggle to CameraX flash mode.
- [x] Save photo timestamp through file metadata.
- [x] Write GPS EXIF metadata when GPS is available.
- [x] Add fallback if EXIF write fails.
- [x] Limit or warn outside the target 2-5 photos per hive.
- [x] Support hardware volume button as photo capture.
- [x] Add capture failure message and retry path.

## Phase 5: Voice Notes

- [x] Add runtime microphone permission handling.
- [x] Add `MediaRecorder` based `.m4a` recording.
- [x] Save voice files to `/audit/PIN/voice/`.
- [x] Use naming format `{PIN}_{Group}_{SeqNo}_{VoiceNo}.m4a`.
- [x] Add large Record / Stop control.
- [x] Show recording timer.
- [x] Auto-save on stop.
- [x] Allow multiple voice notes per hive.
- [ ] Handle app backgrounding during recording.
- [x] Show recording failure message and retry path.

## Phase 6: Hive Record Flow

- [x] Replace placeholder counters with real saved photo/voice file counts.
- [x] Make `Save and next hive` write form + CSV data.
- [ ] Auto-save partial hive state after every capture.
- [x] Restore current audit basics after app restart.
- [x] Restore current sequence after battery loss/app close.
- [ ] Add review list of hives checked in current PIN.
- [ ] Allow editing the last hive record.
- [ ] Add confirmation before decreasing sequence number when files exist.
- [x] Add duplicate filename protection for new photo/voice files.

## Phase 7: Field Usability

- [ ] Increase hit target sizes where needed for gloves.
- [ ] Add high-contrast field mode.
- [ ] Add night/dark mode check.
- [ ] Keep the main capture button reachable with one hand.
- [ ] Reduce text entry after first hive.
- [ ] Add optional PIN/KPIN reuse history.
- [ ] Add optional group reuse history.
- [x] Make app work fully offline with no network permission.
- [x] Confirm no Firebase, Google Play Services, analytics, or internet dependency.

## Phase 8: Export and Office Review

- [ ] Add ZIP export for one PIN audit.
- [ ] Add share/export through Android Sharesheet.
- [ ] Add copy-to-SD-card flow.
- [ ] Add CSV preview screen.
- [ ] Add missing-hive summary.
- [ ] Add photo count summary by hive.
- [ ] Add voice note count summary by hive.

## Phase 9: Packaging

- [ ] Add release build configuration.
- [ ] Add signing instructions.
- [ ] Add reproducible build notes.
- [ ] Add F-Droid metadata draft.
- [ ] Check dependency licenses.
- [ ] Confirm minimum Android version with target old phones.
- [ ] Test side-loading debug APK on an old Android phone.
- [ ] Test release APK install.

## Phase 10: Verification Matrix

- [x] Unit tests: filename generation.
- [x] Unit tests: current CSV generation.
- [ ] Unit tests: storage repository.
- [ ] Unit tests: GPS stale/fresh state.
- [ ] Manual test: app opens.
- [ ] Manual test: PIN/group entry.
- [ ] Manual test: GPS permission denied.
- [ ] Manual test: GPS permission granted.
- [ ] Manual test: camera permission denied.
- [ ] Manual test: camera permission granted.
- [ ] Manual test: forced flash.
- [ ] Manual test: 2-5 photos per hive.
- [ ] Manual test: audio permission denied.
- [ ] Manual test: audio permission granted.
- [ ] Manual test: record and stop voice note.
- [ ] Manual test: save hive record.
- [ ] Manual test: restart app and continue same audit.
- [ ] Manual test: export files and inspect on computer.

## Open Product Questions

- [ ] Decide whether `PIN` and `KPIN` are the same field or separate fields.
- [ ] Decide whether `Group` means hive group, orchard block, yard, row, or a free-form field.
- [ ] Decide whether sequence numbers reset per PIN, per group, or per day.
- [ ] Decide whether visual date/PIN stamping on photos is required.
- [ ] Confirm `.m4a` is acceptable instead of `.mp3`.
- [ ] Confirm the oldest Android version/device model to support.
