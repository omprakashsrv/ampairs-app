package com.ampairs.ui.theme

import androidx.compose.runtime.Composable

/**
 * Desktop-specific density recommendations
 * Desktop typically needs more compact UI due to larger screen real estate
 * and more precise mouse interactions
 */
@Composable
actual fun getRecommendedDensity(): MaterialDensity {
    return MaterialDensity.COMPACT_2 // -2 density for desktop
}