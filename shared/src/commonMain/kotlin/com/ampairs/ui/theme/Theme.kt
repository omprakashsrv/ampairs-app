package com.ampairs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Color schemes
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Density-aware typography
 */
@Composable
fun createDensityAwareTypography(density: MaterialDensity): Typography {
    val baseTypography = Typography()
    val sizeScale = when (density) {
        MaterialDensity.DEFAULT -> 1.0f
        MaterialDensity.COMPACT_1 -> 0.95f
        MaterialDensity.COMPACT_2 -> 0.90f
        MaterialDensity.COMPACT_3 -> 0.85f
        MaterialDensity.COMPACT_4 -> 0.80f
    }
    
    return Typography(
        displayLarge = baseTypography.displayLarge.copy(fontSize = (baseTypography.displayLarge.fontSize.value * sizeScale).sp),
        displayMedium = baseTypography.displayMedium.copy(fontSize = (baseTypography.displayMedium.fontSize.value * sizeScale).sp),
        displaySmall = baseTypography.displaySmall.copy(fontSize = (baseTypography.displaySmall.fontSize.value * sizeScale).sp),
        headlineLarge = baseTypography.headlineLarge.copy(fontSize = (baseTypography.headlineLarge.fontSize.value * sizeScale).sp),
        headlineMedium = baseTypography.headlineMedium.copy(fontSize = (baseTypography.headlineMedium.fontSize.value * sizeScale).sp),
        headlineSmall = baseTypography.headlineSmall.copy(fontSize = (baseTypography.headlineSmall.fontSize.value * sizeScale).sp),
        titleLarge = baseTypography.titleLarge.copy(fontSize = (baseTypography.titleLarge.fontSize.value * sizeScale).sp),
        titleMedium = baseTypography.titleMedium.copy(fontSize = (baseTypography.titleMedium.fontSize.value * sizeScale).sp),
        titleSmall = baseTypography.titleSmall.copy(fontSize = (baseTypography.titleSmall.fontSize.value * sizeScale).sp),
        bodyLarge = baseTypography.bodyLarge.copy(fontSize = (baseTypography.bodyLarge.fontSize.value * sizeScale).sp),
        bodyMedium = baseTypography.bodyMedium.copy(fontSize = (baseTypography.bodyMedium.fontSize.value * sizeScale).sp),
        bodySmall = baseTypography.bodySmall.copy(fontSize = (baseTypography.bodySmall.fontSize.value * sizeScale).sp),
        labelLarge = baseTypography.labelLarge.copy(fontSize = (baseTypography.labelLarge.fontSize.value * sizeScale).sp),
        labelMedium = baseTypography.labelMedium.copy(fontSize = (baseTypography.labelMedium.fontSize.value * sizeScale).sp),
        labelSmall = baseTypography.labelSmall.copy(fontSize = (baseTypography.labelSmall.fontSize.value * sizeScale).sp)
    )
}

/**
 * Main theme with density support
 */
@Composable
fun AmpairsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    density: MaterialDensity = MaterialDensity.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val typography = createDensityAwareTypography(density)
    
    DensityTheme(density = density) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

/**
 * Platform-specific theme wrapper
 */
@Composable
fun PlatformAmpairsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val recommendedDensity = getRecommendedDensity()
    
    AmpairsTheme(
        darkTheme = darkTheme,
        density = recommendedDensity,
        content = content
    )
}