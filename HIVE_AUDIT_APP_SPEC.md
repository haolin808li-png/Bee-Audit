# Hive Pollination Audit App

## Purpose

An offline Android field app for checking hives delivered for pollination and keeping a simple evidence record: photos, location, time, short notes, and optional voice recordings. It should run on old phones, be side-loadable, and be suitable for open-source release on F-Droid.

The core design assumption is that beekeepers may be using sticky gloves, honey-covered hands, propolis, wax, rain, and rough old phones. The app should need very few touches and should not depend on internet access.

## Field Workflow

1. Open the app.
2. Choose or enter the current `PIN` / `KPIN`.
3. Choose or enter the group, for example `Hive`, `Block`, `Orchard row`, or another local naming scheme.
4. Start the audit sequence.
5. For each hive:
   - Take 2-5 photos.
   - Optionally record a voice note.
   - Optionally fill a short form.
   - Save GPS, date, and time automatically.
   - Auto-increment the sequence number.
6. At the end, review the list and export or leave the files on phone/SD card.

## Important UX Rules

- Big buttons that can be hit with gloves.
- Very few screens.
- No internet required.
- Works with dirty old Android phones.
- No Play Store dependency.
- Forced flash option.
- Hardware volume button can trigger photo capture.
- Auto-save everything immediately.
- Auto-increment hive sequence numbers.
- App must recover gracefully if the battery dies or the app is closed.

## Data Model

### Audit Session

- `pin`: grower/property/pollination identifier.
- `group`: user-defined group such as `Hive`, `Row`, `Block`, or yard name.
- `started_at`: local date/time.
- `device_id`: optional local device label.
- `gps`: latest known GPS location.

### Hive Record

- `pin`
- `group`
- `sequence_number`
- `created_at`
- `gps_lat`
- `gps_lon`
- `gps_accuracy_m`
- `photo_count`
- `voice_note_count`
- `form_fields`

### Photo Record

- `pin`
- `group`
- `sequence_number`
- `photo_number`
- `taken_at`
- `gps_lat`
- `gps_lon`
- `gps_accuracy_m`
- `flash_mode`
- `resolution`
- `file_path`

## File Layout

Suggested root:

```text
/audit/
  PIN/
    session.json
    index.csv
    form/
      PIN_Group_SeqNo.json
    voice/
      PIN_Group_SeqNo_01.m4a
    photos/
      PIN_Group_SeqNo_01.jpg
      PIN_Group_SeqNo_02.jpg
      PIN_Group_SeqNo_03.jpg
```

Example:

```text
/audit/KPIN1234/
  session.json
  index.csv
  form/
    KPIN1234_Hive_0007.json
  voice/
    KPIN1234_Hive_0007_01.m4a
  photos/
    KPIN1234_Hive_0007_01.jpg
    KPIN1234_Hive_0007_02.jpg
    KPIN1234_Hive_0007_03.jpg
```

Alternative flat layout, closer to the original idea:

```text
/audit/PIN/Group_PINSeqNo.jpg
```

This is simple, but the grouped layout above will be easier to search, export, and check later when there are many photos per hive.

## Naming Rules

Use padded sequence numbers so files sort correctly:

```text
PIN_Group_0001_01.jpg
PIN_Group_0001_02.jpg
PIN_Group_0002_01.jpg
```

Recommended format:

```text
{PIN}_{Group}_{SeqNo}_{ItemNo}.{ext}
```

Where:

- `PIN`: cleaned text, no spaces or unsafe filesystem characters.
- `Group`: cleaned text.
- `SeqNo`: four digits, for example `0001`.
- `ItemNo`: two digits for photo or voice item number, for example `01`.

## Photos

Requirements:

- 4:3 aspect ratio.
- Default resolution: `1280x960`.
- Optional high resolution: `2880x2160` where supported.
- Forced flash mode.
- Date/time captured in metadata and index.
- GPS captured in metadata and index.
- Save to phone storage or SD card.
- 2-5 photos per hive.

