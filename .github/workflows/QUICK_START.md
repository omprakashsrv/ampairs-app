# Quick Start Guide - GitHub Actions Build Pipeline

## 🎯 What's Been Set Up

You now have two automated build workflows:

1. **Build Multi-Platform Installers** - Automatic builds on main branch commits
2. **Manual Release Creation** - On-demand release creation with custom version

## 🚀 Getting Started (First-Time Setup)

### Step 1: Configure GitHub Secrets (5 minutes)

Before running any builds, set up Android signing secrets:

1. Navigate to: `Settings` → `Secrets and variables` → `Actions`
2. Add these 4 secrets:

```bash
# Encode your keystore first
cd /Users/omprakashsrv/StudioProjects/ampairs-app
base64 -i ampairs.jks | pbcopy  # Copies to clipboard
```

| Secret Name | Value |
|------------|-------|
| `ANDROID_KEYSTORE_BASE64` | Paste the base64 output from above |
| `KEYSTORE_PASSWORD` | `SKFNNFJ234329898g723g47823gr8` |
| `KEY_PASSWORD` | `SKFNNFJ234329898g723g47823gr8` |
| `KEY_ALIAS` | `ampairs` |

📖 **Detailed instructions**: See [SETUP_SECRETS.md](./SETUP_SECRETS.md)

### Step 2: Push Changes to GitHub

```bash
# Commit the workflow files
git add .github/workflows/
git commit -m "Add GitHub Actions build pipeline for multi-platform installers"

# Push to main branch
git push origin main
```

**Note**: The workflow will automatically run after pushing to main!

## 📦 Creating Your First Release

### Option A: Manual Release (Recommended for first release)

1. Go to **Actions** tab on GitHub
2. Click **Manual Release Creation**
3. Click **Run workflow** button
4. Fill in:
   - Version: `v1.0.0`
   - Release name: `Ampairs v1.0.0 - Initial Release`
   - Pre-release: Unchecked
   - Draft: Checked (to review before publishing)
5. Click **Run workflow**
6. Wait ~15-30 minutes for all builds to complete
7. Go to **Releases** tab
8. Edit the draft release, review, and publish

### Option B: Tag-Based Release (Automated)

```bash
# Create and push a version tag
git tag v1.0.0
git push origin v1.0.0

# Workflow automatically:
# - Builds all platforms
# - Creates GitHub Release
# - Attaches installers
```

## 📋 Build Triggers

### Automatic Builds

The workflow automatically runs when you:

- ✅ Push commits to `main` branch
- ✅ Push version tags (e.g., `v1.0.0`, `v2.1.3`)

### Manual Builds

You can manually trigger builds:

- ✅ Via **Actions** tab → **Run workflow** button

## 📥 Downloading Built Packages

### From Releases (For End Users)

1. Go to **Releases** tab
2. Click on latest release
3. Download installer for your platform:
   - **macOS**: `.dmg` file
   - **Windows**: `.msi` file
   - **Linux**: `.deb` file
   - **Android**: `.apk` file

### From Workflow Artifacts (For Testing)

1. Go to **Actions** tab
2. Click on any workflow run
3. Scroll to **Artifacts** section
4. Download artifacts (available for 30 days)

## 🛠️ Build Platforms & Runners

| Platform | Output | Runner | Build Time |
|----------|--------|--------|------------|
| macOS | `.dmg` | `macos-latest` | ~8-12 min |
| Windows | `.msi` | `windows-latest` | ~8-12 min |
| Linux | `.deb` | `ubuntu-latest` | ~6-10 min |
| Android APK | `.apk` | `ubuntu-latest` | ~5-8 min |
| Android AAB | `.aab` | `ubuntu-latest` | ~5-8 min |

**Total Pipeline Time**: ~15-30 minutes (runs in parallel)

## 🎯 Common Workflows

### Releasing a New Version

```bash
# 1. Update version in code
# Update version in build.gradle.kts (line 259)
# packageVersion = "1.0.1"

# 2. Commit changes
git add composeApp/build.gradle.kts
git commit -m "Bump version to 1.0.1"
git push origin main

# 3. Create and push tag
git tag v1.0.1
git push origin v1.0.1

# 4. Workflow automatically creates release with all installers
```

### Testing Build Without Release

```bash
# Simply push to main branch
git push origin main

# Builds run automatically
# Download from Actions → Artifacts
# No release is created
```

### Creating Pre-release/Beta

1. Go to **Actions** → **Manual Release Creation**
2. Run workflow with:
   - Version: `v1.0.0-beta.1`
   - Pre-release: ✅ Checked
3. Users see it marked as "Pre-release"

## 🔍 Monitoring Builds

### View Build Progress

1. Go to **Actions** tab
2. Click on running workflow
3. See real-time logs for each platform

### Build Notifications

GitHub sends email notifications when:
- ✅ Build succeeds
- ❌ Build fails

Configure in: `Settings` → `Notifications`

## ❌ Troubleshooting

### Build Fails: "Keystore not found"

**Fix**: Verify `ANDROID_KEYSTORE_BASE64` secret is set correctly

```bash
# Re-encode keystore without line breaks
base64 -i ampairs.jks | tr -d '\n'
# Update secret with new value
```

### Build Fails: Platform-specific error

- **macOS errors**: Check `build-macos` job logs
- **Windows errors**: Check `build-windows` job logs
- **Linux errors**: Check `build-linux` job logs
- **Android errors**: Check `build-android` job logs

### Release Not Created

**Cause**: `create-release` job only runs on tag pushes

**Fix**: Either:
- Use **Manual Release Creation** workflow
- Push a version tag: `git tag v1.0.0 && git push origin v1.0.0`

## 📊 Build Status Badge (Optional)

Add this to your README.md to show build status:

```markdown
[![Build Status](https://github.com/yourusername/ampairs-app/actions/workflows/build-installers.yml/badge.svg)](https://github.com/yourusername/ampairs-app/actions)
```

## 📚 Additional Documentation

- [README.md](./README.md) - Complete pipeline documentation
- [SETUP_SECRETS.md](./SETUP_SECRETS.md) - Detailed secrets setup guide

## ✅ Verification Checklist

Before creating your first release:

- [ ] GitHub secrets configured (4 secrets)
- [ ] Workflow files pushed to repository
- [ ] Test build completed successfully
- [ ] All 4 platform builds succeeded
- [ ] Artifacts downloadable from workflow run
- [ ] Android APK signed correctly

## 🎉 You're Ready!

Your CI/CD pipeline is now fully configured. Every commit to `main` will automatically build all platform installers, and you can create releases with a single click.

**Next Steps**:
1. Push workflow files to GitHub
2. Configure secrets
3. Run test build
4. Create your first release!
