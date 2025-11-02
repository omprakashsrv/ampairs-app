// Copyright 2020, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.common.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun TiviTheme(
    useDarkColors: Boolean = shouldUseDarkColors(),
    useDynamicColors: Boolean = shouldUseDynamicColors(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme(useDarkColors, useDynamicColors),
        typography = TiviTypography,
        shapes = TiviShapes,
        content = content,
    )
}

@Composable
fun shouldUseDarkColors(): Boolean {
    return isSystemInDarkTheme()
}

@Composable
fun shouldUseDynamicColors(): Boolean {
    return false
}
