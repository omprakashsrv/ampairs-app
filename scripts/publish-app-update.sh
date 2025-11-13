#!/bin/bash

##############################################################################
# Ampairs Desktop App Update Publisher
#
# This script automates the process of publishing desktop app updates:
# 1. Validates inputs and file
# 2. Calculates SHA-256 checksum
# 3. Uploads binary to S3 (private bucket)
# 4. Registers version in database via API
#
# Usage:
#   ./publish-app-update.sh <file> <version> <platform> [options]
#
# Example:
#   ./publish-app-update.sh Ampairs-1.0.0.10.dmg 1.0.0.10 MACOS
#
# Options:
#   --mandatory             Mark as mandatory update
#   --min-version <ver>     Set minimum supported version
#   --release-notes <file>  Path to release notes file
#   --dry-run               Validate only, don't upload
#   --api-url <url>         Override API base URL
#   --s3-bucket <name>      Override S3 bucket name
#
##############################################################################

set -e  # Exit on error
set -u  # Exit on undefined variable

# ============================================================================
# Configuration
# ============================================================================

# Required environment variables
: "${AMPAIRS_ADMIN_TOKEN:?Error: AMPAIRS_ADMIN_TOKEN not set}"
: "${AWS_ACCESS_KEY_ID:?Error: AWS_ACCESS_KEY_ID not set}"
: "${AWS_SECRET_ACCESS_KEY:?Error: AWS_SECRET_ACCESS_KEY not set}"

# Default configuration
API_BASE_URL="${API_BASE_URL:-https://api.ampairs.in}"
S3_BUCKET="${S3_BUCKET:-ampairs-app-updates}"
AWS_REGION="${AWS_REGION:-ap-south-1}"

# ============================================================================
# Color Output
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}" >&2
}

# ============================================================================
# Helper Functions
# ============================================================================

usage() {
    cat << EOF
Usage: $0 <file> <version> <platform> [options]

Arguments:
  file       Path to the installer binary (.dmg, .msi, .deb)
  version    Version string (e.g., 1.0.0.10)
  platform   Platform type (MACOS, WINDOWS, LINUX)

Options:
  --mandatory               Mark as mandatory update
  --min-version <version>   Set minimum supported version
  --release-notes <file>    Path to release notes markdown file
  --dry-run                 Validate only, don't upload
  --api-url <url>           Override API base URL
  --s3-bucket <name>        Override S3 bucket name

Environment Variables:
  AMPAIRS_ADMIN_TOKEN       Admin JWT token (required)
  AWS_ACCESS_KEY_ID         AWS access key (required)
  AWS_SECRET_ACCESS_KEY     AWS secret key (required)
  AWS_REGION                AWS region (default: ap-south-1)
  API_BASE_URL              API base URL (default: https://api.ampairs.in)
  S3_BUCKET                 S3 bucket name (default: ampairs-app-updates)

Examples:
  # Basic release
  $0 Ampairs-1.0.0.10.dmg 1.0.0.10 MACOS

  # Mandatory update with release notes
  $0 Ampairs-1.0.0.11.msi 1.0.0.11 WINDOWS \\
    --mandatory \\
    --min-version 1.0.0.5 \\
    --release-notes release-notes.md

  # Dry run (validation only)
  $0 Ampairs-1.0.0.12.deb 1.0.0.12 LINUX --dry-run

EOF
    exit 1
}

# Calculate SHA-256 checksum
calculate_checksum() {
    local file=$1
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$file" | awk '{print $1}'
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$file" | awk '{print $1}'
    else
        log_error "Neither sha256sum nor shasum found"
        exit 1
    fi
}

# Calculate file size in MB
calculate_file_size() {
    local file=$1
    local size_bytes=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null)
    echo "scale=2; $size_bytes / 1024 / 1024" | bc
}

# Extract version code from version string
extract_version_code() {
    local version=$1
    # Extract last component (e.g., "1.0.0.10" -> "10")
    echo "$version" | awk -F. '{print $NF}'
}

