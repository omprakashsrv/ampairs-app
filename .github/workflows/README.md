# GitHub Actions Build Pipeline

This repository includes automated build pipelines for creating platform-specific installers for Ampairs application.

## 📦 Build Outputs

The pipeline builds the following installers:

- **macOS**: DMG installer
- **Windows**: MSI installer
- **Linux**: DEB installer
- **Android**: APK and AAB files

## 🚀 Triggering Builds

Builds are automatically triggered on:

- **Push** to `main` or `develop` branches
- **Pull requests** targeting `main` or `develop`
- **Tag pushes** (e.g., `v1.0.0`) - Creates a GitHub Release with all artifacts
- **Manual trigger** via GitHub Actions UI

## 🔐 Required GitHub Secrets

To enable Android signing, you must configure the following secrets in your GitHub repository:

### Setting Up Secrets

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add the following secrets:

### Android Signing Secrets

| Secret Name | Description | How to Generate |
|------------|-------------|-----------------|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded keystore file | See instructions below |
| `KEYSTORE_PASSWORD` | Keystore password | From your `build.gradle.kts` (currently: `SKFNNFJ234329898g723g47823gr8`) |
| `KEY_PASSWORD` | Key password | Same as keystore password |
| `KEY_ALIAS` | Key alias | From your `build.gradle.kts` (currently: `ampairs`) |

### Generating Base64-Encoded Keystore

Run this command in your project root directory:

```bash
base64 -i ampairs.jks | pbcopy  # macOS - copies to clipboard
# OR
base64 -i ampairs.jks           # Linux - prints to terminal
# OR
certutil -encode ampairs.jks keystore.txt  # Windows
```

Then paste the base64 string as the `ANDROID_KEYSTORE_BASE64` secret.

## 📋 Build Process

### Platform-Specific Runners

Each platform uses a specific GitHub runner:

- **macOS DMG**: `macos-latest` (required for DMG packaging)
- **Windows MSI**: `windows-latest` (required for MSI packaging)
- **Linux DEB**: `ubuntu-latest` (standard Linux runner)
- **Android**: `ubuntu-latest` (cost-effective for Android builds)

### Build Commands

The pipeline executes these Gradle tasks:

```bash
# Desktop platforms
./gradlew packageDmg    # macOS only
./gradlew packageMsi    # Windows only
./gradlew packageDeb    # Linux only

# Android
./gradlew composeApp:assembleRelease  # APK
./gradlew composeApp:bundleRelease    # AAB
```

## 📥 Downloading Build Artifacts & Creating Releases

### Method 1: Manual Release Creation (Recommended)

This is the easiest way to create a release with downloadable installers:

1. Go to **Actions** tab in your GitHub repository
2. Click on **Manual Release Creation** workflow
3. Click **Run workflow** button (top right)
4. Fill in the form:
   - **Version**: e.g., `v1.0.0`
   - **Release name**: e.g., `Ampairs v1.0.0 - Production Release`
   - **Pre-release**: Check if this is a beta/alpha version
   - **Draft**: Check to create a draft (not publicly visible)
5. Click **Run workflow**
6. Wait for all builds to complete (~15-30 minutes)
7. Go to **Releases** tab to see your new release with all installers attached

### Method 2: Automatic Release on Tag Push

Push a version tag to automatically create a release:

```bash
# Create and push a tag
git tag v1.0.0
git push origin v1.0.0

# The workflow will automatically:
# 1. Build all platform installers
# 2. Create a GitHub Release
# 3. Attach all installers to the release
```

### Method 3: Download from Workflow Runs (Without Release)

If you just want to test builds without creating a release:

1. Go to **Actions** tab in your repository
2. Click on any workflow run (e.g., "Build Multi-Platform Installers")
3. Scroll to **Artifacts** section at the bottom
4. Download the desired artifact:
   - `Ampairs-macOS-DMG` - macOS installer
   - `Ampairs-Windows-MSI` - Windows installer
   - `Ampairs-Linux-DEB` - Linux installer
   - `Ampairs-Android-APK` - Android APK file
   - `Ampairs-Android-AAB` - Android App Bundle

**Note**: Artifacts are kept for 30 days, then automatically deleted.

### Sharing Releases with Users

Once a release is created:

1. Go to **Releases** tab
2. Click on the release version
3. Share the release URL with users
4. Users can download installers directly from the release page

**Example Release URL**: `https://github.com/yourusername/ampairs-app/releases/tag/v1.0.0`

## 🛠️ Workflow Structure

```
build-installers.yml
├── build-macos (macOS DMG)
├── build-windows (Windows MSI)
├── build-linux (Linux DEB)
├── build-android (APK + AAB)
└── create-release (on tag pushes)
    └── Uploads all artifacts to GitHub Release
```

## ⚙️ Gradle Caching

The workflow uses GitHub Actions cache to speed up builds:

- Gradle dependencies cache
- Gradle wrapper cache
- Kotlin/Native (Konan) cache

This reduces build times on subsequent runs.

## 🔧 Customization

### Changing Java Version

Update the `JAVA_VERSION` environment variable in the workflow:

```yaml
env:
  JAVA_VERSION: '21'  # Change to desired version
```

### Modifying Retention Days

Artifacts are kept for 30 days by default. Change in each job:

```yaml
- name: Upload artifact
  uses: actions/upload-artifact@v4
  with:
    retention-days: 30  # Change to desired days (1-90)
```

### Adding Additional Build Types

To build debug variants or additional configurations:

```yaml
- name: Build Debug APK
  run: ./gradlew composeApp:assembleDebug
```

## 🐛 Troubleshooting

### Build Fails on Android Signing

**Problem**: "Keystore file not found" or signing errors

**Solution**:
1. Verify `ANDROID_KEYSTORE_BASE64` secret is set correctly
2. Ensure the base64 encoding doesn't have line breaks
3. Check that `ampairs.jks` exists in your repository root

### macOS Build Fails

**Problem**: "Command not found: packageDmg"

**Solution**: Ensure the job runs on `macos-latest` runner. DMG packaging requires macOS.

### Windows Build Fails

**Problem**: MSI packaging errors

**Solution**: Ensure the job runs on `windows-latest` runner. MSI packaging requires Windows.

### Gradle Build Fails

**Problem**: Build fails with dependency resolution errors

**Solution**:
1. Clear Actions cache: Settings → Actions → Caches → Delete all caches
2. Re-run the workflow
3. Check `build.gradle.kts` for dependency conflicts

## 📝 Notes

- **Artifact Retention**: Artifacts are kept for 30 days before automatic deletion
- **Concurrent Builds**: All platform builds run in parallel for faster completion
- **Release Automation**: Tag pushes automatically create GitHub Releases with all artifacts
- **Cost Optimization**: Android builds use `ubuntu-latest` (free tier) instead of macOS runners

## 🔗 References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Compose Multiplatform Packaging](https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
