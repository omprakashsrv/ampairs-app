package com.ampairs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Material Design density levels
 * 0 = Default (no density scaling)
 * -1 = Slightly more compact
 * -2 = More compact  
 * -3 = Very compact
 * -4 = Maximum density (most compact)
 */
enum class MaterialDensity(val level: Int, val scale: Float) {
    DEFAULT(0, 1.0f),
    COMPACT_1(-1, 0.92f),
    COMPACT_2(-2, 0.84f), 
    COMPACT_3(-3, 0.76f),
    COMPACT_4(-4, 0.68f)
}

val LocalMaterialDensity = staticCompositionLocalOf { MaterialDensity.DEFAULT }

@Composable
fun DensityTheme(
    density: MaterialDensity = MaterialDensity.DEFAULT,
    content: @Composable () -> Unit
) {
    val localDensity = LocalDensity.current
    val scaledDensity = Density(
        density = localDensity.density * density.scale,
        fontScale = localDensity.fontScale
    )
    
    val componentDimensions = createComponentDimensions(density)
    
    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalMaterialDensity provides density,
        LocalComponentDimensions provides componentDimensions
    ) {
        content()
    }
}

/**
 * Platform-specific density recommendations
 */
@Composable
expect fun getRecommendedDensity(): MaterialDensity

/**
 * Component-specific size adjustments based on density
 */
object DensityAdjustedSizes {
    @Composable
    fun buttonHeight(): androidx.compose.ui.unit.Dp {
        val density = LocalMaterialDensity.current
        return when (density) {
            MaterialDensity.DEFAULT -> 56.dp
            MaterialDensity.COMPACT_1 -> 52.dp
            MaterialDensity.COMPACT_2 -> 48.dp
            MaterialDensity.COMPACT_3 -> 44.dp
            MaterialDensity.COMPACT_4 -> 40.dp
        }
    }
    
    @Composable
    fun textFieldHeight(): androidx.compose.ui.unit.Dp {
        val density = LocalMaterialDensity.current
        return when (density) {
            MaterialDensity.DEFAULT -> 56.dp
            MaterialDensity.COMPACT_1 -> 52.dp
            MaterialDensity.COMPACT_2 -> 48.dp
            MaterialDensity.COMPACT_3 -> 44.dp
            MaterialDensity.COMPACT_4 -> 40.dp
        }
    }

    @Composable
    fun textFieldPadding(): androidx.compose.ui.unit.Dp {
        val density = LocalMaterialDensity.current
        return when (density) {
            MaterialDensity.DEFAULT -> 4.dp
            MaterialDensity.COMPACT_1 -> 3.5.dp
            MaterialDensity.COMPACT_2 -> 3.dp
            MaterialDensity.COMPACT_3 -> 2.5.dp
            MaterialDensity.COMPACT_4 -> 2.dp
        }
    }
    
    @Composable
    fun cardPadding(): androidx.compose.ui.unit.Dp {
        val density = LocalMaterialDensity.current
        return when (density) {
            MaterialDensity.DEFAULT -> 16.dp
            MaterialDensity.COMPACT_1 -> 14.dp
            MaterialDensity.COMPACT_2 -> 12.dp
            MaterialDensity.COMPACT_3 -> 10.dp
            MaterialDensity.COMPACT_4 -> 8.dp
        }
    }
}