// Copyright 2022, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.common.compose.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

val TiviTypography: Typography
    @Composable get() {
        val default = Typography()
        return Typography(
            displayLarge = default.displayLarge,
            displayMedium = default.displayMedium,
            displaySmall = default.displaySmall,
            headlineLarge = default.headlineLarge,
            headlineMedium = default.headlineMedium,
            headlineSmall = default.headlineSmall,
            titleLarge = default.titleLarge,
            titleMedium = default.titleMedium,
            titleSmall = default.titleSmall,
            bodyLarge = default.bodyLarge,
            bodyMedium = default.bodyMedium,
            bodySmall = default.bodySmall,
            labelLarge = default.labelLarge,
            labelMedium = default.labelMedium,
            labelSmall = default.labelSmall,
        )
    }
