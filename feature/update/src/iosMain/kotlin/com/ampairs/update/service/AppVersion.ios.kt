package com.ampairs.update.service

import com.ampairs.update.domain.DesktopPlatform
/**
 * iOS implementation for getting current platform
 * Note: iOS apps typically use App Store for updates, not this system
 * This is provided for compilation compatibility only
 */
actual fun getCurrentPlatform(): DesktopPlatform {
    // iOS doesn't use desktop update system - returns MACOS as fallback
    return DesktopPlatform.MACOS
}
