# Android test APK

`BeeAudit-debug-2026-06-25.apk` is a copy of the existing local debug build from
25 June 2026. It is not a new Android build or a production-signed release.
Package: `nz.beeaudit.app`, version name `0.1.0`, version code `1`, Android 6.0+
(API 23), according to the project configuration.

Install only on a test device. Allow installation from the browser or file
manager when prompted. Grant camera, microphone and location permissions as
needed. The app collects evidence offline; this APK does not upload to a cloud
service or call an Agent. Export a visit ZIP to import into the desktop frontend.

If an installed copy uses a different signing certificate, Android may reject
an update. Export and back up all inspection records before considering an
uninstall; uninstalling may remove app-private files. Do not uninstall merely
to clear an install error without securing the data first.

SHA-256:

```text
e6342e314316edbd33402da090ba8d27a3c437e5c969701f7283379d808c4a53
```

See `docs/English_User_Guide.md` for the Android field workflow. Verify this
existing test build on the actual phone before relying on it in the field.
