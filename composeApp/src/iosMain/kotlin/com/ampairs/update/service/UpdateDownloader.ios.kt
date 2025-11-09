package com.ampairs.update.service

/**
 * iOS implementation for update download functions
 * Note: iOS apps use App Store for updates, not manual downloads
 * These implementations are provided for compilation compatibility only
 */

actual suspend fun downloadUpdateFileImpl(
    url: String,
    fileName: String,
    onProgress: (Float) -> Unit
): String? {
    // iOS doesn't support manual update downloads
    println("⚠️ Manual update downloads are not supported on iOS")
    println("   Please use App Store for updates")
    return null
}

actual fun verifyChecksumImpl(filePath: String, expectedChecksum: String): Boolean {
    // iOS doesn't need checksum verification for manual updates
    return false
}

actual fun deleteFileImpl(filePath: String) {
    // No-op on iOS
}
