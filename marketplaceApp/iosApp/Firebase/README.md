# Firebase config (marketplace iOS)

Drop the two `GoogleService-Info` plists for the **marketplace** iOS app here (they are gitignored —
never commit real Firebase keys):

- `GoogleService-Info-Development.plist`
- `GoogleService-Info-Production.plist`

Both come from the Firebase console for the iOS app whose bundle id is `com.ampairs.app.market`
(register it in Firebase first — mirror the Android `marketplaceApp` registration). The
`GOOGLE_SERVICE` build setting in `../Configuration/{Debug,Release}.xcconfig` points the target at
the right plist per configuration; add a "Copy GoogleService-Info" run-script build phase (or set the
plist as the target's `GoogleService-Info.plist` resource) so the active one is bundled as
`GoogleService-Info.plist` at build time.

Then copy each plist's `REVERSED_CLIENT_ID` into `../iosApp/Info.plist` →
`CFBundleURLTypes` → `CFBundleURLSchemes` (replaces `REPLACE_WITH_REVERSED_CLIENT_ID`) so Firebase
Phone Auth's reCAPTCHA callback URL resolves.
