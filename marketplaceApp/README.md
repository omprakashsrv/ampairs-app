# :marketplaceApp — common (multi-store) Ampairs ecom app

The customer-facing app that lists **every published storefront** and lets the user pick one, then
browse + order inside it. Each store gets its own isolated Room DB, activated on selection — mirroring
the main app's workspace listing → selection → DB isolation.

Contrast with `:clientApp`, which builds one white-label APK **pinned** to a single store.

Both apps build on `:shared-ecom`. The difference is a single call site:

```kotlin
// :clientApp     → pinned:    StorefrontRoot(graph, workspaceSlug = BuildConfig.WORKSPACE_SLUG, seedColor = ...)
// :marketplaceApp → directory: StorefrontRoot(graph)   // no slug → storefront picker
```

Flow: **login-first** → storefront directory (`StorefrontDirectoryScreen`) → select a store →
`StorefrontWorkspaceManager.activate(slug)` + apply the store's brand color → catalog / cart / checkout.

## Build

```bash
./gradlew :marketplaceApp:assembleDebug
```

## ⚠️ Firebase — placeholder app id must be replaced before release

`applicationId = com.ampairs.app.market`. The `googleServices` + `firebaseCrashlytics` Gradle plugins
(applied here, mirroring `:clientApp`, because the Crashlytics SDK hard-crashes at startup without its
build-id) **require this package to exist in `google-services.json`**.

`marketplaceApp/google-services.json` now contains a `com.ampairs.app.market` client block so the build
passes, **but its `mobilesdk_app_id` is a placeholder** (`1:682032206651:android:0000000000000000a55d1e`)
— it is not a real Firebase app id. That's enough for `processDebugGoogleServices` (which never contacts
Firebase), but Analytics/Crashlytics reporting will not actually work until you:

1. Register the package `com.ampairs.app.market` in the Firebase console (same Ampairs project `682032206651`).
2. Replace the placeholder `mobilesdk_app_id` in `marketplaceApp/google-services.json` with the real one
   (or drop in the console-downloaded `google-services.json`).

The `api_key` and `project_info` are already the real project values (shared across the project), so only
the `mobilesdk_app_id` needs swapping. This is the same one-time onboarding step every new package needs.

## Backend dependency

The directory needs `GET /api/v1/storefronts` (paginated list of published stores, incl. an optional
`brand_color_argb`). Until that endpoint ships in the `ampairs` backend, the directory shows an error
state. See `feature/ecom/.../data/api/EcomApi.kt#listStorefronts`.
