package com.ampairs.update.service

import com.ampairs.update.domain.UpdateInfo

/**
 * iOS implementation for update installer
 * Note: iOS apps use App Store for updates, not manual installation
 * This implementation is provided for compilation compatibility only
 */
actual fun openInstallerImpl(filePath: String, updateInfo: UpdateInfo): Boolean {
    // iOS doesn't support manual installer opening
    println("⚠️ Manual update installation is not supported on iOS")
    println("   Please use App Store for updates")
    return false
}
