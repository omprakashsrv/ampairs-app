package com.ampairs.update.service

import com.ampairs.update.domain.UpdateInfo

/**
 * Android implementation for update installer
 * Note: Android apps use Google Play Store for updates, not manual installation
 * This implementation is provided for compilation compatibility only
 */
actual fun openInstallerImpl(filePath: String, updateInfo: UpdateInfo): Boolean {
    // Android doesn't support manual installer opening
    println("⚠️ Manual update installation is not supported on Android")
    println("   Please use Google Play Store for updates")
    return false
}
