package com.ampairs.update.service

import com.ampairs.update.domain.DesktopPlatform
import com.ampairs.update.domain.UpdateInfo
import kotlin.js.Date

/**
 * WebAssembly implementation for update functions
 * Note: Web apps don't use traditional update systems - they auto-update on page refresh
 * These implementations are provided for compilation compatibility only
 */

actual fun getCurrentPlatform(): DesktopPlatform {
    // Web doesn't use desktop update system - returns LINUX as fallback
    return DesktopPlatform.LINUX
}

actual fun currentTimeMillis(): Long {
    return Date.now().toLong()
}

actual suspend fun downloadUpdateFileImpl(
    url: String,
    fileName: String,
    onProgress: (Float) -> Unit
): String? {
    // Web apps don't support file downloads in this context
    println("⚠️ Manual update downloads are not supported on Web")
    println("   Web apps auto-update on page refresh")
    return null
}

actual fun verifyChecksumImpl(filePath: String, expectedChecksum: String): Boolean {
    // Web doesn't need checksum verification
    return false
}

actual fun deleteFileImpl(filePath: String) {
    // No-op on Web
}

actual fun openInstallerImpl(filePath: String, updateInfo: UpdateInfo): Boolean {
    // Web apps don't support manual installer opening
    println("⚠️ Manual update installation is not supported on Web")
    println("   Web apps auto-update on page refresh")
    return false
}
