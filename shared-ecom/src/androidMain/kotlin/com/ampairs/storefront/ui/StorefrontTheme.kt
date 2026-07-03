package com.ampairs.storefront.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Fallback brand color when a client build doesn't supply one. */
val DefaultSeedColor = Color(0xFF1B6C4A)

/**
 * Lightweight Material 3 theme for the customer storefront app, driven by a per-client [seedColor]
 * (each white-label build passes its own brand color). The reused ecom + auth screens read
 * `MaterialTheme.colorScheme.*` tokens, so setting `primary` recolors buttons, app bars, selection
 * and progress across the app. `onPrimary` is chosen by luminance so any client color stays legible.
 *
 * Kept dependency-free (no Material Kolor) — a single accent role is enough for white-label branding.
 */
@Composable
fun StorefrontTheme(
    seedColor: Color = DefaultSeedColor,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val onSeed = if (seedColor.luminance() > 0.5f) Color.Black else Color.White
    val colorScheme = if (darkTheme) {
        darkColorScheme(primary = seedColor, onPrimary = onSeed)
    } else {
        lightColorScheme(primary = seedColor, onPrimary = onSeed)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
