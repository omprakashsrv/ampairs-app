package com.ampairs.order.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.order.viewmodel.OrderViewViewModel
import com.ampairs.ui.components.TableCell
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderViewScreen(orderId: String, onNavigateBack: (orderId: String?) -> Unit) {

    val viewModel: OrderViewViewModel = koinInject { parametersOf(orderId) }

    val order = viewModel.order
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "Order Number : " + order.orderNumber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigateBack(order.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Localized description"
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    viewModel.order.totalItems.toString() + " Items",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                viewModel.order.discount?.sumOf { it.value }?.let {
                    Text(
                        "Discount : " + it.toDecimal(),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        viewModel.order.totalCost.toDecimal(),
                        modifier = Modifier.align(Alignment.End).padding(12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (order.orderNumber.isNullOrEmpty()) {
                    ElevatedButton(onClick = {
                        viewModel.saveOrder()
                    }) {
                        if (viewModel.savingOrder)
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .progressSemantics()
                                    .size(24.dp)
                            )
                        else Text("Save")
                    }
                }
                if (!order.orderNumber.isNullOrEmpty() && order.invoiceRefId.isNullOrEmpty()) {
                    ElevatedButton(onClick = {
                        viewModel.createInvoice()
                    }) {
                        if (viewModel.savingOrder)
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .progressSemantics()
                                    .size(24.dp)
                            )
                        else Text("Create Invoice")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxWidth()) {
            Text(
                text = "From :  " + order.fromCustomer?.name,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "To     :  " + order.toCustomer?.name,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.labelSmall
            )
            HorizontalDivider()
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                TableCell(
                    text = "Particulars", weight = 0.4f, title = true, alignment = TextAlign.Start
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                TableCell(
                    text = "Rate", weight = 0.22f, title = true
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                TableCell(
                    text = "Qty", weight = 0.18f, title = true
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                TableCell(
                    text = "Total", weight = 0.3f, title = true, alignment = TextAlign.End
                )
                VerticalDivider(modifier = Modifier.fillMaxHeight())
            }
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                order.items.size.let { orderItems ->
                    items(orderItems) {
                        val orderItem = order.items.get(it)
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            VerticalDivider(modifier = Modifier.fillMaxHeight())
                            Column(
                                modifier = Modifier
                                    .weight(0.4f)
                            ) {
                                Text(
                                    text = orderItem.description,
                                    modifier = Modifier
                                        .padding(10.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                if (orderItem.discountPercent > 0.0) {
                                    Text(
                                        "Discount : " + orderItem.discountPercent.toDecimal() + "%" + " : " + orderItem.discount.sumOf { it.value }
                                            .toDecimal(),
                                        Modifier
                                            .padding(10.dp, 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(9, 121, 105),
                                    )
                                }
                            }
                            VerticalDivider(modifier = Modifier.fillMaxHeight())
                            TableCell(
                                text = orderItem.price.toDecimal(),
                                weight = 0.22f,
                            )
                            VerticalDivider(modifier = Modifier.fillMaxHeight())
                            TableCell(
                                text = orderItem.quantity.toDecimal(),
                                weight = 0.18f,
                            )
                            VerticalDivider(modifier = Modifier.fillMaxHeight())
                            TableCell(
                                text = orderItem.totalCost.toDecimal(),
                                weight = 0.3f,
                                alignment = TextAlign.End,
                                title = true
                            )
                            VerticalDivider(modifier = Modifier.fillMaxHeight())
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}