Implementation notes:

- Use Android CameraX if acceptable for the minimum Android version.
- Store EXIF metadata for date/time and GPS when possible.
- Also store the same data in `index.csv`, because CSV is easier to recover and read on any computer.
- Provide a clear warning if GPS is unavailable, but still allow photos.

## Voice Notes

Preferred format:

```text
.m4a
```

Reason: native Android audio recording commonly supports AAC in M4A more easily than MP3. If MP3 is absolutely required, the app would need an encoder dependency, which makes F-Droid packaging more complicated.

Suggested files:

```text
/audit/PIN/voice/PIN_Group_0001_01.m4a
```

Voice workflow:

- One large Record / Stop button.
- Show recording timer.
- Auto-save when stopped.
- Allow multiple voice notes per hive.

## Form

Keep the form short. Voice notes should handle anything longer.

Suggested fields:

- Hive present: yes/no
- Hive condition: good / fair / poor / missing
- Bee activity: high / medium / low / none
- Queen issue visible: yes/no/unknown
- Damage: yes/no
- Feed present: yes/no/unknown
- Notes: short text

Save as:

```text
/audit/PIN/form/PIN_Group_0001.json
```

Also add one line per hive to:

```text
/audit/PIN/index.csv
```

## CSV Index

Each audit should maintain an `index.csv` for easy office review.

Suggested columns:

```text
pin,group,seq_no,created_at,lat,lon,gps_accuracy_m,photo_count,voice_count,condition,activity,damage,notes
```

Example row:

```text
KPIN1234,Hive,0007,2026-06-08T09:42:11+12:00,-43.5321,172.6362,8.5,3,1,good,high,no,"Strong hive, good entrance activity"
```

## Offline GPS

- Use the phone GPS provider.
- Do not require maps.
- Store latitude, longitude, accuracy, and timestamp.
- If GPS fix is old, mark it as stale.
- Allow audit to continue without GPS, but clearly show `No GPS fix`.

## Permissions

Required Android permissions:

- Camera
- Microphone
- Location
- Read/write selected storage location

For newer Android versions, use the Storage Access Framework so the user can choose the audit folder or SD card folder.

## F-Droid / Open Source Constraints

Recommended:

- Kotlin or Java Android app.
- No Google Play Services.
- No Firebase.
- No network permission unless there is a clear future reason.
- Use open-source dependencies only.
- Keep dependencies minimal for old-phone compatibility.

Likely stack:

- Kotlin
- CameraX or native Camera2
- MediaRecorder for audio
- Android location APIs without Google Play Services
- JSON and CSV written locally

## MVP

The first useful version should include:

- Create/select `PIN`.
- Create/select `Group`.
- Auto-increment hive sequence number.
- Take 1-5 photos per hive.
- Forced flash toggle.
- Save photo files in the audit directory.
- Save timestamp and GPS to CSV.
- Record one or more voice notes.
- Basic short form.
- Review list of saved hives.

## Nice Later

- Export audit as ZIP.
- QR code/barcode scan for PIN.
- Import a list of expected PINs or hive counts.
- Compare expected vs checked hive count.
- Large high-contrast night mode.
- Waterproof-phone mode with even bigger controls.
- Duplicate photo warning.
- Offline map preview if OpenStreetMap tiles are deliberately preloaded.
- Share/export by USB, SD card, or local file only.

## Open Questions

- Is `PIN` the same thing as `KPIN`, or are both needed?
- Does `Group` mean hive type, orchard block, beekeeper group, or something else?
- Should sequence numbers reset per `PIN`, per `Group`, or per day?
- Is `.mp3` required, or is `.m4a` acceptable?
- Should photos be stamped visually with date/time/PIN, or only stored in metadata and CSV?
- What is the oldest Android version the target phones need to support?

