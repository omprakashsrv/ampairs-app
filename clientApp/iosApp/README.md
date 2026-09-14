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

## Entry point

`ContentView.swift` calls the Kotlin entry (from `shared-ecom/src/iosMain/.../StorefrontViewController.kt`):

```swift
StorefrontViewControllerKt.StorefrontViewController(
    workspaceSlug: "<AMPAIRS_WORKSPACE_SLUG>",   // e.g. "ambika-enterprise"
    seedColorArgb: 0xFF1B6C4A                     // parsed from AMPAIRS_THEME_COLOR_ARGB
)
```
