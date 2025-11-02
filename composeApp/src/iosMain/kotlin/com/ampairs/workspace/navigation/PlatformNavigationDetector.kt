package com.ampairs.workspace.navigation

/**
 * iOS implementation of platform navigation detector
 * Returns SIDE_DRAWER pattern for iOS navigation drawer
 */
actual object PlatformNavigationDetector {
    actual fun getNavigationPattern(): NavigationPattern {
        return NavigationPattern.SIDE_DRAWER
    }

    /**
     * iOS has no hardware back button,
     * so UI back button is always required
     */
    actual fun requiresBackButton(): Boolean {
        return true
    }
}