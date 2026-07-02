package com.ampairs.inventory.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_back
import ampairsapp.feature.inventory.generated.resources.inv_detail_adjust
import ampairsapp.feature.inventory.generated.resources.inv_detail_available
import ampairsapp.feature.inventory.generated.resources.inv_detail_balance
import ampairsapp.feature.inventory.generated.resources.inv_detail_cost
import ampairsapp.feature.inventory.generated.resources.inv_detail_deactivate
import ampairsapp.feature.inventory.generated.resources.inv_detail_history
import ampairsapp.feature.inventory.generated.resources.inv_detail_history_empty
import ampairsapp.feature.inventory.generated.resources.inv_detail_mrp
import ampairsapp.feature.inventory.generated.resources.inv_detail_not_found
import ampairsapp.feature.inventory.generated.resources.inv_detail_on_hand
import ampairsapp.feature.inventory.generated.resources.inv_detail_pricing
import ampairsapp.feature.inventory.generated.resources.inv_detail_refresh_banner
import ampairsapp.feature.inventory.generated.resources.inv_detail_reorder
import ampairsapp.feature.inventory.generated.resources.inv_detail_reserved
import ampairsapp.feature.inventory.generated.resources.inv_detail_selling
import ampairsapp.feature.inventory.generated.resources.inv_detail_stock_value
import ampairsapp.feature.inventory.generated.resources.inv_detail_title
import ampairsapp.feature.inventory.generated.resources.edit
import ampairsapp.feature.inventory.generated.resources.inv_reason_correction
import ampairsapp.feature.inventory.generated.resources.inv_reason_count
import ampairsapp.feature.inventory.generated.resources.inv_reason_damage
import ampairsapp.feature.inventory.generated.resources.inv_reason_loss
import ampairsapp.feature.inventory.generated.resources.inv_reason_opening
import ampairsapp.feature.inventory.generated.resources.inv_reason_return
import ampairsapp.feature.inventory.generated.resources.inv_src_purchase
import ampairsapp.feature.inventory.generated.resources.inv_src_sale
import ampairsapp.feature.inventory.generated.resources.inv_type_adjustment
import ampairsapp.feature.inventory.generated.resources.inv_type_count
import ampairsapp.feature.inventory.generated.resources.inv_type_stock_in
import ampairsapp.feature.inventory.generated.resources.inv_type_stock_out
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatDate
import com.ampairs.common.locale.formatMoney
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.inventory.ui.adjust.AdjustStockSheet
import com.ampairs.inventory.ui.components.MonoFamily
import com.ampairs.inventory.ui.components.RefreshBanner
import com.ampairs.inventory.ui.components.StockBadge
import com.ampairs.inventory.ui.components.formatQty
import com.ampairs.inventory.ui.components.movementIcon
import com.ampairs.inventory.ui.components.signedQty
import com.ampairs.inventory.ui.components.sourceLabel
import com.ampairs.inventory.ui.components.stockStatus
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailScreen(
    itemId: String,
    onEditItem: (String) -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    viewModel: InventoryDetailViewModel = assistedMetroViewModel<InventoryDetailViewModel, InventoryDetailViewModel.Factory>(key = itemId) { create(itemId) },
) {
    if (itemId.isBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.inv_detail_not_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    var menuOpen by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }
    val itm = state.item

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(itm?.name ?: stringResource(Res.string.inv_detail_title), maxLines = 1) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.inv_back))
                        }
                    }
                },
                actions = {
                    if (itm != null) {
                        IconButton(onClick = { onEditItem(itm.uid) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.edit))
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.inv_detail_deactivate)) },
                                onClick = { menuOpen = false; viewModel.deactivate() },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (itm != null) {
                FloatingActionButton(onClick = { showAdjust = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = stringResource(Res.string.inv_detail_adjust))
                }
            }
        },
    ) { padding ->
        if (itm == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.inv_detail_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.showRefreshBanner) {
                RefreshBanner(
                    message = stringResource(Res.string.inv_detail_refresh_banner),
                    onRefresh = viewModel::refresh,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                        // On-hand + status
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                formatQty(itm.currentStock),
                                style = MaterialTheme.typography.displaySmall,
                                fontFamily = MonoFamily,
                                fontWeight = FontWeight.SemiBold,
                            )
                            StockBadge(itm.stockStatus())
                        }
                        // Available / reserved / value
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCell(stringResource(Res.string.inv_detail_available), formatQty(itm.availableStock), Modifier.weight(1f))
                            MetricCell(stringResource(Res.string.inv_detail_reserved), formatQty(itm.reservedStock), Modifier.weight(1f))
                            MetricCell(stringResource(Res.string.inv_detail_stock_value), formatMoney(itm.stockValue, locale), Modifier.weight(1f))
                        }
                        // Pricing
                        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(Res.string.inv_detail_pricing), style = MaterialTheme.typography.titleSmall)
                                PriceRow(stringResource(Res.string.inv_detail_cost), formatMoney(itm.costPrice, locale))
                                PriceRow(stringResource(Res.string.inv_detail_selling), formatMoney(itm.sellingPrice, locale))
                                PriceRow(stringResource(Res.string.inv_detail_mrp), formatMoney(itm.mrp, locale))
                                PriceRow(stringResource(Res.string.inv_detail_reorder), formatQty(itm.reorderLevel))
                            }
                        }
                        Text(stringResource(Res.string.inv_detail_history), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                if (state.movements.isEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.inv_detail_history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    items(state.movements, key = { it.uid }) { m -> MovementTimelineRow(m) }
                }
            }
        }
    }

    if (showAdjust && itm != null) {
        AdjustStockSheet(itemId = itm.uid, onDismiss = { showAdjust = false })
    }
}

/** Human label for a movement, preferring the reason, falling back to the type. */
@Composable
fun movementReasonLabel(m: InventoryMovement): String = stringResource(
    when (m.transactionReason) {
        "OPENING" -> Res.string.inv_reason_opening
        "COUNT_ADJUSTMENT" -> Res.string.inv_reason_count
        "CORRECTION" -> Res.string.inv_reason_correction
        "DAMAGE" -> Res.string.inv_reason_damage
        "LOSS" -> Res.string.inv_reason_loss
        "RETURN" -> Res.string.inv_reason_return
        "PURCHASE" -> Res.string.inv_src_purchase
        "SALE" -> Res.string.inv_src_sale
        else -> when (m.transactionType) {
            "STOCK_IN" -> Res.string.inv_type_stock_in
            "STOCK_OUT" -> Res.string.inv_type_stock_out
            "COUNT" -> Res.string.inv_type_count
            else -> Res.string.inv_type_adjustment
        }
    }
)

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = MonoFamily, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun PriceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = MonoFamily)
    }
}

@Composable
private fun MovementTimelineRow(m: InventoryMovement) {
    val locale = LocalAppLocale.current
    val signed = m.signedQuantity
    val qtyColor = if (signed < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxSize()) {}
            Icon(movementIcon(m.transactionType, m.sourceType), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                movementReasonLabel(m),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                sourceLabel(m.sourceType, m.referenceNumber) + " · " + formatDate(m.transactionDate, locale),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(signedQty(signed), style = MaterialTheme.typography.titleMedium, fontFamily = MonoFamily, color = qtyColor)
            Text(stringResource(Res.string.inv_detail_balance, formatQty(m.balanceAfter)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
