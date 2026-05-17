# Desktop In-App Update System

## Overview

This document describes the desktop in-app update system implemented for the Ampairs KMP application. The system provides automatic update checking, downloading, and guided installation for macOS, Windows, and Linux desktop platforms.

## Architecture

### 📦 Components

```
Update System Architecture:
┌─────────────────────────────────────────────────────┐
│  Desktop App Launch                                 │
│  └─> MainView (main.desktop.kt)                    │
│      └─> UpdateChecker.checkForUpdates()           │
│          └─> UpdateApi (/api/v1/app-updates/check) │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  Update Available?                                  │
│  ├─> Yes: Show UpdateDialog                        │
│  │    ├─> Download Button → UpdateDownloader       │
│  │    └─> Install Button → UpdateInstaller         │
│  └─> No: Continue normal app launch                │
└─────────────────────────────────────────────────────┘
```

### 🏗️ Module Structure

```
composeApp/src/commonMain/kotlin/com/ampairs/update/
├── api/
│   ├── UpdateApiModel.kt          # API request/response DTOs
│   ├── UpdateApi.kt               # API interface
│   └── UpdateApiImpl.kt           # Ktor implementation
├── domain/
│   └── UpdateInfo.kt              # Domain models & states
├── service/
│   ├── AppVersion.kt              # Version info (expect/actual)
│   ├── UpdateChecker.kt           # Check logic with rate limiting
│   ├── UpdateDownloader.kt        # Download with progress (expect/actual)
│   └── UpdateInstaller.kt         # Platform-specific install (expect/actual)
├── ui/
│   └── UpdateDialog.kt            # Material 3 UI dialog
└── di/
    └── UpdateModule.kt            # Koin dependency injection

composeApp/src/desktopMain/kotlin/com/ampairs/update/service/
├── AppVersion.desktop.kt          # Platform detection
├── UpdateDownloader.desktop.kt    # Ktor download + SHA-256 verification
└── UpdateInstaller.desktop.kt     # Open .dmg/.msi/.deb installers
```

## Features

### ✅ Implemented Features

1. **Automatic Update Checking**
   - Checks on app launch
   - Rate-limited to once per 4 hours
   - Configurable via `UpdateChecker.checkForUpdates(forceCheck: Boolean)`

