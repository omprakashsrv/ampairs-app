package com.ampairs.update.service

import com.ampairs.update.domain.DesktopPlatform

/**
 * Desktop implementation for getting current platform
 * Detects the OS at runtime
 */
actual fun getCurrentPlatform(): DesktopPlatform {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("mac") || osName.contains("darwin") -> DesktopPlatform.MACOS
        osName.contains("win") -> DesktopPlatform.WINDOWS
        osName.contains("nux") || osName.contains("nix") -> DesktopPlatform.LINUX
        else -> DesktopPlatform.LINUX // Default to Linux for unknown platforms
    }
}

/**
 * Desktop implementation for getting current time
 */
actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
