package com.ampairs.ecom.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_no_orders
import ampairsapp.feature.ecom.generated.resources.ecom_orders_title
import com.ampairs.ecom.domain.asRupee
import com.ampairs.ecom.ui.components.EcomDimens
import com.ampairs.ecom.ui.components.OrderStatusChip
import com.ampairs.ecom.ui.components.orderStatusLabel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun OrdersListScreen(
    onOpenOrder: (String) -> Unit,
    viewModel: OrdersViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(Res.string.ecom_orders_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(16.dp),
        )
        if (state.orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.ecom_no_orders), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                items(state.orders, key = { it.uid }) { order ->
                    OrderCard(
                        ref = order.ecom_order_ref,
                        date = order.placed_at,
                        status = order.status,
                        total = order.total_amount,
                        onClick = { onOpenOrder(order.ecom_order_ref) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(ref: String, date: String, status: String, total: Double, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, EcomDimens.cornerMd)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(ref, style = MaterialTheme.typography.titleSmall)
                Text(date.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OrderStatusChip(status = status, label = orderStatusLabel(status))
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(total.asRupee(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
