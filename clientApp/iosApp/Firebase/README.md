# Firebase config (client iOS)

Drop the per-client `GoogleService-Info` plists here (gitignored — never commit real Firebase keys):

- `GoogleService-Info-Development.plist`
- `GoogleService-Info-Production.plist`

Each comes from the Firebase console for the iOS app whose bundle id matches this client
(`BUNDLE_ID` in `../Configuration/*.xcconfig`, e.g. `com.ampairs.app.ambika` — mirror the Android
`clients/<id>/config.properties` `applicationId`). Every white-label client is its own Firebase iOS
app, so these plists are swapped per client alongside `Configuration/Client-<id>.xcconfig`.

The `GOOGLE_SERVICE` build setting selects the right plist per configuration; add a run-script build
phase (or resource) that bundles the active one as `GoogleService-Info.plist`. Then copy that
plist's `REVERSED_CLIENT_ID` into `../iosApp/Info.plist` → `CFBundleURLTypes` → `CFBundleURLSchemes`
(replaces `REPLACE_WITH_REVERSED_CLIENT_ID`) so Firebase Phone Auth's reCAPTCHA callback resolves.
