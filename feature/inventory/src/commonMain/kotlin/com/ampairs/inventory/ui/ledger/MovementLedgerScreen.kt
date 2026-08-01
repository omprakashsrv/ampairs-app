package com.ampairs.inventory.ui.ledger

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import ampairsapp.feature.inventory.generated.resources.Res
import ampairsapp.feature.inventory.generated.resources.inv_back
import ampairsapp.feature.inventory.generated.resources.inv_ledger_clear_filters
import ampairsapp.feature.inventory.generated.resources.inv_ledger_empty
import ampairsapp.feature.inventory.generated.resources.inv_ledger_export
import ampairsapp.feature.inventory.generated.resources.inv_ledger_immutable
import ampairsapp.feature.inventory.generated.resources.inv_ledger_search
import ampairsapp.feature.inventory.generated.resources.inv_ledger_title
import ampairsapp.feature.inventory.generated.resources.inv_filter_all
import ampairsapp.feature.inventory.generated.resources.inv_type_adjustment
import ampairsapp.feature.inventory.generated.resources.inv_type_count
import ampairsapp.feature.inventory.generated.resources.inv_type_stock_in
import ampairsapp.feature.inventory.generated.resources.inv_type_stock_out
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.inventory.ui.components.InventoryEmptyState
import com.ampairs.inventory.ui.components.MonoFamily
import com.ampairs.inventory.ui.components.movementIcon
import com.ampairs.inventory.ui.components.signedQty
import com.ampairs.inventory.ui.components.sourceLabel
import com.ampairs.inventory.ui.detail.movementReasonLabel
import com.ampairs.common.locale.formatDate
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementLedgerScreen(
    onNavigateBack: () -> Unit,
    viewModel: MovementLedgerViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    val clipboard = LocalClipboardManager.current
    val expanded = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.inv_ledger_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.inv_back))
                    }
                },
                actions = {
                    if (expanded) {
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(buildCsv(state.movements, state.itemNames)))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = stringResource(Res.string.inv_ledger_export))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                placeholder = { Text(stringResource(Res.string.inv_ledger_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TypeChip(stringResource(Res.string.inv_filter_all), state.typeFilter == null) { viewModel.setType(null) }
                TypeChip(stringResource(Res.string.inv_type_stock_in), state.typeFilter == "STOCK_IN") { viewModel.setType("STOCK_IN") }
                TypeChip(stringResource(Res.string.inv_type_stock_out), state.typeFilter == "STOCK_OUT") { viewModel.setType("STOCK_OUT") }
                TypeChip(stringResource(Res.string.inv_type_count), state.typeFilter == "COUNT") { viewModel.setType("COUNT") }
                TypeChip(stringResource(Res.string.inv_type_adjustment), state.typeFilter == "ADJUSTMENT") { viewModel.setType("ADJUSTMENT") }
                if (state.typeFilter != null || state.search.isNotBlank()) {
                    TypeChip(stringResource(Res.string.inv_ledger_clear_filters), false) { viewModel.clearFilters() }
                }
            }
            Text(
                stringResource(Res.string.inv_ledger_immutable),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            HorizontalDivider()

            if (state.movements.isEmpty()) {
                InventoryEmptyState(icon = Icons.Filled.Search, title = stringResource(Res.string.inv_ledger_empty))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.movements, key = { it.uid }) { m ->
                        LedgerRow(m, state.itemNames[m.inventoryItemId] ?: "", formatDate(m.transactionDate, locale))
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun LedgerRow(m: InventoryMovement, itemName: String, date: String) {
    val signed = m.signedQuantity
    val qtyColor = if (signed < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(movementIcon(m.transactionType, m.sourceType), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(itemName.ifBlank { movementReasonLabel(m) }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                movementReasonLabel(m) + " · " + sourceLabel(m.sourceType, m.referenceNumber) + " · " + date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(signedQty(signed), style = MaterialTheme.typography.titleMedium, fontFamily = MonoFamily, color = qtyColor)
    }
}

private fun buildCsv(movements: List<InventoryMovement>, names: Map<String, String>): String {
    val sb = StringBuilder("date,item,type,reason,quantity,balance,reference\n")
    movements.forEach { m ->
        val item = names[m.inventoryItemId] ?: m.inventoryItemId
        sb.append("${m.transactionDate ?: ""},\"$item\",${m.transactionType},${m.transactionReason},${m.signedQuantity},${m.balanceAfter},${m.referenceNumber ?: ""}\n")
    }
    return sb.toString()
}
