#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Ampairs App — Release Tagging Script
# =============================================================================
#
# Usage: ./scripts/release.sh <version> <message>
# Example: ./scripts/release.sh 1.0.16 "Release 1.0.16 — Payments & Printing"
#
# This script ONLY creates and pushes the annotated git tag. The version bump,
# commit, and push of source changes must already be done (see steps below).
#
# -----------------------------------------------------------------------------
# FULL RELEASE PROCEDURE (do these BEFORE running this script)
# -----------------------------------------------------------------------------
# For a release version X.Y.Z (e.g. 1.0.16), versionCode = X*10000 + Y*100 + Z
# padded as 1000ZZ (e.g. 1.0.16 -> 100016, 1.0.15 -> 100015):
#
# 1. Bump the version in BOTH platform build files:
#    - androidApp/build.gradle.kts :
#         versionCode = 1000ZZ        (e.g. 100016)
#         versionName = "X.Y.Z"       (e.g. "1.0.16")
#    - desktopApp/build.gradle.kts :
#         packageVersion = "X.Y.Z"    (e.g. "1.0.16")
#    (iOS marketing version in iosApp/Configuration/Release.xcconfig is NOT
#     bumped per release — leave it as-is, matching past releases.)
#
# 2. Commit and push the bump to main:
#       git add androidApp/build.gradle.kts desktopApp/build.gradle.kts
#       git commit -m "Release X.Y.Z" -m "<body summarizing changes>"
#       git push origin main
#
# 3. Write a release message summarizing changes since the previous tag:
#       git log vPREV..HEAD --oneline --no-merges   # to review what changed
#
# 4. Run this script to create and push the tag:
#       ./scripts/release.sh X.Y.Z "<release message>"
#
#    The pushed tag triggers the GitHub release workflow.
# =============================================================================

VERSION="${1:-}"
MESSAGE="${2:-}"

if [[ -z "$VERSION" || -z "$MESSAGE" ]]; then
  echo "Usage: $0 <version> <message>"
  exit 1
fi

TAG="v${VERSION}"

git tag -a "$TAG" -m "$MESSAGE"
git remote set-url origin git@github.com:omprakashsrv/ampairs-app.git
git push origin "$TAG"

echo "✅  Released $TAG"
