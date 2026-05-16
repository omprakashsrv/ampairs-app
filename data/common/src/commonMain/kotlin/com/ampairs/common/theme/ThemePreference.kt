package com.ampairs.common.theme

/**
 * Theme preference options for the application
 */
enum class ThemePreference(val displayName: String) {
    SYSTEM("System Default"),  // Follow system theme (default)
    LIGHT("Light"),           // Always light theme
    DARK("Dark")              // Always dark theme
}