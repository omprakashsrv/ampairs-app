package com.ampairs.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Component dimensions that scale with density
 */
data class ComponentDimensions(
    // Button dimensions
    val buttonHeight: Dp,
    val buttonMinWidth: Dp,
    val buttonHorizontalPadding: Dp,
    val buttonVerticalPadding: Dp,
    val buttonBorderWidth: Dp,
    val buttonCornerRadius: Dp,
    
    // TextField dimensions
    val textFieldHeight: Dp,
    val textFieldMinHeight: Dp,
    val textFieldHorizontalPadding: Dp,
    val textFieldVerticalPadding: Dp,
    val textFieldBorderWidth: Dp,
    val textFieldCornerRadius: Dp,
    
    // Card dimensions
    val cardElevation: Dp,
    val cardCornerRadius: Dp,
    val cardPadding: Dp,
    val cardMargin: Dp,
    
    // List item dimensions
    val listItemHeight: Dp,
    val listItemPadding: Dp,
    val listItemIconSize: Dp,
    val listItemSpacing: Dp,
    
    // Icon dimensions
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val iconSizeLarge: Dp,
    
    // Spacing dimensions
    val spacingXs: Dp,
    val spacingSmall: Dp,
    val spacingMedium: Dp,
    val spacingLarge: Dp,
    val spacingXl: Dp,
    
    // FAB dimensions
    val fabSize: Dp,
    val fabIconSize: Dp,
    val fabElevation: Dp,
    
    // AppBar dimensions
    val appBarHeight: Dp,
    val appBarElevation: Dp,
    val appBarPadding: Dp,
    
    // Dialog dimensions
    val dialogMaxWidth: Dp,
    val dialogPadding: Dp,
    val dialogCornerRadius: Dp,
    val dialogElevation: Dp
)

/**
 * Create component dimensions based on Material Design density level
 */
fun createComponentDimensions(density: MaterialDensity): ComponentDimensions {
    val scale = density.scale
    
    return ComponentDimensions(
        // Button dimensions (Material Design spec)
        buttonHeight = (40.dp * scale),
        buttonMinWidth = (64.dp * scale),
        buttonHorizontalPadding = (24.dp * scale),
        buttonVerticalPadding = (8.dp * scale),
        buttonBorderWidth = (1.dp * scale),
        buttonCornerRadius = (4.dp * scale),
        
        // TextField dimensions
        textFieldHeight = (56.dp * scale),
        textFieldMinHeight = (40.dp * scale),
        textFieldHorizontalPadding = (16.dp * scale),
        textFieldVerticalPadding = (12.dp * scale),
        textFieldBorderWidth = (1.dp * scale),
        textFieldCornerRadius = (4.dp * scale),
        
        // Card dimensions
        cardElevation = (2.dp * scale),
        cardCornerRadius = (8.dp * scale),
        cardPadding = (16.dp * scale),
        cardMargin = (8.dp * scale),
        
        // List item dimensions
        listItemHeight = (56.dp * scale),
        listItemPadding = (16.dp * scale),
        listItemIconSize = (24.dp * scale),
        listItemSpacing = (16.dp * scale),
        
        // Icon dimensions
        iconSizeSmall = (16.dp * scale),
        iconSizeMedium = (24.dp * scale),
        iconSizeLarge = (32.dp * scale),
        
        // Spacing dimensions
        spacingXs = (4.dp * scale),
        spacingSmall = (8.dp * scale),
        spacingMedium = (16.dp * scale),
        spacingLarge = (24.dp * scale),
        spacingXl = (32.dp * scale),
        
        // FAB dimensions
        fabSize = (56.dp * scale),
        fabIconSize = (24.dp * scale),
        fabElevation = (6.dp * scale),
        
        // AppBar dimensions
        appBarHeight = (56.dp * scale),
        appBarElevation = (4.dp * scale),
        appBarPadding = (16.dp * scale),
        
        // Dialog dimensions
        dialogMaxWidth = (560.dp * scale),
        dialogPadding = (24.dp * scale),
        dialogCornerRadius = (12.dp * scale),
        dialogElevation = (6.dp * scale)
    )
}

/**
 * CompositionLocal for component dimensions
 */
val LocalComponentDimensions = staticCompositionLocalOf { 
    createComponentDimensions(MaterialDensity.DEFAULT) 
}

/**
 * Extension functions for easy access to scaled dimensions
 */
object Dimensions {
    val current: ComponentDimensions
        @Composable get() = LocalComponentDimensions.current
}

/**
 * Scaled shapes based on density
 */
object DensityShapes {
    @Composable
    fun buttonShape(): Shape = RoundedCornerShape(Dimensions.current.buttonCornerRadius)
    
    @Composable
    fun textFieldShape(): Shape = RoundedCornerShape(Dimensions.current.textFieldCornerRadius)
    
    @Composable
    fun cardShape(): Shape = RoundedCornerShape(Dimensions.current.cardCornerRadius)
    
    @Composable
    fun dialogShape(): Shape = RoundedCornerShape(Dimensions.current.dialogCornerRadius)
}

/**
 * Scaled padding values
 */
object DensityPadding {
    @Composable
    fun button(): PaddingValues = PaddingValues(
        horizontal = Dimensions.current.buttonHorizontalPadding,
        vertical = Dimensions.current.buttonVerticalPadding
    )
    
    @Composable
    fun textField(): PaddingValues = PaddingValues(
        horizontal = Dimensions.current.textFieldHorizontalPadding,
        vertical = Dimensions.current.textFieldVerticalPadding
    )
    
    @Composable
    fun card(): PaddingValues = PaddingValues(Dimensions.current.cardPadding)
    
    @Composable
    fun listItem(): PaddingValues = PaddingValues(Dimensions.current.listItemPadding)
    
    @Composable
    fun dialog(): PaddingValues = PaddingValues(Dimensions.current.dialogPadding)
    
    @Composable
    fun appBar(): PaddingValues = PaddingValues(horizontal = Dimensions.current.appBarPadding)
}

/**
 * Scaled border strokes
 */
object DensityBorders {
    @Composable
    fun button() = BorderStroke(
        width = Dimensions.current.buttonBorderWidth,
        color = androidx.compose.material3.MaterialTheme.colorScheme.outline
    )
    
    @Composable
    fun textField() = BorderStroke(
        width = Dimensions.current.textFieldBorderWidth,
        color = androidx.compose.material3.MaterialTheme.colorScheme.outline
    )
}