# Validate version format
validate_version() {
    local version=$1
    if ! [[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        log_error "Invalid version format: $version"
        log_info "Expected format: MAJOR.MINOR.PATCH.BUILD (e.g., 1.0.0.10)"
        exit 1
    fi
}

# Validate platform
validate_platform() {
    local platform=$1
    if [[ ! "$platform" =~ ^(MACOS|WINDOWS|LINUX)$ ]]; then
        log_error "Invalid platform: $platform"
        log_info "Valid platforms: MACOS, WINDOWS, LINUX"
        exit 1
    fi
}

# Validate file extension matches platform
validate_file_extension() {
    local file=$1
    local platform=$2

    case "$platform" in
        MACOS)
            if [[ ! "$file" =~ \.dmg$ ]]; then
                log_error "macOS builds must be .dmg files"
                exit 1
            fi
            ;;
        WINDOWS)
            if [[ ! "$file" =~ \.(msi|exe)$ ]]; then
                log_error "Windows builds must be .msi or .exe files"
                exit 1
            fi
            ;;
        LINUX)
            if [[ ! "$file" =~ \.(deb|rpm|AppImage)$ ]]; then
                log_error "Linux builds must be .deb, .rpm, or AppImage files"
                exit 1
            fi
            ;;
    esac
}

# Upload file to S3
upload_to_s3() {
    local file=$1
    local s3_key=$2

    log_info "Uploading to S3: s3://$S3_BUCKET/$s3_key"

    if command -v aws >/dev/null 2>&1; then
        aws s3 cp "$file" "s3://$S3_BUCKET/$s3_key" \
            --region "$AWS_REGION" \
            --no-progress \
            --acl private
    else
        log_error "AWS CLI not found. Please install: https://aws.amazon.com/cli/"
        exit 1
    fi
}

