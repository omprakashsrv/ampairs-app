#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Ampairs App — Release Tagging Script
# =============================================================================
#
# Usage: ./scripts/release.sh <version> <message-or-file>
# Examples:
#   ./scripts/release.sh 1.0.16 "Release 1.0.16 — short one-liner"
#   ./scripts/release.sh 1.0.16 /tmp/release_notes.txt    # multi-line notes
#
# The 2nd argument may be EITHER an inline string OR a path to a text file.
# For real release notes (multiple lines / bullet points) ALWAYS pass a file —
# a long inline -m string gets truncated to its first line in the GitHub release.
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
# 3. Write the full release notes (multi-line) to a file, summarizing changes
#    since the previous tag:
#       git log vPREV..HEAD --oneline --no-merges   # to review what changed
#       # write the notes into e.g. /tmp/release_notes.txt
#
# 4. Run this script with the NOTES FILE (not an inline string) so the whole
#    message lands in the tag / GitHub release:
#       ./scripts/release.sh X.Y.Z /tmp/release_notes.txt
#
#    The pushed tag triggers the GitHub release workflow.
# =============================================================================

VERSION="${1:-}"
MESSAGE="${2:-}"

if [[ -z "$VERSION" || -z "$MESSAGE" ]]; then
  echo "Usage: $0 <version> <message-or-file>"
  exit 1
fi

TAG="v${VERSION}"

# Accept the 2nd arg as a file path (preferred, keeps multi-line notes intact)
# or fall back to treating it as an inline message string.
if [[ -f "$MESSAGE" ]]; then
  git tag -a "$TAG" -F "$MESSAGE"
else
  git tag -a "$TAG" -m "$MESSAGE"
fi

git remote set-url origin git@github.com:omprakashsrv/ampairs-app.git
git push origin "$TAG"

echo "✅  Released $TAG"
