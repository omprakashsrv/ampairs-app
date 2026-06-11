package com.ampairs.order.ui

import ampairsapp.feature.order.generated.resources.Res
import ampairsapp.feature.order.generated.resources.ord_list_cd_search_clear
import ampairsapp.feature.order.generated.resources.ord_list_clear_filters
import ampairsapp.feature.order.generated.resources.ord_list_count
import ampairsapp.feature.order.generated.resources.ord_list_empty_cta
import ampairsapp.feature.order.generated.resources.ord_list_empty_desc
import ampairsapp.feature.order.generated.resources.ord_list_empty_title
import ampairsapp.feature.order.generated.resources.ord_list_filter_all
import ampairsapp.feature.order.generated.resources.ord_list_filter_draft
import ampairsapp.feature.order.generated.resources.ord_list_filter_invoiced
import ampairsapp.feature.order.generated.resources.ord_list_filter_new
import ampairsapp.feature.order.generated.resources.ord_list_filter_offline
import ampairsapp.feature.order.generated.resources.ord_list_filter_ordered
import ampairsapp.feature.order.generated.resources.ord_list_invoiced_hint
import ampairsapp.feature.order.generated.resources.ord_list_items
import ampairsapp.feature.order.generated.resources.ord_list_new
import ampairsapp.feature.order.generated.resources.ord_list_no_match_desc
import ampairsapp.feature.order.generated.resources.ord_list_no_match_title
import ampairsapp.feature.order.generated.resources.ord_list_offline_cd
import ampairsapp.feature.order.generated.resources.ord_list_retry
import ampairsapp.feature.order.generated.resources.ord_list_search_hint
import ampairsapp.feature.order.generated.resources.ord_list_title
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.ampairs.common.format.toInr
import com.ampairs.common.model.UiState
import com.ampairs.order.db.dto.Order
import com.ampairs.order.viewmodel.OrderListFilter
import com.ampairs.order.viewmodel.OrdersViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Order list v2 (docs/design/order-list-and-view): pinned search + single-select filter chips
 * (status / invoiced / offline), two-line rows with mono number, status chip, mono ₹ amount and
 * an offline glyph; sync progress/error strips under the header; CTA empty states.
 */
