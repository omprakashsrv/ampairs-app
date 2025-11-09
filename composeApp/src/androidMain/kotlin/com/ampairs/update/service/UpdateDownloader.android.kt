package com.ampairs.update.service

/**
 * Android implementation for update download functions
 * Note: Android apps use Google Play Store for updates, not manual downloads
 * These implementations are provided for compilation compatibility only
 */

actual suspend fun downloadUpdateFileImpl(
    url: String,
    fileName: String,
    onProgress: (Float) -> Unit
): String? {
    // Android doesn't support manual update downloads
    println("⚠️ Manual update downloads are not supported on Android")
    println("   Please use Google Play Store for updates")
    return null
}

actual fun verifyChecksumImpl(filePath: String, expectedChecksum: String): Boolean {
    // Android doesn't need checksum verification for manual updates
    return false
}

actual fun deleteFileImpl(filePath: String) {
    // No-op on Android
}
