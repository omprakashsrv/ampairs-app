package com.ampairs.update.service

import com.ampairs.update.domain.DesktopPlatform

/**
 * Android implementation for getting current platform
 * Note: Android apps use Google Play Store for updates, not this system
 * This is provided for compilation compatibility only
 */
actual fun getCurrentPlatform(): DesktopPlatform {
    // Android doesn't use desktop update system - returns LINUX as fallback
    return DesktopPlatform.LINUX
}

/**
 * Android implementation for getting current time
 */
actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