# Register version in database via API
register_version() {
    local version=$1
    local version_code=$2
    local platform=$3
    local s3_key=$4
    local filename=$5
    local file_size_mb=$6
    local checksum=$7
    local is_mandatory=${8:-false}
    local min_version=${9:-null}
    local release_notes=${10:-null}

    log_info "Registering version in database..."

    # Build JSON payload
    local json_payload=$(cat <<EOF
{
  "version": "$version",
  "version_code": $version_code,
  "platform": "$platform",
  "is_mandatory": $is_mandatory,
  "s3_key": "$s3_key",
  "filename": "$filename",
  "file_size_mb": $file_size_mb,
  "checksum": "$checksum"
EOF
)

    # Add optional fields
    if [[ "$min_version" != "null" ]]; then
        json_payload+=",\"min_supported_version\": \"$min_version\""
    fi

    if [[ "$release_notes" != "null" ]]; then
        # Escape quotes and newlines in release notes
        local escaped_notes=$(echo "$release_notes" | jq -Rs .)
        json_payload+=",\"release_notes\": $escaped_notes"
    fi

    json_payload+="}"

    # Make API request
    local response=$(curl -s -w "\n%{http_code}" \
        -X POST \
        -H "Authorization: Bearer $AMPAIRS_ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$json_payload" \
        "$API_BASE_URL/api/v1/app-updates")

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
        log_success "Version registered successfully"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        log_error "Failed to register version (HTTP $http_code)"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        exit 1
    fi
}

# ============================================================================
# Main Script
# ============================================================================

main() {
    # Parse arguments
    if [[ $# -lt 3 ]]; then
        usage
    fi

    local FILE_PATH=$1
    local VERSION=$2
    local PLATFORM=$3
    shift 3

    # Parse options
    local IS_MANDATORY=false
    local MIN_VERSION=""
    local RELEASE_NOTES_FILE=""
    local DRY_RUN=false

    while [[ $# -gt 0 ]]; then
        case $1 in
            --mandatory)
                IS_MANDATORY=true
                shift
                ;;
            --min-version)
                MIN_VERSION=$2
                shift 2
                ;;
            --release-notes)
                RELEASE_NOTES_FILE=$2
                shift 2
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --api-url)
                API_BASE_URL=$2
                shift 2
                ;;
            --s3-bucket)
                S3_BUCKET=$2
                shift 2
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                ;;
        esac
    done

    # Header
    echo ""
    log_info "========================================="
    log_info "  Ampairs Desktop App Update Publisher"
    log_info "========================================="
    echo ""

    # Validate inputs
    log_info "Validating inputs..."

    if [[ ! -f "$FILE_PATH" ]]; then
        log_error "File not found: $FILE_PATH"
        exit 1
    fi

    validate_version "$VERSION"
    validate_platform "$PLATFORM"
    validate_file_extension "$FILE_PATH" "$PLATFORM"

    log_success "Validation passed"

    # Extract version code
    VERSION_CODE=$(extract_version_code "$VERSION")

    # Calculate checksum
    log_info "Calculating SHA-256 checksum..."
    CHECKSUM=$(calculate_checksum "$FILE_PATH")
    log_success "Checksum: $CHECKSUM"

    # Calculate file size
    log_info "Calculating file size..."
    FILE_SIZE_MB=$(calculate_file_size "$FILE_PATH")
    log_success "File size: ${FILE_SIZE_MB} MB"

    # Read release notes if provided
    RELEASE_NOTES=""
    if [[ -n "$RELEASE_NOTES_FILE" ]]; then
        if [[ ! -f "$RELEASE_NOTES_FILE" ]]; then
            log_error "Release notes file not found: $RELEASE_NOTES_FILE"
            exit 1
        fi
        RELEASE_NOTES=$(cat "$RELEASE_NOTES_FILE")
        log_success "Release notes loaded from $RELEASE_NOTES_FILE"
    fi

    # Generate S3 key and filename
    FILE_EXTENSION="${FILE_PATH##*.}"
    S3_KEY="updates/${PLATFORM,,}-${VERSION}.${FILE_EXTENSION}"
    FILENAME="${PLATFORM,,}-${VERSION}.${FILE_EXTENSION}"

    # Summary
    echo ""
    log_info "========================================="
    log_info "  Release Summary"
    log_info "========================================="
    echo "File:             $FILE_PATH"
    echo "Version:          $VERSION (code: $VERSION_CODE)"
    echo "Platform:         $PLATFORM"
    echo "File Size:        ${FILE_SIZE_MB} MB"
    echo "Checksum:         $CHECKSUM"
    echo "Mandatory:        $IS_MANDATORY"
    if [[ -n "$MIN_VERSION" ]]; then
        echo "Min Version:      $MIN_VERSION"
    fi
    echo "S3 Bucket:        $S3_BUCKET"
    echo "S3 Key:           $S3_KEY"
    echo "Filename:         $FILENAME"
    echo "API URL:          $API_BASE_URL"
    echo ""

    if [[ "$DRY_RUN" == true ]]; then
        log_warning "DRY RUN MODE - No changes will be made"
        exit 0
    fi

    # Confirmation (skip in CI environment)
    if [[ -z "${CI:-}" ]]; then
        read -p "Proceed with upload and registration? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_warning "Cancelled by user"
            exit 0
        fi
    fi

    # Upload to S3
    echo ""
    log_info "Step 1/2: Uploading to S3..."
    upload_to_s3 "$FILE_PATH" "$S3_KEY"
    log_success "Upload complete"

    # Register in database
    echo ""
    log_info "Step 2/2: Registering version..."
    register_version \
        "$VERSION" \
        "$VERSION_CODE" \
        "$PLATFORM" \
        "$S3_KEY" \
        "$FILENAME" \
        "$FILE_SIZE_MB" \
        "$CHECKSUM" \
        "$IS_MANDATORY" \
        "${MIN_VERSION:-null}" \
        "${RELEASE_NOTES:-null}"

    # Success
    echo ""
    log_success "========================================="
    log_success "  Release Published Successfully! 🎉"
    log_success "========================================="
    echo ""
    log_info "Users will receive update notifications within 4 hours"
    echo ""
}

# Run main function
main "$@"
