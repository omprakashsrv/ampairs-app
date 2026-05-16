package com.ampairs.workspace.navigation

/**
 * Desktop implementation of platform navigation detector
 * Returns MENU_BAR pattern for native desktop menu bar integration
 */
actual object PlatformNavigationDetector {
    actual fun getNavigationPattern(): NavigationPattern {
        return NavigationPattern.MENU_BAR
    }

    /**
     * Desktop has keyboard shortcuts and browser back button,
     * but UI back button is helpful for discoverability
     */
    actual fun requiresBackButton(): Boolean {
        return true
    }
}