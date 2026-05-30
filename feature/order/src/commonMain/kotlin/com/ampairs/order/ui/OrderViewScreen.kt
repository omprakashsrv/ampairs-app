package com.ampairs.order.ui

import ampairsapp.feature.order.generated.resources.Res
import ampairsapp.feature.order.generated.resources.ord_view_cd_back
import ampairsapp.feature.order.generated.resources.ord_view_cd_edit
import ampairsapp.feature.order.generated.resources.ord_view_col_particulars
import ampairsapp.feature.order.generated.resources.ord_view_col_qty
import ampairsapp.feature.order.generated.resources.ord_view_col_rate
import ampairsapp.feature.order.generated.resources.ord_view_col_total
import ampairsapp.feature.order.generated.resources.ord_view_create_invoice
import ampairsapp.feature.order.generated.resources.ord_view_discount
import ampairsapp.feature.order.generated.resources.ord_view_from
import ampairsapp.feature.order.generated.resources.ord_view_items
import ampairsapp.feature.order.generated.resources.ord_view_save
import ampairsapp.feature.order.generated.resources.ord_view_to
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.order.viewmodel.OrderViewViewModel
import com.ampairs.ui.components.TableCell
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderViewScreen(
    orderId: String,
    onNavigateBack: (orderId: String?) -> Unit,
    viewModel: OrderViewViewModel = assistedMetroViewModel<OrderViewViewModel, OrderViewViewModel.Factory> { create(orderId) }
) {
    val order = viewModel.order

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (!order.orderNumber.isNullOrEmpty()) "Order ${order.orderNumber}" else "Order",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.ord_view_cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateBack(order.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(Res.string.ord_view_cd_edit)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Text(
                    stringResource(Res.string.ord_view_items, order.totalItems),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                order.discount?.sumOf { it.value }?.let { discountTotal ->
                    if (discountTotal > 0) {
                        Text(
                            "${stringResource(Res.string.ord_view_discount)}: ${discountTotal.toDecimal()}",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "₹${order.totalCost.toDecimal()}",
                        modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (order.orderNumber.isNullOrEmpty()) {
                    Button(
                        onClick = { viewModel.saveOrder() },
                        enabled = !viewModel.savingOrder,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (viewModel.savingOrder) {
                            CircularProgressIndicator(
                                modifier = Modifier.progressSemantics().size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(Res.string.ord_view_save))
                        }
                    }
                }
                if (!order.orderNumber.isNullOrEmpty() && order.invoiceRefId.isNullOrEmpty()) {
                    Button(
                        onClick = { viewModel.createInvoice() },
                        enabled = !viewModel.savingOrder,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (viewModel.savingOrder) {
                            CircularProgressIndicator(
                                modifier = Modifier.progressSemantics().size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(Res.string.ord_view_create_invoice))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.ord_view_from),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = order.fromCustomer?.name ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = stringResource(Res.string.ord_view_to),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = order.toCustomer?.name ?: "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        TableCell(
                            text = stringResource(Res.string.ord_view_col_particulars),
                            weight = 0.4f,
                            title = true,
                            alignment = TextAlign.Start
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        TableCell(text = stringResource(Res.string.ord_view_col_rate), weight = 0.22f, title = true)
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        TableCell(text = stringResource(Res.string.ord_view_col_qty), weight = 0.18f, title = true)
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        TableCell(
                            text = stringResource(Res.string.ord_view_col_total),
                            weight = 0.3f,
                            title = true,
                            alignment = TextAlign.End
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                    }
                }
                HorizontalDivider()
            }

            items(order.items.size) { index ->
                val orderItem = order.items[index]
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        Column(modifier = Modifier.weight(0.4f)) {
                            Text(
                                text = orderItem.description,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (orderItem.discountPercent > 0.0) {
                                Text(
                                    text = "Discount: ${orderItem.discountPercent.toDecimal()}% · ${orderItem.discount.sumOf { it.value }.toDecimal()}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(9, 121, 105)
                                )
                            }
                        }
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        TableCell(text = orderItem.price.toDecimal(), weight = 0.22f)
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        TableCell(text = orderItem.quantity.toDecimal(), weight = 0.18f)
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

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
