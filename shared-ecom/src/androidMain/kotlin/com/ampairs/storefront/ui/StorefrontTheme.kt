package com.ampairs.storefront.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StorefrontSeed = Color(0xFF1B6C4A)

private val LightColors = lightColorScheme(
    primary = StorefrontSeed,
    secondary = Color(0xFF4C6358),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6A6),
    secondary = Color(0xFFB2CCBE),
)

/**
 * Lightweight Material 3 theme for the Storefront ecom app. The full app's `PlatformAmpairsTheme`
 * (Material Kolor driven) lives in :shared; this slim variant just needs a consistent color scheme
 * for the reused ecom + auth screens, which read `MaterialTheme.colorScheme.*` tokens.
 */
@Composable
fun StorefrontTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
