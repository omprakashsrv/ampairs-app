package com.ampairs.invoice.ui

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.inv_edit_discount_label
import ampairsapp.feature.invoice.generated.resources.inv_edit_next
import ampairsapp.feature.invoice.generated.resources.inv_edit_ok
import ampairsapp.feature.invoice.generated.resources.inv_edit_price_label
import ampairsapp.feature.invoice.generated.resources.inv_edit_save
import ampairsapp.feature.invoice.generated.resources.inv_edit_unit_label
import ampairsapp.feature.invoice.generated.resources.inv_edit_unit_base
import ampairsapp.feature.invoice.generated.resources.inv_edit_base_qty
import ampairsapp.feature.invoice.generated.resources.inv_edit_has_variants
import ampairsapp.feature.invoice.generated.resources.inv_create_cd
import ampairsapp.feature.invoice.generated.resources.inv_create_title
import ampairsapp.feature.invoice.generated.resources.inv_create_name
import ampairsapp.feature.invoice.generated.resources.inv_create_code
import ampairsapp.feature.invoice.generated.resources.inv_create_price
import ampairsapp.feature.invoice.generated.resources.inv_create_mrp
import ampairsapp.feature.invoice.generated.resources.inv_create_tax
import ampairsapp.feature.invoice.generated.resources.inv_create_confirm
import ampairsapp.feature.invoice.generated.resources.inv_create_cancel
import ampairsapp.feature.invoice.generated.resources.inv_cust_from
import ampairsapp.feature.invoice.generated.resources.inv_cust_to
import ampairsapp.feature.invoice.generated.resources.inv_cust_select
import ampairsapp.feature.invoice.generated.resources.inv_cust_title
import ampairsapp.feature.invoice.generated.resources.inv_cust_search
import ampairsapp.feature.invoice.generated.resources.inv_view_discount
import ampairsapp.feature.invoice.generated.resources.inv_view_items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ampairs.common.components.CartItem
import com.ampairs.common.format.toDecimal
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.invoice.domain.Discount
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.unit.data.repository.UnitOption
import com.ampairs.invoice.viewmodel.InvoiceViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    fromCustomerId: String?,
    toCustomerId: String?,
    id: String?,
    onInvoiceSaved: (String) -> Unit,
    productPickerSlot: @Composable (onProductClick: (String) -> Unit) -> Unit = {},
    viewModel: InvoiceViewModel = assistedMetroViewModel<InvoiceViewModel, InvoiceViewModel.Factory> { create(fromCustomerId, toCustomerId, id) }
) {
    val invoiceViewModel = viewModel

    val scope = rememberCoroutineScope()
    var showCreateProduct by remember { mutableStateOf(false) }
    val bottomSheetState =
        rememberStandardBottomSheetState(initialValue = SheetValue.Hidden, skipHiddenState = false)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateProduct = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.inv_create_cd))
            }
        },
        bottomBar = {
        val invoiceItems = invoiceViewModel.invoiceItems
        if (invoiceItems.size > 0) {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    stringResource(Res.string.inv_view_items, invoiceViewModel.invoice.totalItems),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        invoiceViewModel.invoice.totalCost.toDecimal(),
                        modifier = Modifier.align(Alignment.End).padding(12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                ElevatedButton(onClick = {
                    if (!scaffoldState.bottomSheetState.isVisible) {
                        scope.launch { bottomSheetState.expand() }
                    } else {
                        invoiceViewModel.saveInvoice(onInvoiceSaved)
                    }
                }) {
                    if (invoiceViewModel.savingInvoice)
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(24.dp)
                        )
                    else Text(
                        if (scaffoldState.bottomSheetState.isVisible)
                            stringResource(Res.string.inv_edit_save)
                        else
                            stringResource(Res.string.inv_edit_next)
                    )
                }
            }
        }
    }) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            BottomSheetScaffold(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        scope.launch {
                            if (bottomSheetState.isVisible) {
                                bottomSheetState.hide()
                            }
                        }
                    })
                },
                scaffoldState = scaffoldState,
                sheetContent = {
                    LazyColumn {
                        item {
                            CustomerSelectSection(
                                fromName = invoiceViewModel.fromCustomerName,
                                toName = invoiceViewModel.toCustomerName,
                                results = invoiceViewModel.customerResults,
                                onSearch = invoiceViewModel::searchCustomers,
                                onPickFrom = invoiceViewModel::selectFromCustomer,
                                onPickTo = invoiceViewModel::selectToCustomer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                        item {
                            InvoiceTotalsPanel(
                                totals = invoiceViewModel.totals,
                                overallDiscountKind = invoiceViewModel.overallDiscountKind,
                                overallDiscountAmount = invoiceViewModel.overallDiscountAmount,
                                onPriceMode = invoiceViewModel::selectPriceMode,
                                onOverallDiscount = invoiceViewModel::setOverallDiscount,
                                onOverallDiscountMode = invoiceViewModel::selectOverallDiscountMode,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                        items(invoiceViewModel.invoiceItems.size) { index ->
                            val invoiceItem = invoiceViewModel.invoiceItems[index]
                            ListItem(
                                modifier = Modifier.clickable {
                                    invoiceViewModel.selectedInvoiceItem = invoiceItem
                                },
                                headlineContent = {
                                    Text(
                                        invoiceItem.description,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingContent = {
                                    Text(
                                        (index + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                supportingContent = {
                                    if (invoiceItem.productPrice > invoiceItem.price) {
                                        Row {
                                            Text(
                                                invoiceItem.productPrice.toDecimal(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    textDecoration = TextDecoration.LineThrough,
                                                )
                                            )
                                            Text(
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                text = invoiceItem.price.toDecimal(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    } else {
                                        Text(
                                            invoiceItem.price.toDecimal(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    if (invoiceItem.discountPercent > 0) {
                                        Text(
                                            "${stringResource(Res.string.inv_view_discount)}: ${invoiceItem.discountPercent.toDecimal()}%",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                },
                                trailingContent = {
                                    Column {
                                        CartItem(
                                            (invoiceItem.productId + invoiceItem.quantity),
                                            invoiceItem.quantity,
                                            width = 40.dp
                                        ) {
                                            invoiceItem.quantity = it
                                            if (it <= 0) {
                                                invoiceViewModel.invoiceItems.remove(invoiceItem)
                                            }
                                            invoiceViewModel.invoice.updateTotalCost()
                                            invoiceViewModel.recalculate()
                                        }
                                        Text(
                                            invoiceItem.totalCost.toDecimal(),
                                            modifier = Modifier.align(Alignment.End),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                })
                        }
                    }
                }) { _ ->
                productPickerSlot { productId -> invoiceViewModel.addProduct(productId) }
            }
        }

        if (invoiceViewModel.selectedInvoiceItem != null) {
            AlertDialog(
                title = {
                    Text(text = invoiceViewModel.selectedInvoiceItem?.product?.name ?: "")
                },
                text = {
                    Column {
                        val selItem = invoiceViewModel.selectedInvoiceItem
                        LaunchedEffect(selItem?.id) {
                            if (selItem != null) invoiceViewModel.loadUnitOptions(selItem)
                        }
                        if (selItem != null) {
                            UnitVariantRow(
                                item = selItem,
                                options = invoiceViewModel.unitOptions,
                                onUnitSelected = { invoiceViewModel.selectUnit(selItem, it) },
                            )
                        }
                        Row {
                            Text(text = stringResource(Res.string.inv_edit_discount_label))
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                CartItem(
                                    id = invoiceViewModel.selectedInvoiceItem?.product?.id + invoiceViewModel.selectedInvoiceItem?.product?.quantity,
                                    qty = invoiceViewModel.selectedInvoiceItem?.discountPercent ?: 0.0
                                ) { discountPercent ->
                                    invoiceViewModel.selectedInvoiceItem?.discountPercent = discountPercent
                                    val productPrice = invoiceViewModel.selectedInvoiceItem?.productPrice ?: 0.0
                                    invoiceViewModel.selectedInvoiceItem?.price =
                                        productPrice * (if (discountPercent > 0) 1 - (discountPercent / 100) else 1.0)
                                    invoiceViewModel.selectedInvoiceItem?.discount?.clear()
                                    if (discountPercent > 0) {
                                        invoiceViewModel.selectedInvoiceItem?.discount?.add(
                                            Discount(
                                                discountPercent,
                                                productPrice - (invoiceViewModel.selectedInvoiceItem?.price ?: 0.0)
                                            )
                                        )
                                    }
                                    invoiceViewModel.selectedInvoiceItem?.updateTotal()
                                    invoiceViewModel.invoice.updateDiscount()
                                    invoiceViewModel.invoice.updateTotalCost()
                                    invoiceViewModel.recalculate()
                                }
                            }
                        }

                        Row {
                            Text(text = stringResource(Res.string.inv_edit_price_label))
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                CartItem(
                                    id = invoiceViewModel.selectedInvoiceItem?.product?.id + "SP" + invoiceViewModel.selectedInvoiceItem?.product?.quantity,
                                    qty = invoiceViewModel.selectedInvoiceItem?.price ?: 0.0
                                ) { price ->
                                    invoiceViewModel.selectedInvoiceItem?.price = price
                                    invoiceViewModel.selectedInvoiceItem?.updateTotal()
                                    val productPrice = (invoiceViewModel.selectedInvoiceItem?.productPrice ?: 0.0)
                                    val discount = productPrice - price
                                    val discountPercent = if (discount > 0.0) ((discount * 100) / productPrice) else 0.0
                                    invoiceViewModel.selectedInvoiceItem?.discountPercent = discountPercent
                                    invoiceViewModel.selectedInvoiceItem?.discount?.clear()
                                    if (discountPercent > 0) {
                                        invoiceViewModel.selectedInvoiceItem?.discount?.add(
                                            Discount(
                                                discountPercent,
                                                productPrice - (invoiceViewModel.selectedInvoiceItem?.price ?: 0.0)
                                            )
                                        )
                                    }
                                    invoiceViewModel.invoice.updateDiscount()
                                    invoiceViewModel.invoice.updateTotalCost()
                                    invoiceViewModel.recalculate()
                                }
                            }
                        }

                        Row {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = "${stringResource(Res.string.inv_view_discount)} @ ${invoiceViewModel.selectedInvoiceItem?.discountPercent.toDecimal()} %: "
                            )
                            LazyColumn {
                                items(invoiceViewModel.selectedInvoiceItem?.discount?.size ?: 0) { discountIndex ->
                                    Text(
                                        modifier = Modifier.padding(4.dp),
                                        text = invoiceViewModel.selectedInvoiceItem?.discount?.get(discountIndex)?.value.toDecimal()
                                    )
                                }
                            }
                        }
                    }
                },
                onDismissRequest = { invoiceViewModel.selectedInvoiceItem = null },
                confirmButton = {
                    TextButton(onClick = { invoiceViewModel.selectedInvoiceItem = null }) {
                        Text(stringResource(Res.string.inv_edit_ok))
                    }
                }
            )
        }

        if (showCreateProduct) {
            CreateProductDialog(
                onDismiss = { showCreateProduct = false },
                onCreate = { name, code, price, mrp, tax ->
                    invoiceViewModel.createAndAddProduct(name, code, price, mrp, tax)
                    showCreateProduct = false
                }
            )
        }
    }
}

@Composable
private fun CreateProductDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, code: String, price: Double, mrp: Double, taxCode: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var tax by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.inv_create_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.inv_create_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code, onValueChange = { code = it },
                    label = { Text(stringResource(Res.string.inv_create_code)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(Res.string.inv_create_price)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mrp, onValueChange = { mrp = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(Res.string.inv_create_mrp)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tax, onValueChange = { tax = it },
                    label = { Text(stringResource(Res.string.inv_create_tax)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(name.trim(), code.trim(), price.toDoubleOrNull() ?: 0.0, mrp.toDoubleOrNull() ?: 0.0, tax.trim())
                },
                enabled = name.isNotBlank()
            ) { Text(stringResource(Res.string.inv_create_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.inv_create_cancel)) }
        }
    )
}

@Composable
private fun UnitVariantRow(
    item: InvoiceItem,
    options: List<UnitOption>,
    onUnitSelected: (UnitOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(Res.string.inv_edit_unit_label))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            var expanded by remember { mutableStateOf(false) }
            val currentLabel = item.unitName.ifBlank {
                options.firstOrNull { it.unitId == item.unitId }?.shortName
                    ?: stringResource(Res.string.inv_edit_unit_base)
            }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = options.isNotEmpty()
                ) {
                    Text(currentLabel, style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (option.isBase) "${option.shortName} (${stringResource(Res.string.inv_edit_unit_base)})"
                                    else "${option.shortName} · ×${option.multiplier.toDecimal()}"
                                )
                            },
                            onClick = {
                                expanded = false
                                onUnitSelected(option)
                            }
                        )
                    }
                }
            }
            if (item.unitMultiplier != 1.0) {
                Text(
                    "${stringResource(Res.string.inv_edit_base_qty)}: ${item.baseQuantity.toDecimal()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (item.product?.hasVariants == true) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = {},
                label = { Text(item.variantSku ?: stringResource(Res.string.inv_edit_has_variants)) }
            )
        }
    }
}

@Composable
private fun CustomerSelectSection(
    fromName: String,
    toName: String,
    results: List<CustomerListItem>,
    onSearch: (String) -> Unit,
    onPickFrom: (String) -> Unit,
    onPickTo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf<String?>(null) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            CustomerRow(stringResource(Res.string.inv_cust_from), fromName) { onSearch(""); picking = "from" }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            CustomerRow(stringResource(Res.string.inv_cust_to), toName) { onSearch(""); picking = "to" }
        }
    }
    if (picking != null) {
        CustomerPickerDialog(
            results = results,
            onSearch = onSearch,
            onSelect = { id ->
                if (picking == "from") onPickFrom(id) else onPickTo(id)
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun CustomerRow(label: String, name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            name.ifBlank { stringResource(Res.string.inv_cust_select) },
            style = MaterialTheme.typography.bodyMedium,
            color = if (name.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CustomerPickerDialog(
    results: List<CustomerListItem>,
    onSearch: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.inv_cust_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    placeholder = { Text(stringResource(Res.string.inv_cust_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(results.size) { index ->
                        val customer = results[index]
                        Column(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(customer.id) }.padding(vertical = 10.dp)
                        ) {
                            Text(customer.name, style = MaterialTheme.typography.bodyMedium)
                            if (!customer.phone.isNullOrBlank()) {
                                Text(
                                    customer.phone!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.inv_edit_ok)) }
        }
    )
}
