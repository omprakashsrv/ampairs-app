package com.ampairs.invoice.ui

import Route
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.ampairs.common.components.CartItem
import com.ampairs.common.format.toDecimal
import com.ampairs.invoice.domain.Discount
import com.ampairs.invoice.viewmodel.InvoiceViewModel
import com.ampairs.product.productNavigation
import com.ampairs.product.ui.product.ProductViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    fromCustomerId: String?,
    toCustomerId: String?,
    id: String?,
    onInvoiceSaved: (String) -> Unit
) {

    val productViewModel: ProductViewModel = koinInject()
    val invoiceViewModel: InvoiceViewModel = koinInject { parametersOf(fromCustomerId, toCustomerId, id) }

    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberStandardBottomSheetState(initialValue = SheetValue.Hidden, skipHiddenState = false)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    productViewModel.onProductQtyChangeCallback = { products ->
        invoiceViewModel.updateInvoiceItems(products)
        scope.launch {
            bottomSheetState.hide()
        }
    }

    Scaffold(bottomBar = {
        val invoiceItems = invoiceViewModel.invoiceItems
        if (invoiceItems.size > 0) {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    invoiceViewModel.invoice.totalItems.toString() + " Items",
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
                    else Text(if (scaffoldState.bottomSheetState.isVisible) "Save" else "Next")
                }
            }
        }
    }) {
        Box(modifier = Modifier.padding(it)) {
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
                                            "Discount : " + invoiceItem.discountPercent.toDecimal() + "%",
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
                                            invoiceViewModel.updateTaxInfos()
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
                }) {

                val navController = rememberNavController()
                NavHost(
                    // Assign the navigator to the NavHost
                    navController = navController,
                    // The start destination
                    startDestination = Route.Product,
                ) {
                    productNavigation(navController)
                }
            }

        }

        if (invoiceViewModel.selectedInvoiceItem != null) {
            AlertDialog(
                title = {
                    Text(text = invoiceViewModel.selectedInvoiceItem?.product?.name ?: "")
                },
                text = {
                    Column {
                        Row {
                            Text(text = "Discount % : ")
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                CartItem(
                                    id = invoiceViewModel.selectedInvoiceItem?.product?.id + invoiceViewModel.selectedInvoiceItem?.product?.quantity,
                                    qty = invoiceViewModel.selectedInvoiceItem?.discountPercent
                                        ?: 0.0
                                ) { discountPercent ->
                                    invoiceViewModel.selectedInvoiceItem?.discountPercent =
                                        discountPercent
                                    val productPrice =
                                        invoiceViewModel.selectedInvoiceItem?.productPrice ?: 0.0
                                    invoiceViewModel.selectedInvoiceItem?.price =
                                        productPrice * (if (discountPercent > 0) 1 - (discountPercent / 100) else 1.0)
                                    invoiceViewModel.selectedInvoiceItem?.discount?.clear()
                                    if (discountPercent > 0) {
                                        invoiceViewModel.selectedInvoiceItem?.discount?.add(
                                            Discount(
                                                discountPercent,
                                                productPrice - (invoiceViewModel.selectedInvoiceItem?.price
                                                    ?: 0.0)
                                            )
                                        )
                                    }
                                    invoiceViewModel.selectedInvoiceItem?.updateTotal()
                                    invoiceViewModel.invoice.updateDiscount()
                                    invoiceViewModel.invoice.updateTotalCost()
                                    invoiceViewModel.updateTaxInfos()
                                }
                            }
                        }

                        Row {
                            Text(text = "Price : ")
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
                                    val productPrice =
                                        (invoiceViewModel.selectedInvoiceItem?.productPrice
                                            ?: 0.0)
                                    val discount = productPrice - price
                                    val discountPercent = if (discount > 0.0)
                                        ((discount * 100) / productPrice) else 0.0
                                    invoiceViewModel.selectedInvoiceItem?.discountPercent =
                                        discountPercent
                                    invoiceViewModel.selectedInvoiceItem?.discount?.clear()
                                    if (discountPercent > 0) {
                                        invoiceViewModel.selectedInvoiceItem?.discount?.add(
                                            Discount(
                                                discountPercent,
                                                productPrice - (invoiceViewModel.selectedInvoiceItem?.price
                                                    ?: 0.0)
                                            )
                                        )
                                    }
                                    invoiceViewModel.invoice.updateDiscount()
                                    invoiceViewModel.invoice.updateTotalCost()
                                    invoiceViewModel.updateTaxInfos()
                                }
                            }
                        }

                        Row {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = "Discount @ " + invoiceViewModel.selectedInvoiceItem?.discountPercent.toDecimal() + " % : "
                            )
                            LazyColumn {
                                items(
                                    invoiceViewModel.selectedInvoiceItem?.discount?.size ?: 0
                                ) { discountIndex ->
                                    Text(
                                        modifier = Modifier.padding(4.dp),
                                        text = invoiceViewModel.selectedInvoiceItem?.discount?.get(
                                            discountIndex
                                        )?.value.toDecimal()
                                    )
                                }
                            }
                        }
                    }
                },
                onDismissRequest = {
                    invoiceViewModel.selectedInvoiceItem = null
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            invoiceViewModel.selectedInvoiceItem = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

