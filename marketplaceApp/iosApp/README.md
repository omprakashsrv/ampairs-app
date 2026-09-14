# Ampairs Market — iOS app (multi-store storefront)

iOS counterpart of the Android `:marketplaceApp`. Both build on `:shared-ecom`
(`StorefrontRoot(graph, workspaceSlug = null)` → **directory mode**: after login the customer picks a
storefront and that store's isolated graph is activated on selection). This directory holds the thin
Swift/CocoaPods shell; all UI + logic live in the shared `SharedEcom` framework.

> The Xcode **project** (`iosApp.xcodeproj`) is created once in Xcode (see below) — this repo ships
> the Swift sources, Podfile, `Info.plist`, and xcconfigs it wires up. Same approach as `../../iosApp`.

## Layout

```
marketplaceApp/iosApp/
├── Podfile                      # pod 'shared-ecom' (framework SharedEcom) + Firebase pods
├── Configuration/
│   ├── Debug.xcconfig           # bundle id, app name, GoogleService plist (dev)
│   └── Release.xcconfig         # + versioning (prod)
├── Firebase/                    # drop GoogleService-Info-*.plist here (gitignored) — see its README
└── iosApp/
    ├── iOSApp.swift             # SwiftUI @main
    ├── AppDelegate.swift        # FirebaseApp.configure() + Phone Auth push/URL forwarding
    ├── ContentView.swift        # ComposeView → StorefrontViewController(workspaceSlug: nil, seedColorArgb: 0)
    └── Info.plist               # AMPAIRS_ENVIRONMENT, Firebase reCAPTCHA URL scheme, usage strings
```

## One-time Xcode setup

1. **Create the app target.** In Xcode: File → New → Project → iOS App named `iosApp`, saved into
   this folder (so `marketplaceApp/iosApp/iosApp.xcodeproj` sits next to `Podfile`). Delete the
   auto-generated `ContentView.swift`/`<App>.swift` and add the four Swift files under `iosApp/` here,
   plus this `Info.plist`. Match `../../iosApp` for build settings.
2. **Wire the xcconfigs.** Project → Info → Configurations: set Debug → `Configuration/Debug.xcconfig`
   and Release → `Configuration/Release.xcconfig`. In Build Settings set the product bundle id to
   `$(BUNDLE_ID)` and, if you use it, the display name to `$(APP_NAME)`.
3. **Firebase.** Register an iOS app with bundle id `com.ampairs.app.market` in the Firebase console,
   download both `GoogleService-Info` plists into `Firebase/` (see `Firebase/README.md`), and copy
   the `REVERSED_CLIENT_ID` into `iosApp/Info.plist` (`CFBundleURLSchemes`). Enable the **Push
   Notifications** capability (Phone Auth silent-push verification).
4. **CocoaPods.** From this folder: `pod install`, then always open `iosApp.xcworkspace` (not the
   `.xcodeproj`). The `shared-ecom` pod builds the `SharedEcom` framework from Gradle automatically.
5. **Run.** Select the `iosApp` scheme + a simulator/device and Run. On the Simulator, Phone Auth
   uses the reCAPTCHA fallback (expected).

## How the shared framework is produced

`:shared-ecom` has a `cocoapods { framework { baseName = "SharedEcom"; isStatic = true } }` block.
`pod 'shared-ecom', :path => '../../shared-ecom'` makes CocoaPods invoke the Gradle
`podInstallSyntheticIos` / framework tasks. Firebase (Core/Auth) and reCAPTCHA symbols are referenced
transitively by `:feature:auth` and resolved at the app link step by the pods in this `Podfile`
(mirrors `../../iosApp/Podfile`).

## Entry point

`ContentView.swift` calls the Kotlin entry:

```swift
StorefrontViewControllerKt.StorefrontViewController(workspaceSlug: nil, seedColorArgb: 0)
```

`nil` slug = directory mode; `0` = default seed color (each selected store applies its own brand
color). Defined in `shared-ecom/src/iosMain/.../StorefrontViewController.kt`.
