#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/release.sh <version> <message>
# Example: ./scripts/release.sh 1.0.9 "Initial release"

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