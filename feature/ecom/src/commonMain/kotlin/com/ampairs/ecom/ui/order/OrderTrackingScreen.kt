package com.ampairs.ecom.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_tracking_title
import ampairsapp.feature.ecom.generated.resources.ecom_delivery_address
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.formwidgets.location.DeliveryLocationMap
import com.ampairs.ecom.ui.components.OrderStatusChip
import com.ampairs.ecom.ui.components.orderStatusLabel
import com.ampairs.ecom.ui.components.trackingProgress
import com.ampairs.ecom.ui.components.trackingStages
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun OrderTrackingScreen(
    orderRef: String,
    onBack: () -> Unit,
    viewModel: OrderTrackingViewModel =
        assistedMetroViewModel<OrderTrackingViewModel, OrderTrackingViewModel.Factory>(key = orderRef) { create(orderRef) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    val order = state.order

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ScreenBackButton(onClick = onBack, contentDescription = "Back")
            Text(stringResource(Res.string.ecom_tracking_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        }

        if (order == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("…") }
            return
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(order.order_number.ifBlank { order.ecom_order_ref }, style = MaterialTheme.typography.titleMedium)
                        Text(order.placed_at.take(10), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OrderStatusChip(status = order.status, label = orderStatusLabel(order.status))
                }
            }

            item {
                val progress = trackingProgress(order.status)
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    trackingStages.forEachIndexed { index, stage ->
                        TimelineStep(
                            label = orderStatusLabel(stage.name),
                            done = index <= progress,
                            current = index == progress,
                            last = index == trackingStages.lastIndex,
                        )
                    }
                }
            }

            item { HorizontalDivider(Modifier.padding(bottom = 12.dp)) }

            items(state.lineItems, key = { it.uid }) { li ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${li.quantity_ordered} × ${li.product_name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(formatMoney(li.line_total, locale), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleSmall)
                    Text(formatMoney(order.total_amount, locale), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }

            state.deliveryAddress?.let { addr ->
                item {
                    Column(Modifier.fillMaxWidth()) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Text(
                            stringResource(Res.string.ecom_delivery_address),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        val addressLine = listOf(addr.addressLine1, addr.addressLine2.orEmpty(), addr.city, addr.state, addr.pinCode)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                        Text(addressLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        addr.phone?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val lat = addr.latitude
                        val lng = addr.longitude
                        if (lat != null && lng != null) {
                            Spacer(Modifier.height(10.dp))
                            DeliveryLocationMap(latitude = lat, longitude = lng, label = addressLine)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineStep(label: String, done: Boolean, current: Boolean, last: Boolean) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(24.dp).clip(CircleShape)
                    .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
            if (!last) {
                Box(
                    Modifier.width(2.dp).height(24.dp)
                        .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
