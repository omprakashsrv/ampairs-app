# feature:update

App update management. Checks for available updates, downloads new versions, and triggers platform-appropriate installation flows.

## Responsibilities

- Poll backend for the latest app version
- Compare against the currently installed version
- Show an update dialog (optional or blocking based on `forceUpdate` flag)
- Download the update package on Desktop
- Route to the platform store on Android/iOS

## Key Classes

| Class | Purpose |
|---|---|
| `UpdateApi` / `UpdateApiImpl` | Endpoint: `GET /api/v1/app/version` |
| `UpdateChecker` | Compares remote vs local version, emits `UpdateStatus` |
| `AppVersion` | Platform expect/actual for reading the installed version |
| `UpdateDownloader` | Downloads APK/installer (Desktop / Android sideload) |
| `UpdateInstaller` | Triggers OS install intent |
| `UpdateDialog` | Compose dialog shown when an update is available |

## Koin Module

```kotlin
updateModule  // in com.ampairs.update.di
```

## Platform-Specific

| Platform | Behaviour |
|---|---|
| Android | Redirects to Play Store or downloads APK for sideload |
| Desktop | Downloads installer and launches it |
| iOS | Redirects to App Store URL |