@Composable
fun OrdersScreen(
    onOrderSelected: (String) -> Unit,
    onCreateOrder: () -> Unit = {},
    selectedOrderId: String? = null,
    expanded: Boolean = false,
    viewModel: OrdersViewModel = metroViewModel()
) {
    val lazyListState = rememberLazyListState()
    val orders = viewModel.orders.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    val syncState = viewModel.ordersState.value

    fun refreshList() {
        orders.refresh()
        scope.launch { lazyListState.animateScrollToItem(0) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (!expanded) {
                ExtendedFloatingActionButton(
                    onClick = onCreateOrder,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.ord_list_new)) }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // ── pinned header: title + search + filter chips ──
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.ord_list_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (orders.itemCount > 0) {
                                Text(
                                    text = stringResource(Res.string.ord_list_count, orders.itemCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (expanded) {
                            FilledTonalButton(onClick = onCreateOrder) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  " + stringResource(Res.string.ord_list_new))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.searchText,
                        onValueChange = {
                            viewModel.searchText = it
                            refreshList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                stringResource(Res.string.ord_list_search_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (viewModel.searchText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchText = ""; refreshList() }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(Res.string.ord_list_cd_search_clear),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrderListFilter.entries.forEach { f ->
                            val selected = viewModel.filter == f
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    viewModel.filter = if (selected) OrderListFilter.ALL else f
                                    refreshList()
                                },
                                label = { Text(stringResource(f.labelRes())) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                            )
                        }
                    }
                }
            }
            HorizontalDivider()

            // ── sync strips: progress while pulling, inline error with retry ──
            if (syncState is UiState.Loading<*>) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (syncState is UiState.Error) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            syncState.msg ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::retrySync) {
                            Text(stringResource(Res.string.ord_list_retry))
                        }
                    }
                }
            }

            if (orders.itemCount > 0) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        count = orders.itemCount,
                        key = { index -> orders[index]?.id ?: index },
                        contentType = { 1 }
                    ) { index ->
                        val order = orders[index]
                        if (order != null) {
                            OrderRow(
                                order = order,
                                selected = order.id == selectedOrderId,
                                onClick = { onOrderSelected(order.id) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                val filtered = viewModel.searchText.isNotBlank() || viewModel.filter != OrderListFilter.ALL
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (filtered) Icons.Filled.Search else Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(if (filtered) Res.string.ord_list_no_match_title else Res.string.ord_list_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(if (filtered) Res.string.ord_list_no_match_desc else Res.string.ord_list_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (filtered) {
                            TextButton(onClick = {
                                viewModel.searchText = ""
                                viewModel.filter = OrderListFilter.ALL
                                refreshList()
                            }) { Text(stringResource(Res.string.ord_list_clear_filters)) }
                        } else {
                            FilledTonalButton(onClick = onCreateOrder) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  " + stringResource(Res.string.ord_list_empty_cta))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun OrderListFilter.labelRes(): StringResource = when (this) {
    OrderListFilter.ALL -> Res.string.ord_list_filter_all
    OrderListFilter.DRAFT -> Res.string.ord_list_filter_draft
    OrderListFilter.NEW -> Res.string.ord_list_filter_new
    OrderListFilter.ORDERED -> Res.string.ord_list_filter_ordered
    OrderListFilter.INVOICED -> Res.string.ord_list_filter_invoiced
    OrderListFilter.OFFLINE -> Res.string.ord_list_filter_offline
}

/** Two-line list row (design row anatomy): number · buyer · status · ₹ amount / date · items · hints. */
@Composable
private fun OrderRow(
    order: Order,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val offlineCd = stringResource(Res.string.ord_list_offline_cd)
    val buyer = order.toCustomerName.ifBlank { order.fromCustomerName }.ifBlank { "—" }
    val number = order.orderNumber.ifBlank { "—" }
    val amount = order.totalCost.toInr()
    val rowCd = "$number, $buyer, $amount, ${order.status.lowercase()}" +
        if (!order.synced) ", $offlineCd" else ""

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) cs.secondaryContainer.copy(alpha = 0.4f) else cs.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .heightIn(min = 56.dp)
                .semantics { contentDescription = rowCd },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(cs.tertiaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ShoppingCart, contentDescription = null,
                    tint = cs.onTertiaryContainer, modifier = Modifier.size(18.dp)
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "  $buyer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                    )
                    StatusChip(order.status, invoiced = !order.invoiceRefId.isNullOrBlank())
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = listOfNotNull(
                            formatOrderDate(order.orderDate),
                            stringResource(Res.string.ord_list_items, order.totalItems.toInt()),
                            if (!order.invoiceRefId.isNullOrBlank()) stringResource(Res.string.ord_list_invoiced_hint) else null,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                if (!order.synced) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = offlineCd,
                        tint = cs.secondary,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = cs.outlineVariant)
    }
}

/**
 * Status chip with the design's container mapping (orders.jsx STATUS_META):
 * Draft = secondary, New = tertiary, Ordered = primary; a converted order shows
 * "Invoiced" (tertiary) in place of its status.
 */
@Composable
internal fun StatusChip(status: String, invoiced: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    val (container, content) = when {
        invoiced -> cs.tertiaryContainer to cs.onTertiaryContainer
        status.uppercase() == "DRAFT" -> cs.secondaryContainer to cs.onSecondaryContainer
        status.uppercase() == "NEW" -> cs.tertiaryContainer to cs.onTertiaryContainer
        else -> cs.primaryContainer to cs.onPrimaryContainer
    }
    Surface(color = container, shape = RoundedCornerShape(50)) {
        Text(
            text = if (invoiced) stringResource(Res.string.ord_list_filter_invoiced)
            else status.lowercase().replaceFirstChar { it.uppercaseChar() },
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

/** `2026-06-11T13:30:00` → `11 Jun 2026` (falls back to the raw string on parse failure). */
internal fun formatOrderDate(iso: String): String = try {
    val date = LocalDateTime.parse(iso.take(19)).date
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    "${date.day.toString().padStart(2, '0')} $month ${date.year}"
} catch (_: Exception) {
    iso.take(10)
}
