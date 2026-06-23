package com.ampairs.inventory.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_back
import ampairsapp.feature.inventory.generated.resources.inv_set_allow_manual_desc
import ampairsapp.feature.inventory.generated.resources.inv_set_allow_manual_title
import ampairsapp.feature.inventory.generated.resources.inv_set_allow_negative_desc
import ampairsapp.feature.inventory.generated.resources.inv_set_allow_negative_title
import ampairsapp.feature.inventory.generated.resources.inv_set_auto_deduct_desc
import ampairsapp.feature.inventory.generated.resources.inv_set_auto_deduct_title
import ampairsapp.feature.inventory.generated.resources.inv_set_block_out_desc
import ampairsapp.feature.inventory.generated.resources.inv_set_block_out_title
import ampairsapp.feature.inventory.generated.resources.inv_set_low_alerts_desc
import ampairsapp.feature.inventory.generated.resources.inv_set_low_alerts_title
import ampairsapp.feature.inventory.generated.resources.inv_settings_header
import ampairsapp.feature.inventory.generated.resources.inv_settings_subtitle
import ampairsapp.feature.inventory.generated.resources.inv_settings_title
import com.ampairs.inventory.domain.InventorySettings
import com.ampairs.inventory.util.InventoryConstants
import org.jetbrains.compose.resources.StringResource
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

private data class FlagSpec(
    val key: String,
    val icon: ImageVector,
    val title: StringResource,
    val desc: StringResource,
    val value: (InventorySettings) -> Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventorySettingsViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = state.settings
    val expanded = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    val flags = listOf(
        FlagSpec(InventoryConstants.FLAG_AUTO_DEDUCT_ON_SALE, Icons.Filled.PointOfSale, Res.string.inv_set_auto_deduct_title, Res.string.inv_set_auto_deduct_desc) { it.autoDeductOnSale },
        FlagSpec(InventoryConstants.FLAG_BLOCK_ORDERS_WHEN_OUT, Icons.Filled.Block, Res.string.inv_set_block_out_title, Res.string.inv_set_block_out_desc) { it.blockOrdersWhenOut },
        FlagSpec(InventoryConstants.FLAG_ALLOW_NEGATIVE_STOCK, Icons.Filled.Remove, Res.string.inv_set_allow_negative_title, Res.string.inv_set_allow_negative_desc) { it.allowNegativeStock },
        FlagSpec(InventoryConstants.FLAG_ALLOW_MANUAL_ADJUSTMENTS, Icons.Filled.EditNote, Res.string.inv_set_allow_manual_title, Res.string.inv_set_allow_manual_desc) { it.allowManualAdjustments },
        FlagSpec(InventoryConstants.FLAG_LOW_STOCK_ALERTS, Icons.Filled.NotificationsActive, Res.string.inv_set_low_alerts_title, Res.string.inv_set_low_alerts_desc) { it.lowStockAlerts },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.inv_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.inv_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.inv_settings_header), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.inv_settings_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                flags.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { spec ->
                            FlagCard(spec, spec.value(s), Modifier.weight(1f)) { v -> viewModel.setFlag(spec.key, v) }
                        }
                        if (pair.size == 1) Box(Modifier.weight(1f))
                    }
                }
            } else {
                flags.forEach { spec ->
                    FlagCard(spec, spec.value(s), Modifier.fillMaxWidth()) { v -> viewModel.setFlag(spec.key, v) }
                }
            }
        }
    }
}

@Composable
private fun FlagCard(spec: FlagSpec, checked: Boolean, modifier: Modifier = Modifier, onToggle: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(spec.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(spec.title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Text(stringResource(spec.desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}
