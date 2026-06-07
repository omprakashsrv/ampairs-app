package com.ampairs.order.ui

import ampairsapp.feature.order.generated.resources.Res
import ampairsapp.feature.order.generated.resources.ord_edit_discount_label
import ampairsapp.feature.order.generated.resources.ord_edit_next
import ampairsapp.feature.order.generated.resources.ord_edit_ok
import ampairsapp.feature.order.generated.resources.ord_edit_price_label
import ampairsapp.feature.order.generated.resources.ord_edit_save
import ampairsapp.feature.order.generated.resources.ord_edit_taxable
import ampairsapp.feature.order.generated.resources.ord_edit_line_total
import ampairsapp.feature.order.generated.resources.ord_edit_unit_label
import ampairsapp.feature.order.generated.resources.ord_edit_unit_base
import ampairsapp.feature.order.generated.resources.ord_edit_base_qty
import ampairsapp.feature.order.generated.resources.ord_edit_has_variants
import ampairsapp.feature.order.generated.resources.ord_create_cd
import ampairsapp.feature.order.generated.resources.ord_create_title
import ampairsapp.feature.order.generated.resources.ord_create_name
import ampairsapp.feature.order.generated.resources.ord_create_code
import ampairsapp.feature.order.generated.resources.ord_create_price
import ampairsapp.feature.order.generated.resources.ord_create_mrp
import ampairsapp.feature.order.generated.resources.ord_create_tax
import ampairsapp.feature.order.generated.resources.ord_create_confirm
import ampairsapp.feature.order.generated.resources.ord_create_cancel
import ampairsapp.feature.order.generated.resources.ord_cust_from
import ampairsapp.feature.order.generated.resources.ord_cust_to
import ampairsapp.feature.order.generated.resources.ord_cust_select
import ampairsapp.feature.order.generated.resources.ord_cust_title
import ampairsapp.feature.order.generated.resources.ord_cust_search
import ampairsapp.feature.order.generated.resources.ord_view_discount
import ampairsapp.feature.order.generated.resources.ord_view_items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
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
import com.ampairs.order.domain.Discount
import com.ampairs.order.domain.OrderItem
import com.ampairs.unit.data.repository.UnitOption
import com.ampairs.order.viewmodel.OrderViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    fromCustomerId: String?,
    toCustomerId: String?,
    id: String?,
    onOrderSaved: (String) -> Unit,
    productPickerSlot: @Composable (onProductClick: (String) -> Unit) -> Unit = {},
    viewModel: OrderViewModel = assistedMetroViewModel<OrderViewModel, OrderViewModel.Factory> { create(fromCustomerId, toCustomerId, id) }
) {
    val orderViewModel = viewModel

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
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.ord_create_cd))
            }
        },
        bottomBar = {
        val orderItems = orderViewModel.orderItems
        if (orderItems.size > 0) {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    stringResource(Res.string.ord_view_items, orderViewModel.order.totalItems),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        orderViewModel.order.totalCost.toDecimal(),
                        modifier = Modifier.align(Alignment.End).padding(12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                ElevatedButton(onClick = {
                    if (!scaffoldState.bottomSheetState.isVisible) {
                        scope.launch { bottomSheetState.expand() }
                    } else {
                        orderViewModel.saveOrder(onOrderSaved)
                    }
                }) {
                    if (orderViewModel.savingOrder)
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(24.dp)
                        )
                    else Text(
                        if (scaffoldState.bottomSheetState.isVisible)
                            stringResource(Res.string.ord_edit_save)
                        else
                            stringResource(Res.string.ord_edit_next)
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
                                fromName = orderViewModel.fromCustomerName,
                                toName = orderViewModel.toCustomerName,
                                results = orderViewModel.customerResults,
                                onSearch = orderViewModel::searchCustomers,
                                onPickFrom = orderViewModel::selectFromCustomer,
                                onPickTo = orderViewModel::selectToCustomer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                        item {
                            OrderTotalsPanel(
                                totals = orderViewModel.totals,
                                overallDiscountKind = orderViewModel.overallDiscountKind,
                                overallDiscountAmount = orderViewModel.overallDiscountAmount,
                                onPriceMode = orderViewModel::selectPriceMode,
                                onOverallDiscount = orderViewModel::setOverallDiscount,
                                onOverallDiscountMode = orderViewModel::selectOverallDiscountMode,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                        items(orderViewModel.orderItems.size) { index ->
                            val orderItem = orderViewModel.orderItems[index]
                            ListItem(
                                modifier = Modifier.clickable {
                                    orderViewModel.selectedOrderItem = orderItem
                                },
                                headlineContent = {
                                    Text(
                                        orderItem.description,
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
                                    if (orderItem.productPrice > orderItem.price) {
                                        Row {
                                            Text(
                                                orderItem.productPrice.toDecimal(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    textDecoration = TextDecoration.LineThrough,
                                                )
                                            )
                                            Text(
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                                text = orderItem.price.toDecimal(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    } else {
                                        Text(
                                            orderItem.price.toDecimal(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    if (orderItem.discountPercent > 0) {
                                        Text(
                                            "${stringResource(Res.string.ord_view_discount)}: ${orderItem.discountPercent.toDecimal()}%",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                },
                                trailingContent = {
                                    Column {
                                        CartItem(
                                            (orderItem.productId + orderItem.quantity),
                                            orderItem.quantity,
                                            width = 40.dp
                                        ) {
                                            orderItem.quantity = it
                                            if (it <= 0) {
                                                orderViewModel.orderItems.remove(orderItem)
                                            }
                                            orderViewModel.order.updateTotalCost()
                                            orderViewModel.recalculate()
                                        }
                                        Text(
                                            orderItem.totalCost.toDecimal(),
                                            modifier = Modifier.align(Alignment.End),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                })
                        }
                    }
                }) { innerPadding ->
                productPickerSlot { productId -> orderViewModel.addProduct(productId) }
            }
        }

        if (orderViewModel.selectedOrderItem != null) {
            AlertDialog(
                title = {
                    Text(text = orderViewModel.selectedOrderItem?.product?.name ?: "")
                },
                text = {
                    Column {
                        val selItem = orderViewModel.selectedOrderItem
                        LaunchedEffect(selItem?.id) {
                            if (selItem != null) orderViewModel.loadUnitOptions(selItem)
                        }
                        if (selItem != null) {
                            UnitVariantRow(
                                item = selItem,
                                options = orderViewModel.unitOptions,
                                onUnitSelected = { orderViewModel.selectUnit(selItem, it) },
                            )
                        }
                        Row {
                            Text(text = stringResource(Res.string.ord_edit_discount_label))
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                CartItem(
                                    id = orderViewModel.selectedOrderItem?.product?.id + orderViewModel.selectedOrderItem?.product?.quantity,
                                    qty = orderViewModel.selectedOrderItem?.discountPercent ?: 0.0
                                ) { discountPercent ->
                                    orderViewModel.selectedOrderItem?.discountPercent = discountPercent
                                    val productPrice = orderViewModel.selectedOrderItem?.productPrice ?: 0.0
                                    orderViewModel.selectedOrderItem?.price =
                                        productPrice * (if (discountPercent > 0) 1 - (discountPercent / 100) else 1.0)
                                    orderViewModel.selectedOrderItem?.discount?.clear()
                                    if (discountPercent > 0) {
                                        orderViewModel.selectedOrderItem?.discount?.add(
                                            Discount(
                                                discountPercent,
                                                productPrice - (orderViewModel.selectedOrderItem?.price ?: 0.0)
                                            )
                                        )
                                    }
                                    orderViewModel.selectedOrderItem?.updateTotal()
                                    orderViewModel.order.updateDiscount()
                                    orderViewModel.order.updateTotalCost()
                                    orderViewModel.recalculate()
                                }
                            }
                        }

                        Row {
                            Text(text = stringResource(Res.string.ord_edit_price_label))
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                CartItem(
                                    id = orderViewModel.selectedOrderItem?.product?.id + "SP" + orderViewModel.selectedOrderItem?.product?.quantity,
                                    qty = orderViewModel.selectedOrderItem?.price ?: 0.0
                                ) { price ->
                                    orderViewModel.selectedOrderItem?.price = price
                                    orderViewModel.selectedOrderItem?.updateTotal()
                                    val productPrice = (orderViewModel.selectedOrderItem?.productPrice ?: 0.0)
                                    val discount = productPrice - price
                                    val discountPercent = if (discount > 0.0) ((discount * 100) / productPrice) else 0.0
                                    orderViewModel.selectedOrderItem?.discountPercent = discountPercent
                                    orderViewModel.selectedOrderItem?.discount?.clear()
                                    if (discountPercent > 0) {
                                        orderViewModel.selectedOrderItem?.discount?.add(
                                            Discount(
                                                discountPercent,
                                                productPrice - (orderViewModel.selectedOrderItem?.price ?: 0.0)
                                            )
                                        )
                                    }
                                    orderViewModel.order.updateDiscount()
                                    orderViewModel.order.updateTotalCost()
                                    orderViewModel.recalculate()
                                }
                            }
                        }

                        Row {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = "${stringResource(Res.string.ord_view_discount)} @ ${orderViewModel.selectedOrderItem?.discountPercent.toDecimal()} %: "
                            )
                            LazyColumn {
                                items(orderViewModel.selectedOrderItem?.discount?.size ?: 0) { discountIndex ->
                                    Text(
                                        modifier = Modifier.padding(4.dp),
                                        text = orderViewModel.selectedOrderItem?.discount?.get(discountIndex)?.value.toDecimal()
                                    )
                                }
                            }
                        }

                        val sel = orderViewModel.selectedOrderItem
                        if (sel != null) {
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            BreakRow(stringResource(Res.string.ord_edit_taxable), sel.basePrice.toDecimal())
                            sel.taxInfos.forEach { ti ->
                                BreakRow("${ti.name} ${ti.percentage.toDecimal()}%", (ti.value ?: 0.0).toDecimal())
                            }
                            BreakRow(stringResource(Res.string.ord_edit_line_total), sel.totalCost.toDecimal(), bold = true)
                        }
                    }
                },
                onDismissRequest = { orderViewModel.selectedOrderItem = null },
                confirmButton = {
                    TextButton(onClick = { orderViewModel.selectedOrderItem = null }) {
                        Text(stringResource(Res.string.ord_edit_ok))
                    }
                }
            )
        }

        if (showCreateProduct) {
            CreateProductDialog(
                onDismiss = { showCreateProduct = false },
                onCreate = { name, code, price, mrp, tax ->
                    orderViewModel.createAndAddProduct(name, code, price, mrp, tax)
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
        title = { Text(stringResource(Res.string.ord_create_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.ord_create_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code, onValueChange = { code = it },
                    label = { Text(stringResource(Res.string.ord_create_code)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(Res.string.ord_create_price)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = mrp, onValueChange = { mrp = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(Res.string.ord_create_mrp)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tax, onValueChange = { tax = it },
                    label = { Text(stringResource(Res.string.ord_create_tax)) },
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
            ) { Text(stringResource(Res.string.ord_create_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ord_create_cancel)) }
        }
    )
}

@Composable
private fun UnitVariantRow(
    item: OrderItem,
    options: List<UnitOption>,
    onUnitSelected: (UnitOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(Res.string.ord_edit_unit_label))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            var expanded by remember { mutableStateOf(false) }
            val currentLabel = item.unitName.ifBlank {
                options.firstOrNull { it.unitId == item.unitId }?.shortName
                    ?: stringResource(Res.string.ord_edit_unit_base)
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
                                    if (option.isBase) "${option.shortName} (${stringResource(Res.string.ord_edit_unit_base)})"
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
                    "${stringResource(Res.string.ord_edit_base_qty)}: ${item.baseQuantity.toDecimal()}",
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
                label = { Text(item.variantSku ?: stringResource(Res.string.ord_edit_has_variants)) }
            )
        }
    }
}

@Composable
private fun BreakRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            CustomerRow(stringResource(Res.string.ord_cust_from), fromName) { onSearch(""); picking = "from" }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            CustomerRow(stringResource(Res.string.ord_cust_to), toName) { onSearch(""); picking = "to" }
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
            name.ifBlank { stringResource(Res.string.ord_cust_select) },
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
        title = { Text(stringResource(Res.string.ord_cust_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    placeholder = { Text(stringResource(Res.string.ord_cust_search)) },
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
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ord_edit_ok)) }
        }
    )
}
