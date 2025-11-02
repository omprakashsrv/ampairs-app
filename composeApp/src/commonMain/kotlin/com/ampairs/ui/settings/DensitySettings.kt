package com.ampairs.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ampairs.ui.theme.MaterialDensity

/**
 * Settings screen component for density selection
 */
@Composable
fun DensitySettingsCard(
    currentDensity: MaterialDensity,
    onDensityChanged: (MaterialDensity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "UI Density",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Control the size and spacing of UI components. Higher density values make components more compact.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            MaterialDensity.values().forEach { density ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (density == currentDensity),
                            onClick = { onDensityChanged(density) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (density == currentDensity),
                        onClick = { onDensityChanged(density) }
                    )
                    
                    Column(
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = getDensityDisplayName(density),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = getDensityDescription(density),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getDensityDisplayName(density: MaterialDensity): String {
    return when (density) {
        MaterialDensity.DEFAULT -> "Default (0)"
        MaterialDensity.COMPACT_1 -> "Compact (-1)"
        MaterialDensity.COMPACT_2 -> "More Compact (-2)"
        MaterialDensity.COMPACT_3 -> "Very Compact (-3)"
        MaterialDensity.COMPACT_4 -> "Maximum Density (-4)"
    }
}

private fun getDensityDescription(density: MaterialDensity): String {
    return when (density) {
        MaterialDensity.DEFAULT -> "Standard component sizes"
        MaterialDensity.COMPACT_1 -> "Slightly smaller components"
        MaterialDensity.COMPACT_2 -> "Noticeably smaller components (recommended for desktop)"
        MaterialDensity.COMPACT_3 -> "Very compact layout"
        MaterialDensity.COMPACT_4 -> "Maximum information density"
    }
}