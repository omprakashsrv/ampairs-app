package com.ampairs.update.service

import com.ampairs.update.domain.DesktopPlatform
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation for getting current platform
 * Note: iOS apps typically use App Store for updates, not this system
 * This is provided for compilation compatibility only
 */
actual fun getCurrentPlatform(): DesktopPlatform {
    // iOS doesn't use desktop update system - returns MACOS as fallback
    return DesktopPlatform.MACOS
}

/**
 * iOS implementation for getting current time
 */
actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
