package com.ampairs.ecom.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Ecom design tokens. Brand surfaces come from `MaterialTheme.colorScheme` (the Ampairs M3
 * palette). Only the success/savings green pair — which has no M3 colorScheme slot — is
 * defined here, lifted verbatim from the prototype's `ecom.css` (#C8EAD0 / #0d4f1c).
 */
object EcomColors {
    val SuccessGreen = Color(0xFFC8EAD0)
    val OnSuccessGreen = Color(0xFF0D4F1C)
}

/** M3 corner + spacing tokens from `colors_and_type.css`. */
object EcomDimens {
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 24.dp

    val cornerXs = RoundedCornerShape(4.dp)
    val cornerSm = RoundedCornerShape(8.dp)
    val cornerMd = RoundedCornerShape(12.dp)
    val cornerLg = RoundedCornerShape(16.dp)
    val cornerXl = RoundedCornerShape(28.dp)
}
