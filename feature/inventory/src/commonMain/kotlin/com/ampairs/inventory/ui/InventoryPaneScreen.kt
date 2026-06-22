package com.ampairs.inventory.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import ampairsapp.feature.inventory.generated.resources.inv_dash_low_stock
import ampairsapp.feature.inventory.generated.resources.inv_dash_out_of_stock
import ampairsapp.feature.inventory.generated.resources.inv_dash_total_value
import ampairsapp.feature.inventory.generated.resources.inv_list_add_inventory
import ampairsapp.feature.inventory.generated.resources.inv_list_empty_title
import ampairsapp.feature.inventory.generated.resources.inv_list_search_placeholder
import ampairsapp.feature.inventory.generated.resources.inv_list_title
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.inventory.domain.InventoryItem
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryPaneScreen(
    viewModel: InventoryListViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current

    var showAddDialog by remember { mutableStateOf(false) }
    var adjustTarget by remember { mutableStateOf<InventoryItem?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.inv_list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.inv_list_add_inventory))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Dashboard summary
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(stringResource(Res.string.inv_dash_low_stock), uiState.lowStockCount.toString(), Modifier.weight(1f))
                StatCard(stringResource(Res.string.inv_dash_out_of_stock), uiState.outOfStockCount.toString(), Modifier.weight(1f))
                StatCard(stringResource(Res.string.inv_dash_total_value), formatMoney(uiState.totalStockValue, locale), Modifier.weight(1f))
            }

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text(stringResource(Res.string.inv_list_search_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            if (uiState.items.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(Res.string.inv_list_empty_title), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.items, key = { it.uid }) { item ->
                        InventoryRow(item = item, locale = locale, onClick = { adjustTarget = item })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, sku, sellingPrice, costPrice, stock, reorder ->
                viewModel.saveItem(
                    InventoryItem(
                        name = name,
                        sku = sku.ifBlank { null },
                        sellingPrice = sellingPrice,
                        costPrice = costPrice,
                        currentStock = stock,
                        availableStock = stock,
                        reorderLevel = reorder,
                        active = true,
                    )
                )
                showAddDialog = false
            },
        )
    }

    adjustTarget?.let { target ->
        AdjustStockDialog(
            item = target,
            onDismiss = { adjustTarget = null },
            onApply = { qty, stockIn, reason ->
                viewModel.adjustStock(target, qty, stockIn, reason)
                adjustTarget = null
            },
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InventoryRow(item: InventoryItem, locale: com.ampairs.common.locale.AppLocale, onClick: () -> Unit) {
    val stockColor = when {
        item.isOutOfStock -> MaterialTheme.colorScheme.error
        item.isLowStock -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(formatMoney(item.sellingPrice, locale), style = MaterialTheme.typography.bodySmall)
        }
        Text(item.currentStock.toString(), style = MaterialTheme.typography.titleMedium, color = stockColor)
    }
}