2. **Smart Update Management**
   - **Mandatory updates**: User must download and install (no dismiss button)
   - **Optional updates**: User can dismiss and continue using the app
   - Dismissed updates tracked per version (won't show again for that version)

3. **Progress Tracking**
   - Real-time download progress bar
   - File size display
   - Download/Install state management

4. **Platform-Specific Installation**
   - **macOS**: Opens `.dmg` file with system `open` command
   - **Windows**: Opens `.msi` or `.exe` installer
   - **Linux**: Opens `.deb` with package manager or shows file location
   - Fallback to file manager if direct installation fails

5. **Security Features**
   - SHA-256 checksum verification (if provided by backend)
   - File integrity validation before installation

6. **Data Persistence**
   - Last check timestamp stored in DataStore
   - Dismissed versions tracked per version
   - Workspace-independent (global app setting)

## Backend API Requirements

### 🔌 API Endpoint

**Endpoint:** `GET /api/v1/app-updates/check`

**Query Parameters:**
```
platform: String       # "MACOS", "WINDOWS", "LINUX"
currentVersion: String # e.g., "1.0.0.9"
versionCode: Int       # e.g., 9
```

**Response Format:**
```json
{
  "data": {
    "update_available": true,
    "update_info": {
      "version": "1.0.0.10",
      "version_code": 10,
      "release_date": "2025-01-15T10:00:00",
      "is_mandatory": false,
      "download_url": "https://backend.com/downloads/Ampairs-1.0.0.10-macos.dmg",
      "file_size_mb": 125.5,
      "platform": "MACOS",
      "release_notes": "- New features\n- Bug fixes\n- Performance improvements",
      "min_supported_version": "1.0.0.5",
      "checksum": "sha256_hash_of_file_here"
    },
    "message": "New version available"
  },
  "error": null
}
```

**Response when no update available:**
```json
{
  "data": {
    "update_available": false,
    "update_info": null,
    "message": "You are running the latest version"
  },
  "error": null
}
```

### 🗄️ Backend Database Schema (Suggested)

```sql
CREATE TABLE app_versions (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,           -- "1.0.0.10"
    version_code INT NOT NULL,              -- 10
    platform VARCHAR(20) NOT NULL,          -- "MACOS", "WINDOWS", "LINUX"
    is_mandatory BOOLEAN DEFAULT FALSE,
    download_url TEXT NOT NULL,
    file_size_mb DECIMAL(10, 2),
    release_date TIMESTAMP,
    release_notes TEXT,
    min_supported_version VARCHAR(50),
    checksum VARCHAR(128),                   -- SHA-256 hash
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(version, platform)
);

-- Index for fast lookup
CREATE INDEX idx_app_versions_platform_active
    ON app_versions(platform, is_active, version_code DESC);
```

### 🔧 Backend Service Logic (Pseudo-code)

```kotlin
// Backend Spring Boot Controller
@GetMapping("/api/v1/app-updates/check")
fun checkForUpdates(
    @RequestParam platform: String,
    @RequestParam currentVersion: String,
    @RequestParam versionCode: Int
): Response<UpdateCheckResponse> {

    // 1. Find latest version for the platform
    val latestVersion = appVersionRepository
        .findLatestByPlatformAndActive(platform, true)

    // 2. Compare version codes
    if (latestVersion == null || latestVersion.versionCode <= versionCode) {
        return Response.success(
            UpdateCheckResponse(
                updateAvailable = false,
                updateInfo = null,
                message = "You are running the latest version"
            )
        )
    }

    // 3. Check if update is mandatory (below minimum supported version)
    val isMandatory = latestVersion.minSupportedVersion != null &&
        compareVersions(currentVersion, latestVersion.minSupportedVersion) < 0

    // 4. Build update info
    val updateInfo = UpdateInfoApiModel(
        version = latestVersion.version,
        versionCode = latestVersion.versionCode,
        releaseDate = latestVersion.releaseDate.toString(),
        isMandatory = isMandatory || latestVersion.isMandatory,
        downloadUrl = latestVersion.downloadUrl,
        fileSizeMb = latestVersion.fileSizeMb,
        platform = latestVersion.platform,
        releaseNotes = latestVersion.releaseNotes,
        minSupportedVersion = latestVersion.minSupportedVersion,
        checksum = latestVersion.checksum
    )

    return Response.success(
        UpdateCheckResponse(
            updateAvailable = true,
            updateInfo = updateInfo,
            message = "New version available"
        )
    )
}
```

## File Hosting Options

### Option 1: Direct Backend Hosting
- Store files in server filesystem or database
- Serve via Spring Boot controller
- Pros: Simple, no external dependencies
- Cons: Server bandwidth, storage costs

### Option 2: Cloud Storage (Recommended)
- Use AWS S3, Google Cloud Storage, or Azure Blob Storage
- Generate signed URLs with expiration
- Pros: CDN support, scalability, cost-effective
- Cons: Requires cloud setup

### Option 3: CDN/External URLs
- Upload to external CDN
- Store download URLs in database
- Pros: Fast downloads, no backend load
- Cons: Requires CDN management

## Usage Flow

### 🚀 User Experience

1. **App Launch**
   ```
   User opens Ampairs Desktop
   ↓
   App checks for updates (if > 4 hours since last check)
   ↓
   [No update] → App continues normally
   [Update available] → Show UpdateDialog
   ```

2. **Optional Update**
   ```
   UpdateDialog shows:
   - Version info
   - File size
   - Release notes
   - [Download Update] button
   - [Later] button

   User clicks "Later" → Dialog closes, app continues
   User clicks "Download" → Progress bar shows download
   Download complete → [Install Now] button appears
   User clicks "Install" → Opens installer
   ```

3. **Mandatory Update**
   ```
   UpdateDialog shows:
   - "⚠️ This update is mandatory" badge
   - Version info
   - Release notes
   - [Download Update] button (no dismiss option)

   User MUST download and install to continue
   ```

## Configuration

### Rate Limiting

Edit `UpdateChecker.kt`:
```kotlin
companion object {
    // Current: Check once per 4 hours
    private val CHECK_INTERVAL = 4.hours.inWholeMilliseconds

    // To change to 1 hour:
    private val CHECK_INTERVAL = 1.hours.inWholeMilliseconds

    // To disable rate limiting (check every launch):
    // Just call: updateChecker.checkForUpdates(forceCheck = true)
}
```

### Version Management

Edit `AppVersion.kt`:
```kotlin
object AppVersion {
    const val VERSION_NAME = "1.0.0.9"  // Update on new release
    const val VERSION_CODE = 9          // Increment on new release
}
```

**IMPORTANT:** Sync these values with `composeApp/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        versionCode = 9
        versionName = "1.0.0.9"
    }
}
```

## Testing

### Manual Testing Checklist

**Before Testing:**
1. Set up backend API endpoint with test data
2. Build desktop application
3. Clear DataStore preferences (for fresh state)

**Test Scenarios:**

1. **No Update Available**
   - [ ] Launch app
   - [ ] Console shows: "✅ App is up to date"
   - [ ] No dialog shown
   - [ ] App continues normally

2. **Optional Update Available**
   - [ ] Launch app
   - [ ] Dialog shows update information
   - [ ] "Later" button visible
   - [ ] Click "Later" → Dialog closes
   - [ ] Re-launch within 4 hours → No check (rate limited)
   - [ ] Re-launch after 4 hours → Check again

3. **Mandatory Update**
   - [ ] Launch app
   - [ ] Dialog shows "⚠️ This update is mandatory"
   - [ ] No "Later" button
   - [ ] Cannot dismiss dialog
   - [ ] Must download to proceed

4. **Download Flow**
   - [ ] Click "Download Update"
   - [ ] Progress bar shows download progress
   - [ ] Download completes → "✅ Download complete!"
   - [ ] "Install Now" button appears

5. **Installation Flow (Platform-Specific)**
   - **macOS:**
     - [ ] Click "Install Now"
     - [ ] DMG file opens in Finder
     - [ ] Can drag app to Applications folder

   - **Windows:**
     - [ ] Click "Install Now"
     - [ ] MSI installer opens
     - [ ] Can follow installation wizard

   - **Linux:**
     - [ ] Click "Install Now"
     - [ ] Package manager opens (or file location shown)
     - [ ] Can install .deb package

6. **Checksum Verification (if backend provides checksum)**
   - [ ] Download file with valid checksum → Success
   - [ ] Download file with invalid checksum → Error shown

7. **Error Handling**
   - [ ] Network error → Console shows error, no crash
   - [ ] Invalid download URL → Error shown in dialog
   - [ ] Failed installer open → Error message shown

## Troubleshooting

### Common Issues

**Issue: Update check not triggered**
- **Solution:** Check last check timestamp in DataStore. Force check: `updateChecker.checkForUpdates(forceCheck = true)`

**Issue: Download fails**
- **Solution:** Verify download URL is accessible. Check network connectivity. Review console logs.

**Issue: Installer doesn't open**
- **Solution:** Check file exists in Downloads folder. Verify file permissions. Try opening manually.

**Issue: Checksum verification fails**
- **Solution:** Verify backend provides correct SHA-256 hash. Re-download file.

### Debug Logging

All update operations log to console:
```
🚀 Desktop app launched - checking for updates...
🔍 Checking for updates...
   Platform: MACOS
   Current version: 1.0.0.9 (9)
✅ Update available: 1.0.0.10
   Mandatory: false
   Size: 125.5 MB
📥 Starting update download...
   URL: https://...
   Size: 125.5 MB
🔐 Verifying checksum...
✅ Checksum verified
✅ Download complete: /Users/.../Downloads/Ampairs-1.0.0.10.dmg
🔧 Starting update installation...
   File: /Users/.../Downloads/Ampairs-1.0.0.10.dmg
   Version: 1.0.0.10
🍎 Opening macOS installer...
   DMG mounted. Please drag app to Applications folder.
✅ Installer opened successfully
```

## Future Enhancements

### Potential Features (Not Yet Implemented)

1. **Auto-Install on Desktop**
   - Silent installation without user interaction
   - Requires elevated permissions
   - Platform-specific implementation complexity

2. **Background Downloads**
   - Download updates in background
   - Notify user when ready to install
   - Requires persistent background service

3. **Delta Updates**
   - Only download changed files
   - Reduce download size
   - Requires binary diff/patch system

4. **Rollback Support**
   - Keep previous version for rollback
   - Automatic rollback on crash
   - Requires version management system

5. **Analytics & Telemetry**
   - Track update adoption rates
   - Monitor download success/failure
   - Identify problematic versions

6. **A/B Testing**
   - Roll out updates to percentage of users
   - Test stability before full release
   - Requires backend feature flags

## Dependencies

### Added to Project

- **kotlinx.datetime**: Cross-platform time handling
- **Ktor CIO engine**: File downloads (desktop)
- **DataStore Preferences**: Persistent storage
- **Koin**: Dependency injection

### Platform-Specific

- **JVM/Desktop:**
  - `java.security.MessageDigest`: SHA-256 checksum
  - `java.io.File`: File operations
  - `java.awt.Desktop`: Open files/directories

## Security Considerations

1. **HTTPS Only**: Ensure all download URLs use HTTPS
2. **Checksum Verification**: Always provide SHA-256 checksums
3. **Code Signing**: Sign release binaries (platform-specific)
4. **Update Authentication**: Consider JWT/API key for update API (if needed)
5. **Malware Scanning**: Scan uploaded binaries before distribution

## Maintenance

### Release Process

1. **Build new version:**
   ```bash
   ./gradlew composeApp:packageDistributionForCurrentOS
   ```

2. **Update version in code:**
   - Update `AppVersion.VERSION_NAME` and `VERSION_CODE`
   - Update `composeApp/build.gradle.kts` version values

3. **Upload to backend:**
   - Upload platform-specific binaries (DMG, MSI, DEB)
   - Calculate SHA-256 checksum: `sha256sum file.dmg`
   - Create database entry with version info

4. **Test update flow:**
   - Run app with older version
   - Verify update dialog appears
   - Test download and installation

5. **Monitor rollout:**
   - Check backend logs for update requests
   - Monitor error rates
   - Verify user adoption

## Contact & Support

For backend implementation assistance or questions about the update system:
- Review this document
- Check console logs for detailed debug information
- Test API endpoint manually using curl or Postman

---

**Implementation Date:** January 2025
**Platform:** Kotlin Multiplatform (KMP)
**Supported Platforms:** macOS, Windows, Linux (Desktop)
