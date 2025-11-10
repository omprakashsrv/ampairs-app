# CI/CD Pipeline for Desktop App Updates

Automated release pipeline for publishing desktop app updates to S3 and registering them in the database.

## Table of Contents
- [Overview](#overview)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [GitHub Actions Setup](#github-actions-setup)
- [Manual Publishing](#manual-publishing)
- [Version Numbering](#version-numbering)
- [Testing the Pipeline](#testing-the-pipeline)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

---

## Overview

The CI/CD pipeline automates the following tasks when you push a version tag:

1. ✅ **Build** desktop apps for macOS, Windows, and Linux (in parallel)
2. ✅ **Calculate** SHA-256 checksums for file integrity
3. ✅ **Upload** binaries to private S3 bucket
4. ✅ **Register** version metadata in database via API
5. ✅ **Create** GitHub releases with download links
6. ✅ **Notify** users through in-app update checker

### Security Features
- 🔒 Private S3 bucket (no public URLs)
- 🔐 Backend-controlled file streaming
- ⏱️ Rate limiting (1 download per 10 seconds)
- ✅ Checksum verification on client side
- 🔑 Admin-only API access for publishing

---

## Quick Start

### 1. First-Time Setup

**a) Configure GitHub Secrets** (Settings → Secrets and variables → Actions):

```
AWS_ACCESS_KEY_ID          - IAM user with S3 write access
AWS_SECRET_ACCESS_KEY      - IAM secret key
AMPAIRS_ADMIN_TOKEN        - Admin JWT token from backend
```

**b) IAM Policy** for GitHub Actions (in AWS):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:PutObjectAcl",
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::ampairs-app-updates/updates/*"
    }
  ]
}
```

### 2. Create a Release

**Step 1: Update version in code**

Edit `composeApp/src/commonMain/kotlin/com/ampairs/update/service/AppVersion.kt`:

```kotlin
object AppVersion {
    const val VERSION_NAME = "1.0.0.10"  // Update this
    const val VERSION_CODE = 10          // Update this
}
```

Also update `composeApp/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        versionCode = 10
        versionName = "1.0.0.10"
    }
}
```

**Step 2: Commit changes**

```bash
git add .
git commit -m "Bump version to 1.0.0.10"
git push origin main
```

**Step 3: Create annotated tag with release notes**

```bash
git tag -a v1.0.0.10 -m "Release 1.0.0.10

## What's New
- New feature: Dark mode support
- New feature: In-app update system
- Bug fix: Fixed crash on startup
- Performance: Improved loading times by 30%

## Breaking Changes
- Minimum supported version: 1.0.0.5

## Technical Details
- Added kotlinx-datetime dependency
- Migrated to Ktor 3.x
- Updated Material 3 components"
```

**Step 4: Push the tag**

```bash
git push origin v1.0.0.10
```

**Step 5: Monitor the workflow**

1. Go to your GitHub repository
2. Click "Actions" tab
3. Watch the "Release Desktop App" workflow
4. All 3 platforms build in parallel (~15-30 minutes)

**Step 6: Verify release**

Once workflow completes:
- ✅ Check GitHub Releases for the new version
- ✅ Verify files in S3: `aws s3 ls s3://ampairs-app-updates/updates/`
- ✅ Check database entries via API
- ✅ Test update check from desktop app

---

## Architecture

```
┌─────────────────┐
│  Git Tag Push   │
│   (v1.0.0.10)   │
└────────┬────────┘
         │
         v
┌──────────────────────────────────────────────────┐
│             CI/CD Pipeline (GitHub Actions)       │
│                                                   │
│  Stage 1: Prepare                                 │
│    - Extract version from tag                     │
│    - Extract release notes                        │
│                                                   │
│  Stage 2: Build (Parallel)                        │
│    ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│    │  macOS   │  │ Windows  │  │  Linux   │     │
│    │   DMG    │  │   MSI    │  │   DEB    │     │
│    └────┬─────┘  └────┬─────┘  └────┬─────┘     │
│         │             │             │            │
│  Stage 3: Publish (Parallel)                      │
│    ├─ Calculate SHA-256 checksum                  │
│    ├─ Upload to S3 (private)                      │
│    └─ Register in database via API                │
│                                                   │
│  Stage 4: Release                                 │
│    - Create GitHub Release                        │
│    - Attach binaries as assets                    │
│                                                   │
│  Stage 5: Notify                                  │
│    - Log completion status                        │
│    - Optional: Slack/Discord notification         │
└──────────────────────────────────────────────────┘
         │
         v
┌──────────────────────────────────────────────────┐
│               Infrastructure                      │
│                                                   │
│  ┌──────────────┐      ┌──────────────────────┐ │
│  │  S3 Bucket   │◄─────┤   Spring Boot        │ │
│  │  (Private)   │      │   Backend            │ │
│  │              │      │   (Streams files)    │ │
│  └──────────────┘      └──────────┬───────────┘ │
│                                   │              │
│                                   v              │
│                        ┌──────────────────────┐ │
│                        │    PostgreSQL        │ │
│                        │    (app_versions)    │ │
│                        └──────────────────────┘ │
└──────────────────────────────────────────────────┘
         │
         v
┌──────────────────────────────────────────────────┐
│             Desktop Clients (macOS/Win/Linux)     │
│                                                   │
│  1. Check for updates (GET /api/v1/app-updates/  │
│     check?platform=MACOS&currentVersion=1.0.0.9) │
│  2. Download via backend (streamed from S3)       │
│  3. Verify SHA-256 checksum                       │
│  4. Open platform-specific installer              │
│  5. User completes installation                   │
└──────────────────────────────────────────────────┘
```

---

## GitHub Actions Setup

### Workflow File

Located at: `.github/workflows/release-desktop-app.yml`

### Workflow Stages

#### Stage 1: Prepare (1-2 minutes)
- Extracts version from git tag (e.g., `v1.0.0.10` → `1.0.0.10`)
- Extracts version code (e.g., `1.0.0.10` → `10`)
- Reads release notes from tag annotation
- Creates artifacts for next stages

#### Stage 2: Build (15-30 minutes, parallel)

**macOS Job (runs on macos-latest):**
- Sets up JDK 21
- Caches Gradle dependencies
- Runs `./gradlew composeApp:packageDmg`
- Finds `.dmg` file in build output
- Uploads as artifact

**Windows Job (runs on windows-latest):**
- Sets up JDK 21
- Caches Gradle dependencies
- Runs `./gradlew composeApp:packageMsi`
- Finds `.msi` file in build output
- Uploads as artifact

**Linux Job (runs on ubuntu-latest):**
- Sets up JDK 21
- Caches Gradle dependencies
- Runs `./gradlew composeApp:packageDeb`
- Finds `.deb` file in build output
- Uploads as artifact

#### Stage 3: Publish (5-10 minutes, parallel)

For each platform:
1. Downloads build artifact
2. Downloads release notes
3. Configures AWS credentials
4. Runs `publish-app-update.sh` script:
   - Calculates SHA-256 checksum
   - Calculates file size in MB
   - Uploads to S3 private bucket
   - Calls backend API to register version
5. Uploads to GitHub Release (backup)

#### Stage 4: Create Release (1-2 minutes)

- Downloads all artifacts
- Creates GitHub Release with:
  - Tag name (e.g., `v1.0.0.10`)
  - Release title (e.g., `Release 1.0.0.10`)
  - Release notes from tag annotation
  - Attached binaries (DMG, MSI, DEB)

#### Stage 5: Notify (< 1 minute)

- Logs success/failure status
- Optional: Sends notifications to Slack/Discord

### Expected Output

After successful workflow run:

**S3 Bucket:**
```
s3://ampairs-app-updates/updates/
├── macos-1.0.0.10.dmg (125.5 MB)
├── windows-1.0.0.10.msi (115.3 MB)
└── linux-1.0.0.10.deb (110.8 MB)
```

**Database (app_versions table):**
```sql
SELECT version, platform, is_active, version_code, file_size_mb
FROM app_versions
WHERE version = '1.0.0.10';

-- Result:
-- version   | platform | is_active | version_code | file_size_mb
-- 1.0.0.10  | MACOS    | true      | 10           | 125.50
-- 1.0.0.10  | WINDOWS  | true      | 10           | 115.30
-- 1.0.0.10  | LINUX    | true      | 10           | 110.80
```

**GitHub Release:**
- URL: `https://github.com/your-org/ampairs-app/releases/tag/v1.0.0.10`
- Assets: 3 binary files
- Release notes displayed

**User Impact:**
- Users receive update notification within 4 hours (rate limiting)
- Dialog shows version info, release notes, file size
- Download begins when user clicks "Download Update"

---

## Manual Publishing

Use the helper script for manual releases or other CI/CD systems.

### Script Location

`scripts/publish-app-update.sh`

### Prerequisites

```bash
# Required environment variables
export AMPAIRS_ADMIN_TOKEN="your-jwt-token-here"
export AWS_ACCESS_KEY_ID="your-aws-access-key"
export AWS_SECRET_ACCESS_KEY="your-aws-secret-key"

# Optional overrides
export API_BASE_URL="https://api.ampairs.in"  # default
export S3_BUCKET="ampairs-app-updates"        # default
export AWS_REGION="ap-south-1"                # default
```

### Basic Usage

```bash
./scripts/publish-app-update.sh <file> <version> <platform>
```

### Examples

**1. Basic release:**

```bash
./scripts/publish-app-update.sh \
  Ampairs-1.0.0.10.dmg \
  1.0.0.10 \
  MACOS
```

**2. Mandatory update with minimum version:**

```bash
./scripts/publish-app-update.sh \
  Ampairs-1.0.0.11.dmg \
  1.0.0.11 \
  MACOS \
  --mandatory \
  --min-version 1.0.0.5
```

**3. With release notes from file:**

```bash
# Create release notes file
cat > release-notes.md <<EOF
## What's New
- New feature: Dark mode
- Bug fix: Fixed crash
- Performance improvements

## Breaking Changes
- Minimum version: 1.0.0.5
EOF

./scripts/publish-app-update.sh \
  Ampairs-1.0.0.12.msi \
  1.0.0.12 \
  WINDOWS \
  --release-notes release-notes.md
```

**4. Dry run (validation only):**

```bash
./scripts/publish-app-update.sh \
  Ampairs-1.0.0.13.deb \
  1.0.0.13 \
  LINUX \
  --dry-run
```

### Script Options

| Option | Description | Example |
|--------|-------------|---------|
| `--mandatory` | Mark as mandatory update | `--mandatory` |
| `--min-version <ver>` | Set minimum supported version | `--min-version 1.0.0.5` |
| `--release-notes <file>` | Path to markdown file with notes | `--release-notes notes.md` |
| `--dry-run` | Validate only, don't upload | `--dry-run` |
| `--api-url <url>` | Override API base URL | `--api-url https://staging.ampairs.in` |
| `--s3-bucket <name>` | Override S3 bucket name | `--s3-bucket my-bucket` |

### Script Features

- ✅ Validates all inputs before uploading
- ✅ Checks file existence and extension
- ✅ Calculates SHA-256 checksum automatically
- ✅ Calculates file size in MB
- ✅ Extracts version code from version string
- ✅ Supports markdown release notes
- ✅ Interactive confirmation (skipped in CI)
- ✅ Colored output for easy monitoring
- ✅ Comprehensive error handling
- ✅ Detailed logging of each step

---

## Version Numbering

### Format: `MAJOR.MINOR.PATCH.BUILD`

Example: `1.0.0.10`

- **MAJOR (1)**: Breaking changes, major new features
- **MINOR (0)**: New features (backward compatible)
- **PATCH (0)**: Bug fixes, small improvements
- **BUILD (10)**: Incremental build number (used as version_code)

### Rules

1. **Always increment BUILD** for new releases
2. **Never reuse version numbers**
3. **Use semantic versioning** for clarity
4. **Update both** `AppVersion.kt` and `build.gradle.kts`

### Version Code

- **Purpose**: Simplified integer comparison
- **Extraction**: Last component of version string
- **Example**: `1.0.0.10` → version_code = `10`
- **Usage**: Client compares version codes to determine if update available

### Git Tags

- **Format**: `v{VERSION}` (must start with `v`)
- **Example**: `v1.0.0.10`
- **Validation**: Must have 4 numeric components
- **Annotation**: Use `-a` flag with release notes

**Creating an annotated tag:**

```bash
git tag -a v1.0.0.10 -m "Release 1.0.0.10

## What's New
- Feature description
- Bug fix description

## Breaking Changes
- Change description"
```

---

## Testing the Pipeline

### 1. Local Testing (Script Only)

Test the publish script without triggering CI/CD:

```bash
# Create test file
dd if=/dev/zero of=test.dmg bs=1M count=10

# Dry run (validation only)
./scripts/publish-app-update.sh \
  test.dmg \
  1.0.0.999 \
  MACOS \
  --dry-run

# Expected output:
# ✅ Validation passed
# ✅ Checksum: abc123...
# ✅ File size: 10.00 MB
# ⚠️  DRY RUN MODE - No changes will be made
```

### 2. Test S3 Upload

```bash
# Real upload test (will create actual S3 file)
./scripts/publish-app-update.sh \
  test.dmg \
  1.0.0.999 \
  MACOS

# Verify in S3
aws s3 ls s3://ampairs-app-updates/updates/ | grep 999

# Expected output:
# 2025-01-15 10:00:00   10485760 macos-1.0.0.999.dmg

# Clean up
aws s3 rm s3://ampairs-app-updates/updates/macos-1.0.0.999.dmg
curl -X DELETE \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.ampairs.in/api/v1/app-updates/{version-id}
```

### 3. Test Database Registration

```bash
# Check all versions
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.ampairs.in/api/v1/app-updates \
  | jq '.data[] | {version, platform, is_active}'

# Expected output:
# {
#   "version": "1.0.0.999",
#   "platform": "MACOS",
#   "is_active": true
# }
```

### 4. Test Update Check (Client Perspective)

```bash
# Simulate client checking for updates
curl "https://api.ampairs.in/api/v1/app-updates/check?platform=MACOS&currentVersion=1.0.0.8&versionCode=8" \
  | jq '.'

# Expected output:
# {
#   "data": {
#     "update_available": true,
#     "update_info": {
#       "version": "1.0.0.999",
#       "version_code": 999,
#       ...
#     }
#   }
# }
```

### 5. Test Full Pipeline (GitHub Actions)

**Option A: Test on feature branch**

```bash
# Create test tag on feature branch
git checkout -b test-release
git tag -a v1.0.0.999 -m "Test release"
git push origin test-release v1.0.0.999

# Monitor workflow in GitHub Actions
# Delete tag when done:
git tag -d v1.0.0.999
git push origin :refs/tags/v1.0.0.999
```

**Option B: Use workflow_dispatch (manual trigger)**

1. Go to GitHub Actions
2. Select "Release Desktop App" workflow
3. Click "Run workflow"
4. Enter version manually
5. Monitor execution

---

## Troubleshooting

### Build Fails

**Issue: `packageDmg` task not found**

```bash
# Check available tasks
./gradlew tasks --all | grep package

# Verify Compose Desktop plugin
grep "org.jetbrains.compose" build.gradle.kts
```

**Issue: Java version mismatch**

```bash
# Check Java version
java -version  # Should be Java 21

# GitHub Actions uses setup-java@v4
# Ensure JAVA_VERSION env var is '21'
```

**Issue: Out of memory during build**

```kotlin
// Add to gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

### Upload Fails

**Issue: S3 Access Denied**

```bash
# Verify IAM permissions
aws s3 ls s3://ampairs-app-updates/ --debug

# Check IAM policy includes:
# - s3:PutObject
# - s3:PutObjectAcl
# - s3:GetObject

# Verify bucket exists
aws s3 ls | grep ampairs-app-updates
```

**Issue: Invalid AWS credentials**

```bash
# Verify credentials work
aws sts get-caller-identity

# Check GitHub Secrets are set:
# AWS_ACCESS_KEY_ID
# AWS_SECRET_ACCESS_KEY

# Ensure no extra spaces in secrets
```

### Registration Fails

**Issue: 401 Unauthorized**

```bash
# Test admin token
curl -H "Authorization: Bearer $AMPAIRS_ADMIN_TOKEN" \
  https://api.ampairs.in/api/v1/app-updates

# If fails, generate new token from backend
```

**Issue: Version already exists**

```bash
# Check existing versions
curl -H "Authorization: Bearer $TOKEN" \
  https://api.ampairs.in/api/v1/app-updates \
  | jq '.data[] | select(.version == "1.0.0.10")'

# Options:
# 1. Increment version number
# 2. Delete existing version (not recommended)
# 3. Deactivate old version first
```

**Issue: Invalid JSON in request**

```bash
# Check script for JSON escaping
# Release notes with quotes need escaping
# Script uses `jq -Rs .` for proper escaping
```

### GitHub Actions Fails

**Issue: Secret not found**

1. Go to GitHub → Settings → Secrets and variables → Actions
2. Add missing secrets:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AMPAIRS_ADMIN_TOKEN`
3. Ensure secrets are set for the correct repository

**Issue: Artifact not found**

```bash
# Check find command in workflow
# Verify build output path matches

# For debugging, add step:
- name: List build output
  run: |
    find composeApp/build -type f \( -name "*.dmg" -o -name "*.msi" -o -name "*.deb" \)
```

**Issue: Workflow not triggered**

```bash
# Verify tag format
git tag -l  # Must start with 'v'

# Check workflow trigger
# Should have:
# on:
#   push:
#     tags:
#       - 'v*'

# Re-push tag if needed
git push origin v1.0.0.10
```

---

## Best Practices

### Before Release

1. ✅ **Test locally** on all platforms
2. ✅ **Update version** in code (`AppVersion.kt`, `build.gradle.kts`)
3. ✅ **Write clear release notes** with breaking changes
4. ✅ **Test update flow** from previous version
5. ✅ **Review database** for existing versions
6. ✅ **Check S3 storage** costs and limits

### During Release

1. ✅ **Use annotated tags** (`git tag -a`)
2. ✅ **Monitor workflow** progress in GitHub Actions
3. ✅ **Watch for errors** in each stage
4. ✅ **Verify artifacts** are created correctly
5. ✅ **Check file sizes** are reasonable
6. ✅ **Ensure checksums** are calculated

### After Release

1. ✅ **Verify S3 upload** completed
2. ✅ **Check database entries** are active
3. ✅ **Test update check** from client
4. ✅ **Download and verify** checksums
5. ✅ **Test installation** on each platform
6. ✅ **Monitor error rates** in backend logs
7. ✅ **Update documentation** if needed
8. ✅ **Announce release** to users

### Security

1. 🔒 **S3 bucket is private** (no public-read ACL)
2. 🔑 **IAM user has minimal permissions** (S3 only)
3. 🔄 **Rotate admin tokens** regularly (every 90 days)
4. 🔐 **Store secrets securely** (GitHub Secrets, never in code)
5. 📝 **CI/CD logs don't expose secrets** (use masked values)
6. ⏱️ **Rate limiting enabled** (backend: 1 req/10s)
7. ✅ **Checksums verified** on client side
8. 🌐 **Downloads streamed** through backend (not direct S3)
9. 📊 **S3 access logging** enabled
10. 📜 **CloudTrail logs** all API calls

### Monitoring

1. 📊 **Set up CloudWatch alarms** for S3 costs
2. 📈 **Monitor download success rates** via backend logs
3. 🔍 **Track update adoption** by version
4. ⚠️ **Alert on high error rates** (>5%)
5. 💰 **Review monthly S3 costs**
6. 🗄️ **Clean up old versions** (keep last 5)

---

## Support & Resources

### Documentation
- **Backend API**: `BACKEND_UPDATE_API_IMPLEMENTATION.md`
- **Client Implementation**: `DESKTOP_UPDATE_SYSTEM.md`
- **GitHub Workflow**: `.github/workflows/release-desktop-app.yml`
- **Publish Script**: `scripts/publish-app-update.sh`

### Quick Links
- **S3 Console**: https://console.aws.amazon.com/s3/buckets/ampairs-app-updates
- **GitHub Actions**: https://github.com/your-org/ampairs-app/actions
- **API Docs**: https://api.ampairs.in/swagger-ui.html

### Getting Help

For issues or questions:
1. Check this documentation
2. Review GitHub Actions logs
3. Test with `--dry-run` flag
4. Verify all secrets are set correctly
5. Check backend logs for API errors

---

**Generated with Claude Code** • Last updated: January 2025
