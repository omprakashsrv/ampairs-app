package com.ampairs.purchase.ui

import ampairsapp.feature.purchase.generated.resources.Res
import ampairsapp.feature.purchase.generated.resources.pur_detail_delete
import ampairsapp.feature.purchase.generated.resources.pur_detail_edit
import ampairsapp.feature.purchase.generated.resources.pur_detail_items
import ampairsapp.feature.purchase.generated.resources.pur_detail_supplier
import ampairsapp.feature.purchase.generated.resources.pur_detail_supplier_invoice
import ampairsapp.feature.purchase.generated.resources.pur_detail_title
import ampairsapp.feature.purchase.generated.resources.pur_detail_total
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.purchase.db.entity.PurchaseItemEntity
import com.ampairs.purchase.viewmodel.PurchaseDetailsViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailsScreen(
    purchaseId: String,
    onEdit: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PurchaseDetailsViewModel = assistedMetroViewModel<PurchaseDetailsViewModel, PurchaseDetailsViewModel.Factory>(
        key = purchaseId
    ) { create(purchaseId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    val purchase = state.purchase

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(purchase?.purchase_number?.ifBlank { stringResource(Res.string.pur_detail_title) } ?: stringResource(Res.string.pur_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(purchaseId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.pur_detail_edit))
                    }
                    IconButton(onClick = { viewModel.deletePurchase(onNavigateBack) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.pur_detail_delete))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (purchase != null) {
                Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.pur_detail_supplier),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = purchase.supplier_name.ifBlank { "—" },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                purchase.supplier_invoice_number?.takeIf { it.isNotBlank() }?.let { supplierInvoiceNumber ->
                                    Text(
                                        text = stringResource(
                                            Res.string.pur_detail_supplier_invoice,
                                            supplierInvoiceNumber
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            PurchaseStatusChip(purchase.status)
                        }
                    }
                }
                HorizontalDivider()
                Text(
                    text = stringResource(Res.string.pur_detail_items, state.items.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.items, key = { it.id }) { item ->
                        PurchaseItemRow(item)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.pur_detail_total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatMoney(purchase.total_cost, locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseItemRow(item: PurchaseItemEntity) {
    val locale = LocalAppLocale.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.description.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.quantity} × ${formatMoney(item.price, locale)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatMoney(item.total_cost, locale),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
