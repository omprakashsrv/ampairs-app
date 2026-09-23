# Ampairs Client — iOS app (white-label storefront)

iOS counterpart of the Android `:clientApp`. Builds on `:shared-ecom`
(`StorefrontRoot(graph, workspaceSlug = <pinned>, seedColor = <brand>)` → **pinned mode**: no picker,
lands straight on the client's storefront after login). Like the Android build, **one target serves
every client** — the pinned workspace slug and brand color are injected at build time (here via
Info.plist / xcconfig instead of Android's `-Pclient=<id>`).

> The Xcode **project** (`iosApp.xcodeproj`) is created once in Xcode (see below) — this repo ships
> the Swift sources, Podfile, `Info.plist`, and xcconfigs. Same approach as `../../iosApp`.

## Layout

```
clientApp/iosApp/
├── Podfile                      # pod 'shared-ecom' (framework SharedEcom) + Firebase pods
├── Configuration/
│   ├── Debug.xcconfig           # bundle id, app name, GoogleService plist, client slug + color (dev)
│   ├── Release.xcconfig         # + versioning (prod)
│   └── Client-ambika.xcconfig   # example per-client override (copy per client)
├── Firebase/                    # per-client GoogleService-Info-*.plist (gitignored) — see its README
└── iosApp/
    ├── iOSApp.swift             # SwiftUI @main
    ├── AppDelegate.swift        # FirebaseApp.configure() + Phone Auth push/URL forwarding
    ├── ContentView.swift        # reads AMPAIRS_WORKSPACE_SLUG + AMPAIRS_THEME_COLOR_ARGB from Info.plist
    └── Info.plist               # exposes the two client vars (xcconfig-substituted) + reCAPTCHA scheme
```

## How per-client selection works

Mirrors `clients/<id>/config.properties` on Android:

| Android (`clients/<id>/config.properties`) | iOS (`Configuration/Client-<id>.xcconfig`) |
|---|---|
| `applicationId`  | `BUNDLE_ID` |
| `appName`        | `APP_NAME` |
| `workspaceSlug`  | `AMPAIRS_WORKSPACE_SLUG` |
| `themeColorArgb` (`0xFF1B6C4A`) | `AMPAIRS_THEME_COLOR_ARGB` (`FF1B6C4A`) |

`Info.plist` surfaces the two `AMPAIRS_*` vars via `$(...)` substitution; `ContentView.swift` reads
them at launch and passes them to `StorefrontViewController(workspaceSlug:seedColorArgb:)`. A blank
slug falls back to directory mode; a blank/invalid color falls back to the default Ampairs green.

Onboard a new client:
1. Copy `Configuration/Client-ambika.xcconfig` → `Client-<id>.xcconfig`, edit the four values.
2. Point `Debug.xcconfig` / `Release.xcconfig`'s `#include?` (and their default values) at it, or keep
   one client per build config / scheme.
3. Drop the client's `GoogleService-Info-*.plist` under `Firebase/` and its `REVERSED_CLIENT_ID` into
   `iosApp/Info.plist`.
4. Add the client's app icon set to the asset catalog.

## One-time Xcode setup

1. **Create the app target** `iosApp` saved into this folder (so `clientApp/iosApp/iosApp.xcodeproj`
   sits next to `Podfile`). Add the four Swift files under `iosApp/` and this `Info.plist`. Match
   `../../iosApp` for build settings.
2. **Wire xcconfigs.** Project → Info → Configurations: Debug → `Configuration/Debug.xcconfig`,
   Release → `Configuration/Release.xcconfig`. Set product bundle id `$(BUNDLE_ID)`, display name
   `$(APP_NAME)`.
3. **Firebase.** Register the client's iOS bundle id in Firebase, add both `GoogleService-Info` plists
   to `Firebase/` (see `Firebase/README.md`), copy `REVERSED_CLIENT_ID` into `Info.plist`, enable the
   **Push Notifications** capability.
4. **CocoaPods.** `pod install`, then open `iosApp.xcworkspace`.
5. **Run** the `iosApp` scheme.

## Build & run a specific client

There is no `-Pclient=<id>` flag on iOS — the client is selected by **which xcconfig values are
active** (see the previous section). Once the target is set up and the client's
`Configuration/Client-<id>.xcconfig` + `Firebase/GoogleService-Info-*.plist` are in place:

### Prerequisites (once per machine)
```bash
# JDK 21+, Xcode 15+, CocoaPods installed (brew install cocoapods)
cd clientApp/iosApp
pod install            # regenerates Pods/ + iosApp.xcworkspace; re-run after Podfile changes
```
`pod install` invokes Gradle to build the `SharedEcom` framework from `:shared-ecom`, so the first
run downloads the Kotlin/Native toolchain and can take a while.

### Run in the simulator / on a device
```bash
open clientApp/iosApp/iosApp.xcworkspace   # ALWAYS the .xcworkspace, never the .xcodeproj
```
In Xcode: pick the `iosApp` scheme + a simulator or device → **Run** (⌘R). Debug builds use the
`Debug.xcconfig` values (dev API base URL via `AMPAIRS_ENVIRONMENT`, dev GoogleService plist). On the
Simulator, Phone Auth uses the reCAPTCHA fallback — that's expected.

Command line equivalent (simulator):
```bash
cd clientApp/iosApp
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -configuration Debug -sdk iphonesimulator build
```

### Switch clients
Point `Debug.xcconfig` / `Release.xcconfig` at the target client's `Client-<id>.xcconfig`
(`#include?` + matching `BUNDLE_ID` / `AMPAIRS_WORKSPACE_SLUG` / `AMPAIRS_THEME_COLOR_ARGB` defaults),
swap in that client's `Firebase/GoogleService-Info-*.plist` and `REVERSED_CLIENT_ID`, then rebuild.
Prefer a **separate Xcode scheme per client** (each wired to its own xcconfig) so switching is just a
scheme change and CI can `-scheme <client>` — cleaner than editing shared files between builds.

### Archive for TestFlight / App Store (Release)
Release builds pull the prod API base URL + prod GoogleService plist from `Release.xcconfig`.
```bash
cd clientApp/iosApp
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -configuration Release -sdk iphoneos \
  -archivePath build/ambika.xcarchive archive

xcodebuild -exportArchive -archivePath build/ambika.xcarchive \
  -exportOptionsPlist ExportOptions.plist \
  -exportPath build/ambika-ipa
# then upload build/ambika-ipa/*.ipa with `xcrun altool`/Transporter, or Xcode → Organizer → Distribute
```
`ExportOptions.plist` (App Store distribution, your team id) is standard Xcode export config — not
checked in. Set `TEAM_ID` / `CURRENT_PROJECT_VERSION` / `MARKETING_VERSION` in the client's xcconfig.

### Build every client in CI
Mirror the Android loop in `clients/README.md` — one archive + upload per client:
```bash
for dir in clients/*/; do
  id=$(basename "$dir")
  # ensure clientApp/iosApp/Configuration/Client-$id.xcconfig + Firebase plists exist and a
  # per-client scheme "$id" is committed, then:
  xcodebuild -workspace clientApp/iosApp/iosApp.xcworkspace -scheme "$id" \
    -configuration Release -sdk iphoneos -archivePath "build/$id.xcarchive" archive
  # export + upload the IPA to that client's App Store Connect app
done
```

## Entry point

`ContentView.swift` calls the Kotlin entry (from `shared-ecom/src/iosMain/.../StorefrontViewController.kt`):

```swift
StorefrontViewControllerKt.StorefrontViewController(
    workspaceSlug: "<AMPAIRS_WORKSPACE_SLUG>",   // e.g. "ambika-enterprise"
    seedColorArgb: 0xFF1B6C4A                     // parsed from AMPAIRS_THEME_COLOR_ARGB
)
```
