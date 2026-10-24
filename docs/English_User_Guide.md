# Bee Audit English User Guide

## 1. What This App Does

Bee Audit is an offline Android app for checking beehives delivered for pollination.

It records photos, voice notes, GPS location, and simple form data for each hive. It does not require internet access.

## 2. Installation

1. Send `app-debug.apk` to the Android phone.
2. Open the APK file on the phone.
3. If Android blocks the install, allow "Install unknown apps" for the email app, file manager, or browser being used.
4. After installation, open `Bee Audit`.

## 3. First-Time Permissions

Please allow these permissions:

- Camera: for hive photos
- Microphone: for voice notes
- Location/GPS: for recording the inspection location

If the permission popup does not appear immediately, tap `Take photo`, `Record`, or `GPS`.

## 4. Start a Hive Check

1. Open the app.
2. At the top of the screen, find the `PIN / KPIN` field.
3. Enter a PIN, for example: `TEST001`.
4. Check the `Visit date`. It defaults to today's date.
5. Leave `Group` as `Hive`, or enter another group name.
6. Check the current hive number, for example `Hive 0001`.

The same PIN visited on different dates is saved into separate date folders.

## 5. GPS and Maps

1. Tap `GPS` to get the current position.
2. The app shows latitude, longitude, and accuracy after a fix.
3. Tap `Map` to send the coordinates to a map app such as CoMaps, Organic Maps, OsmAnd, or Google Maps.

## 6. Taking Photos

1. Make sure the camera preview is visible.
2. If the camera is off, tap `Camera on`.
3. Turn `Forced flash` on or off as needed.
4. Tap `Photo`.
3. After saving, the status should show `Saved photo ...`.
4. Look at the `Current files` section.
5. A `.jpg` file should appear under `Photos`.

Recommended: take 2 to 5 photos per hive.

The app turns the camera off when recording starts to reduce battery drain.

## 7. Recording Voice Notes

1. Tap `Record` to start recording.
2. Speak your notes.
3. While recording, the app shows `REC 1s / REC 2s...`.
4. Tap `End` to finish.
5. After saving, the status should show `Saved voice note ...`.
6. A `.m4a` file should appear under `Voice notes`.

## 8. Review, Play, and Delete Files

In `Current files`:

- Tap `View` beside a photo to preview it in the app.
- Tap `Play` beside a voice note to listen to it.
- Tap `Stop` to stop playback.
- Tap `Delete` to remove a file. The app asks for confirmation before deleting.

## 9. File Location

Each hive check has its own folder.

Photos, voice notes, and the form file are saved together in the same folder, for example:

```text
audit/TEST001/2026-06-25/TEST001_Hive_0001/
  TEST001_Hive_0001_01.jpg
  TEST001_Hive_0001_02.jpg
  TEST001_Hive_0001_01.m4a
  TEST001_Hive_0001.json
```

The app shows the current folder path under `Current files`.

## 10. Choose an External Save Folder

In the `Save folder` section:

1. Tap `Choose`.
2. Select an SD card, USB device, or another phone folder.
3. The app still saves a reliable local copy, and also mirrors photos, voice notes, and forms into the chosen external folder.
4. Tap `Clear` to stop external mirroring.

## 11. Export

Tap `Export` to create a ZIP file for the current PIN and visit date, then send it through Android's share sheet to email, a file manager, cloud storage, or another app.

## 12. Save, New PIN, and Exit

1. Confirm that photos and voice notes are listed.
2. Fill in the form or notes if needed.
3. Tap `Save and next hive`.
4. The hive number will increase, for example from `0001` to `0002`.
5. The next hive will use a new check folder.
6. Tap `New PIN / Group` to start a new PIN or group.
7. Tap `Close and exit` to clear the current entry fields and close the app.

## 13. What to Report During Testing

Please report:

- Phone model
- Android version
- Whether the camera preview works
- Whether `.jpg` files appear after taking photos
- Whether `Record` starts the timer
- Whether `.m4a` files appear after tapping `End`
- Whether `View` opens photos
- Whether `Play` plays voice notes
- Whether `Delete` removes files
- Whether `Map` opens a map app
- Whether `Export` creates and shares a ZIP
- Whether `Choose` can select an SD card, USB device, or another folder
- Whether photos and voice notes are in the same `This check folder`
- If anything fails, copy the app status message exactly and send it back
