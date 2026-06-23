package com.ampairs.inventory.ui.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_action_add_item
import ampairsapp.feature.inventory.generated.resources.inv_action_adjust
import ampairsapp.feature.inventory.generated.resources.inv_action_count
import ampairsapp.feature.inventory.generated.resources.inv_action_ledger
import ampairsapp.feature.inventory.generated.resources.inv_action_low_stock
import ampairsapp.feature.inventory.generated.resources.inv_dash_out_of_stock
import ampairsapp.feature.inventory.generated.resources.inv_dash_low_stock
import ampairsapp.feature.inventory.generated.resources.inv_dash_quick_actions
import ampairsapp.feature.inventory.generated.resources.inv_dash_recent_empty
import ampairsapp.feature.inventory.generated.resources.inv_dash_recent_movements
import ampairsapp.feature.inventory.generated.resources.inv_dash_title
import ampairsapp.feature.inventory.generated.resources.inv_dash_total_items
import ampairsapp.feature.inventory.generated.resources.inv_dash_total_value
import ampairsapp.feature.inventory.generated.resources.inv_settings_title
import ampairsapp.feature.inventory.generated.resources.inv_view_all
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatDate
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.inventory.ui.components.KpiCard
import com.ampairs.inventory.ui.components.MonoFamily
import com.ampairs.inventory.ui.components.SyncChip
import com.ampairs.inventory.ui.components.SyncChipState
import com.ampairs.inventory.ui.components.movementIcon
import com.ampairs.inventory.ui.components.signedQty
import com.ampairs.inventory.ui.components.sourceLabel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDashboardScreen(
    onOpenItems: () -> Unit,
    onAddItem: () -> Unit,
    onOpenCount: () -> Unit,
    onOpenLedger: () -> Unit,
    onOpenLowStock: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: InventoryDashboardViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    val expanded = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.inv_dash_title)) },
                actions = {
                    SyncChip(
                        if (state.syncState == DashboardSyncState.SYNCING) SyncChipState.PENDING else SyncChipState.SYNCED,
                        Modifier.padding(end = 4.dp),
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.inv_settings_title))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // KPI cards
            val totalCard: @Composable (Modifier) -> Unit = { m ->
                KpiCard(stringResource(Res.string.inv_dash_total_items), state.totalItems.toString(), m)
            }
            val valueCard: @Composable (Modifier) -> Unit = { m ->
                KpiCard(stringResource(Res.string.inv_dash_total_value), com.ampairs.common.locale.formatMoney(state.totalStockValue, locale), m)
            }
            val lowCard: @Composable (Modifier) -> Unit = { m ->
                KpiCard(
                    stringResource(Res.string.inv_dash_low_stock), state.lowStockCount.toString(), m,
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            val outCard: @Composable (Modifier) -> Unit = { m ->
                KpiCard(
                    stringResource(Res.string.inv_dash_out_of_stock), state.outOfStockCount.toString(), m,
                    container = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (expanded) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    totalCard(Modifier.weight(1f)); valueCard(Modifier.weight(1f)); lowCard(Modifier.weight(1f)); outCard(Modifier.weight(1f))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { totalCard(Modifier.weight(1f)); valueCard(Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { lowCard(Modifier.weight(1f)); outCard(Modifier.weight(1f)) }
            }

            // Quick actions
            Text(stringResource(Res.string.inv_dash_quick_actions), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Icons.Filled.Add, stringResource(Res.string.inv_action_add_item), Modifier.weight(1f), onAddItem)
                QuickAction(Icons.Filled.Tune, stringResource(Res.string.inv_action_adjust), Modifier.weight(1f), onOpenItems)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Icons.Filled.FactCheck, stringResource(Res.string.inv_action_count), Modifier.weight(1f), onOpenCount)
                QuickAction(Icons.Filled.NotificationImportant, stringResource(Res.string.inv_action_low_stock), Modifier.weight(1f), onOpenLowStock)
            }

            // Recent movements
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.inv_dash_recent_movements), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.clickable(onClick = onOpenLedger), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.inv_view_all), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(Res.string.inv_action_ledger), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    if (state.recentMovements.isEmpty()) {
                        Text(
                            stringResource(Res.string.inv_dash_recent_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        state.recentMovements.forEach { m ->
                            RecentRow(m, state.itemNames[m.inventoryItemId] ?: "", formatDate(m.transactionDate, locale))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(11.dp)) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                }
            }
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RecentRow(m: InventoryMovement, itemName: String, date: String) {
    val signed = m.signedQuantity
    val qtyColor = if (signed < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(movementIcon(m.transactionType, m.sourceType), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(itemName.ifBlank { sourceLabel(m.sourceType, m.referenceNumber) }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, fontWeight = FontWeight.Medium)
            Text(sourceLabel(m.sourceType, m.referenceNumber) + " · " + date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Text(signedQty(signed), style = MaterialTheme.typography.titleMedium, fontFamily = MonoFamily, color = qtyColor)
    }
}
