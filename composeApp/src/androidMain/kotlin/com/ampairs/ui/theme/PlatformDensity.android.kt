package com.ampairs.ui.theme

import androidx.compose.runtime.Composable

/**
 * Android-specific density recommendations
 * Android typically uses default or slightly compact density
 * depending on screen size and user preferences
 */
@Composable
actual fun getRecommendedDensity(): MaterialDensity {
    return MaterialDensity.COMPACT_1 // -1 density for Android (slightly more compact)
}