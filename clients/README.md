# Client (white-label) builds

The customer storefront app is **one** Gradle module, `:clientApp`, built once per client by
selecting a client at build time. Nothing here duplicates code — `:clientApp` and `:shared-ecom`
are 100% shared across every client. This scales to N clients with **no new modules and no product
flavors** (which would explode into 2×N build variants).

## Build a client

```bash
./gradlew :clientApp:assembleDebug                 # defaults to client 'ambika'
./gradlew :clientApp:bundleRelease -Pclient=ambika # release AAB for a specific client
```

The IDE configures a single variant, so sync/build stays fast no matter how many clients exist.

## What a client provides

Everything client-specific lives in `clients/<id>/`:

```
clients/<id>/
  config.properties     # applicationId, appName, workspaceSlug, versionCode/Name, themeColorArgb
  res/mipmap-*/         # launcher icons (ic_launcher, _foreground, _background, _monochrome, _round)
```

`config.properties` keys (all required unless noted):

| key            | used for                                                        |
|----------------|-----------------------------------------------------------------|
| `applicationId`| the unique Play Store package name (one listing per id)         |
| `appName`      | home-screen label (manifest `android:label` placeholder)        |
| `workspaceSlug`| backend storefront slug → `BuildConfig.WORKSPACE_SLUG`          |
| `versionCode`  | independent version per client                                  |
| `versionName`  | independent version per client                                  |
| `themeColorArgb`| brand seed color (forward-looking; wiring into the theme is a small `:shared-ecom` follow-up) |

## Onboard a new client

1. `cp -r clients/ambika clients/<id>` and edit `config.properties` (new `applicationId`, name, slug).
2. Drop the client's launcher icons into `clients/<id>/res/mipmap-*`.
3. Register the new `applicationId` in the Firebase console (needed for Phone-Auth SHA-1/256), then
   re-download `google-services.json` into `clientApp/` — one file holds a `client` block per
   package name (same Firebase project), so it just gains another entry.
4. `./gradlew :clientApp:bundleRelease -Pclient=<id>` and upload the AAB to that client's Play listing.

No Kotlin/Gradle module changes are needed to add a client.

## iOS builds

The same white-label model works on iOS via `clientApp/iosApp/` (a Swift/CocoaPods shell over the
`SharedEcom` framework from `:shared-ecom`). There is **no `-Pclient=<id>` flag** — the client is
selected by the active Xcode build config instead of a Gradle property:

| Android (`clients/<id>/config.properties`) | iOS (`clientApp/iosApp/Configuration/Client-<id>.xcconfig`) |
|---|---|
| `applicationId` | `BUNDLE_ID` |
| `appName`       | `APP_NAME` |
| `workspaceSlug` | `AMPAIRS_WORKSPACE_SLUG` |
| `themeColorArgb` (`0xFF1B6C4A`) | `AMPAIRS_THEME_COLOR_ARGB` (`FF1B6C4A`) |
| `google-services.json` (one file, per-client block) | per-client `GoogleService-Info-*.plist` (one Firebase iOS app per client) |
| launcher icons in `res/mipmap-*` | app icon set in the Xcode asset catalog |

Build/run/archive commands and the per-client CI loop live in **`clientApp/iosApp/README.md`**. In
short: `pod install` (builds the shared framework), open `iosApp.xcworkspace`, pick the client's
scheme, Run for dev or `xcodebuild ... archive` + export for TestFlight/App Store. Onboarding a new
client on iOS = copy `Configuration/Client-ambika.xcconfig`, drop in that client's Firebase plists +
`REVERSED_CLIENT_ID` and icon set, and add a scheme — no Kotlin/Gradle changes, same as Android.

## CI

Loop over `clients/*/` and build + publish each:

```bash
for dir in clients/*/; do
  id=$(basename "$dir")
  ./gradlew :clientApp:bundleRelease -Pclient="$id"
  # then upload clientApp/build/outputs/bundle/release/*.aab to the $id Play listing
done
```
