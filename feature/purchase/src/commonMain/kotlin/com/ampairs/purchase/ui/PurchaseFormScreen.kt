package com.ampairs.purchase.ui

import ampairsapp.feature.purchase.generated.resources.Res
import ampairsapp.feature.purchase.generated.resources.pur_form_add_item
import ampairsapp.feature.purchase.generated.resources.pur_form_cd_remove_item
import ampairsapp.feature.purchase.generated.resources.pur_form_create_title
import ampairsapp.feature.purchase.generated.resources.pur_form_edit_title
import ampairsapp.feature.purchase.generated.resources.pur_form_items
import ampairsapp.feature.purchase.generated.resources.pur_form_price
import ampairsapp.feature.purchase.generated.resources.pur_form_product_search
import ampairsapp.feature.purchase.generated.resources.pur_form_qty
import ampairsapp.feature.purchase.generated.resources.pur_form_save
import ampairsapp.feature.purchase.generated.resources.pur_form_status
import ampairsapp.feature.purchase.generated.resources.pur_form_supplier
import ampairsapp.feature.purchase.generated.resources.pur_form_supplier_invoice
import ampairsapp.feature.purchase.generated.resources.pur_form_supplier_search
import ampairsapp.feature.purchase.generated.resources.pur_form_total
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.purchase.domain.PurchaseStatus
import com.ampairs.purchase.viewmodel.PurchaseItemFormState
import com.ampairs.purchase.viewmodel.PurchaseFormViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Self-contained purchase editor: supplier picker, a product-search line composer, per-line qty/cost
 * editing, status selection and a running total. UID is generated in the ViewModel (PUR prefix);
 * Save persists the parent + items atomically (offline-first).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFormScreen(
    purchaseId: String? = null,
    onPurchaseSaved: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PurchaseFormViewModel = assistedMetroViewModel<PurchaseFormViewModel, PurchaseFormViewModel.Factory>(
        key = purchaseId ?: "new"
    ) { create(purchaseId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current

    var supplierQuery by remember { mutableStateOf("") }
    var productQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEdit) stringResource(Res.string.pur_form_edit_title)
                        else stringResource(Res.string.pur_form_create_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onPurchaseSaved) },
                        enabled = !state.isSaving && state.supplierId.isNotBlank() && state.items.isNotEmpty()
                    ) {
                        Text(stringResource(Res.string.pur_form_save))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── supplier ──
            item {
                Text(
                    text = stringResource(Res.string.pur_form_supplier),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.supplierId.isNotBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(state.supplierName) },
                    )
                }
                OutlinedTextField(
                    value = supplierQuery,
                    onValueChange = { supplierQuery = it; viewModel.searchSuppliers(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.pur_form_supplier_search)) }
                )
                state.supplierResults.take(6).forEach { s ->
                    Text(
                        text = s.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectSupplier(s)
                                supplierQuery = ""
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            }

            // ── supplier invoice number + status ──
            item {
                OutlinedTextField(
                    value = state.supplierInvoiceNumber,
                    onValueChange = viewModel::setSupplierInvoiceNumber,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(Res.string.pur_form_supplier_invoice)) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.pur_form_status),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PurchaseStatus.entries.filter { it != PurchaseStatus.CANCELLED }.forEach { st ->
                        FilterChip(
                            selected = state.status == st,
                            onClick = { viewModel.setStatus(st) },
                            label = { Text(st.name.lowercase().replaceFirstChar { it.uppercaseChar() }) }
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            // ── product composer ──
            item {
                Text(
                    text = stringResource(Res.string.pur_form_add_item),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = productQuery,
                    onValueChange = { productQuery = it; viewModel.searchProducts(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.pur_form_product_search)) }
                )
                state.productResults.take(6).forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addProduct(p)
                                productQuery = ""
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(p.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            formatMoney(if (p.dp > 0.0) p.dp else p.sellingPrice, locale),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // ── line items ──
            item {
                Text(
                    text = stringResource(Res.string.pur_form_items, state.items.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(state.items, key = { it.id }) { line ->
                LineItemEditor(
                    line = line,
                    onQuantityChange = { viewModel.updateQuantity(line.id, it) },
                    onPriceChange = { viewModel.updatePrice(line.id, it) },
                    onRemove = { viewModel.removeItem(line.id) }
                )
            }

            // ── total ──
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.pur_form_total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatMoney(state.totalCost, locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun LineItemEditor(
    line: PurchaseItemFormState,
    onQuantityChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    val locale = LocalAppLocale.current
    Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = line.name.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.pur_form_cd_remove_item))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = if (line.quantity == 0.0) "" else line.quantity.toString(),
                    onValueChange = { onQuantityChange(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text(stringResource(Res.string.pur_form_qty)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.width(110.dp)
                )
                OutlinedTextField(
                    value = if (line.price == 0.0) "" else line.price.toString(),
                    onValueChange = { onPriceChange(it.toDoubleOrNull() ?: 0.0) },
                    label = { Text(stringResource(Res.string.pur_form_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatMoney(line.lineTotal, locale),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
