package com.ampairs.order.ui

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
import com.ampairs.order.domain.Discount
import com.ampairs.order.viewmodel.OrderViewModel
import com.ampairs.product.productNavigation
import com.ampairs.product.ui.product.ProductViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    fromCustomerId: String?,
    toCustomerId: String?,
    id: String?,
    onOrderSaved: (String) -> Unit
) {
    val productViewModel: ProductViewModel = koinInject()
    val orderViewModel: OrderViewModel = koinInject { parametersOf(fromCustomerId, toCustomerId, id) }

    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberStandardBottomSheetState(initialValue = SheetValue.Hidden, skipHiddenState = false)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    productViewModel.onProductQtyChangeCallback = { products ->
        orderViewModel.updateOrderItems(products)
        scope.launch {
            bottomSheetState.hide()
        }
    }

    Scaffold(bottomBar = {
        val orderItems = orderViewModel.orderItems
        if (orderItems.size > 0) {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    orderViewModel.order.totalItems.toString() + " Items",
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
                    else Text(if (scaffoldState.bottomSheetState.isVisible) "Save" else "Next")
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
                        items(orderViewModel.orderItems.size) { index ->
                            val orderItem = orderViewModel.orderItems.get(index)
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
                                            "Discount : " + orderItem.discountPercent.toDecimal() + "%",
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
                                            orderViewModel.updateTaxInfos()
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

        if (orderViewModel.selectedOrderItem != null) {
            AlertDialog(
                title = {
                    Text(text = orderViewModel.selectedOrderItem?.product?.name ?: "")
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
                                    id = orderViewModel.selectedOrderItem?.product?.id + orderViewModel.selectedOrderItem?.product?.quantity,
                                    qty = orderViewModel.selectedOrderItem?.discountPercent ?: 0.0
                                ) { discountPercent ->
                                    orderViewModel.selectedOrderItem?.discountPercent =
                                        discountPercent
                                    val productPrice =
                                        orderViewModel.selectedOrderItem?.productPrice ?: 0.0
                                    orderViewModel.selectedOrderItem?.price =
                                        productPrice * (if (discountPercent > 0) 1 - (discountPercent / 100) else 1.0)
                                    orderViewModel.selectedOrderItem?.discount?.clear()
                                    if (discountPercent > 0) {
                                        orderViewModel.selectedOrderItem?.discount?.add(
                                            Discount(
                                                discountPercent,
                                                productPrice - (orderViewModel.selectedOrderItem?.price
                                                    ?: 0.0)
                                            )
                                        )
                                    }
                                    orderViewModel.selectedOrderItem?.updateTotal()
                                    orderViewModel.order.updateDiscount()
                                    orderViewModel.order.updateTotalCost()
                                    orderViewModel.updateTaxInfos()
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
                                    id = orderViewModel.selectedOrderItem?.product?.id + "SP" + orderViewModel.selectedOrderItem?.product?.quantity,
                                    qty = orderViewModel.selectedOrderItem?.price ?: 0.0
                                ) { price ->
                                    orderViewModel.selectedOrderItem?.price = price
                                    orderViewModel.selectedOrderItem?.updateTotal()
                                    val productPrice =
                                        (orderViewModel.selectedOrderItem?.productPrice
                                            ?: 0.0)
                                    val discount = productPrice - price
                                    val discountPercent = if (discount > 0.0)
                                        ((discount * 100) / productPrice) else 0.0
                                    orderViewModel.selectedOrderItem?.discountPercent =
                                        discountPercent
                                    orderViewModel.selectedOrderItem?.discount?.clear()
                                    if (discountPercent > 0) {
                                        orderViewModel.selectedOrderItem?.discount?.add(
                                            Discount(
                                                discountPercent,
                                                productPrice - (orderViewModel.selectedOrderItem?.price
                                                    ?: 0.0)
                                            )
                                        )
                                    }
                                    orderViewModel.order.updateDiscount()
                                    orderViewModel.order.updateTotalCost()
                                    orderViewModel.updateTaxInfos()
                                }
                            }
                        }

                        Row {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = "Discount @ " + orderViewModel.selectedOrderItem?.discountPercent.toDecimal() + " % : "
                            )
                            LazyColumn {
                                items(
                                    orderViewModel.selectedOrderItem?.discount?.size ?: 0
                                ) { discountIndex ->
                                    Text(
                                        modifier = Modifier.padding(4.dp),
                                        text = orderViewModel.selectedOrderItem?.discount?.get(
                                            discountIndex
                                        )?.value.toDecimal()
                                    )
                                }
                            }
                        }
                    }
                },
                onDismissRequest = {
                    orderViewModel.selectedOrderItem = null
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            orderViewModel.selectedOrderItem = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

