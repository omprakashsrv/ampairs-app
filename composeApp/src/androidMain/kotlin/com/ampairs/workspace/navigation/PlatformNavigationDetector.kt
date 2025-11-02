package com.ampairs.workspace.navigation

/**
 * Android implementation of platform navigation detector
 * Returns SIDE_DRAWER pattern for Material Design navigation drawer
 */
actual object PlatformNavigationDetector {
    actual fun getNavigationPattern(): NavigationPattern {
        return NavigationPattern.SIDE_DRAWER
    }

    /**
     * Android has hardware back button or gesture navigation,
     * so UI back button is optional
     */
    actual fun requiresBackButton(): Boolean {
        return true
    }
}