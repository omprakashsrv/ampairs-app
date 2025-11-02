package com.ampairs.ui.theme

import androidx.compose.runtime.Composable

/**
 * iOS-specific density recommendations
 * iOS typically uses default density as the platform
 * handles scaling through accessibility settings
 */
@Composable
actual fun getRecommendedDensity(): MaterialDensity {
    return MaterialDensity.DEFAULT // iOS uses default density
}