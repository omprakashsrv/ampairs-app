package com.ampairs.inventory.ui.lowstock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import ampairsapp.feature.inventory.generated.resources.inv_detail_on_hand
import ampairsapp.feature.inventory.generated.resources.inv_detail_reorder
import ampairsapp.feature.inventory.generated.resources.inv_empty_low_desc
import ampairsapp.feature.inventory.generated.resources.inv_empty_low_title
import ampairsapp.feature.inventory.generated.resources.inv_low_alerts_off
import ampairsapp.feature.inventory.generated.resources.inv_low_alerts_on
import ampairsapp.feature.inventory.generated.resources.inv_low_attention
import ampairsapp.feature.inventory.generated.resources.inv_low_create_order
import ampairsapp.feature.inventory.generated.resources.inv_low_stock_in
import ampairsapp.feature.inventory.generated.resources.inv_low_title
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.ui.adjust.AdjustStockSheet
import com.ampairs.inventory.ui.components.InventoryEmptyState
import com.ampairs.inventory.ui.components.MonoFamily
import com.ampairs.inventory.ui.components.StockBadge
import com.ampairs.inventory.ui.components.formatQty
import com.ampairs.inventory.ui.components.stockStatus
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockScreen(
    onNavigateBack: () -> Unit,
    onCreateOrder: () -> Unit,
    viewModel: LowStockViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var adjustItem by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.inv_low_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.inv_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.items.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.NotificationImportant, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(Res.string.inv_low_attention, state.items.size) + " · " +
                            stringResource(if (state.alertsEnabled) Res.string.inv_low_alerts_on else Res.string.inv_low_alerts_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            if (state.items.isEmpty()) {
                InventoryEmptyState(
                    icon = Icons.Filled.TaskAlt,
                    title = stringResource(Res.string.inv_empty_low_title),
                    description = stringResource(Res.string.inv_empty_low_desc),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.items, key = { it.uid }) { item ->
                        LowStockCard(
                            item = item,
                            onStockIn = { adjustItem = item.uid },
                            onCreateOrder = onCreateOrder,
                        )
                    }
                }
            }
        }
    }

    adjustItem?.let { id ->
        AdjustStockSheet(itemId = id, onDismiss = { adjustItem = null })
    }
}

@Composable
private fun LowStockCard(item: InventoryItem, onStockIn: () -> Unit, onCreateOrder: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1)
                StockBadge(item.stockStatus())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LabeledValue(stringResource(Res.string.inv_detail_on_hand), formatQty(item.currentStock))
                LabeledValue(stringResource(Res.string.inv_detail_reorder), formatQty(item.reorderLevel))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStockIn, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.SouthWest, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource(Res.string.inv_low_stock_in), modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = onCreateOrder, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource(Res.string.inv_low_create_order), modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = MonoFamily, fontWeight = FontWeight.Medium)
    }
}
