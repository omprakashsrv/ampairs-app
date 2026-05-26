package com.ampairs.update.service

import com.ampairs.update.domain.DesktopPlatform

/**
 * App version information
 * This should be updated when building releases
 */
object AppVersion {
    const val VERSION_NAME = "1.0.0.17"
    const val VERSION_CODE = 17
}

/**
 * Get current platform
 * This is implemented using expect/actual pattern for each platform
 */
expect fun getCurrentPlatform(): DesktopPlatform
