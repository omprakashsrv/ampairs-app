package com.ampairs.invoice.ui

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.inv_list_cd_search_clear
import ampairsapp.feature.invoice.generated.resources.inv_list_clear_filters
import ampairsapp.feature.invoice.generated.resources.inv_list_count
import ampairsapp.feature.invoice.generated.resources.inv_list_empty_cta
import ampairsapp.feature.invoice.generated.resources.inv_list_empty_desc
import ampairsapp.feature.invoice.generated.resources.inv_list_empty_title
import ampairsapp.feature.invoice.generated.resources.inv_list_filter_all
import ampairsapp.feature.invoice.generated.resources.inv_list_filter_draft
import ampairsapp.feature.invoice.generated.resources.inv_list_filter_from_order
import ampairsapp.feature.invoice.generated.resources.inv_list_filter_invoiced
import ampairsapp.feature.invoice.generated.resources.inv_list_filter_new
import ampairsapp.feature.invoice.generated.resources.inv_list_filter_offline
import ampairsapp.feature.invoice.generated.resources.inv_list_from_order_hint
import ampairsapp.feature.invoice.generated.resources.inv_list_items
import ampairsapp.feature.invoice.generated.resources.inv_list_new
import ampairsapp.feature.invoice.generated.resources.inv_list_no_match_desc
import ampairsapp.feature.invoice.generated.resources.inv_list_no_match_title
import ampairsapp.feature.invoice.generated.resources.inv_list_offline_cd
import ampairsapp.feature.invoice.generated.resources.inv_list_retry
import ampairsapp.feature.invoice.generated.resources.inv_list_search_hint
import ampairsapp.feature.invoice.generated.resources.inv_list_title
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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
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
import com.ampairs.invoice.db.dto.Invoice
import com.ampairs.invoice.viewmodel.InvoiceListFilter
import com.ampairs.invoice.viewmodel.InvoicesViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Invoice list v2 (docs/design/order-list-and-view, mirrored): pinned search + single-select
 * filter chips (status / from-order / offline), two-line rows with mono number, status chip,
 * mono ₹ amount and an offline glyph; sync progress/error strips; CTA empty states.
 */
@Composable
fun InvoicesScreen(
    onInvoiceSelected: (String) -> Unit,
    onCreateInvoice: () -> Unit = {},
    selectedInvoiceId: String? = null,
    expanded: Boolean = false,
    viewModel: InvoicesViewModel = metroViewModel()
) {
    val lazyListState = rememberLazyListState()
    val invoices = viewModel.invoices.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    val syncState = viewModel.invoicesState.value

    fun refreshList() {
        invoices.refresh()
        scope.launch { lazyListState.animateScrollToItem(0) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (!expanded) {
                ExtendedFloatingActionButton(
                    onClick = onCreateInvoice,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.inv_list_new)) }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.inv_list_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (invoices.itemCount > 0) {
                                Text(
                                    text = stringResource(Res.string.inv_list_count, invoices.itemCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (expanded) {
                            FilledTonalButton(onClick = onCreateInvoice) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  " + stringResource(Res.string.inv_list_new))
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
                                stringResource(Res.string.inv_list_search_hint),
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
                                        contentDescription = stringResource(Res.string.inv_list_cd_search_clear),
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
                        InvoiceListFilter.entries.forEach { f ->
                            val selected = viewModel.filter == f
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    viewModel.filter = if (selected) InvoiceListFilter.ALL else f
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
                            Text(stringResource(Res.string.inv_list_retry))
                        }
                    }
                }
            }

            if (invoices.itemCount > 0) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        count = invoices.itemCount,
                        key = { index -> invoices[index]?.id ?: index },
                        contentType = { 1 }
                    ) { index ->
                        val invoice = invoices[index]
                        if (invoice != null) {
                            InvoiceRow(
                                invoice = invoice,
                                selected = invoice.id == selectedInvoiceId,
                                onClick = { onInvoiceSelected(invoice.id) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                val filtered = viewModel.searchText.isNotBlank() || viewModel.filter != InvoiceListFilter.ALL
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (filtered) Icons.Filled.Search else Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(if (filtered) Res.string.inv_list_no_match_title else Res.string.inv_list_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(if (filtered) Res.string.inv_list_no_match_desc else Res.string.inv_list_empty_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (filtered) {
                            TextButton(onClick = {
                                viewModel.searchText = ""
                                viewModel.filter = InvoiceListFilter.ALL
                                refreshList()
                            }) { Text(stringResource(Res.string.inv_list_clear_filters)) }
                        } else {
                            FilledTonalButton(onClick = onCreateInvoice) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("  " + stringResource(Res.string.inv_list_empty_cta))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun InvoiceListFilter.labelRes(): StringResource = when (this) {
    InvoiceListFilter.ALL -> Res.string.inv_list_filter_all
    InvoiceListFilter.DRAFT -> Res.string.inv_list_filter_draft
    InvoiceListFilter.NEW -> Res.string.inv_list_filter_new
    InvoiceListFilter.INVOICED -> Res.string.inv_list_filter_invoiced
    InvoiceListFilter.FROM_ORDER -> Res.string.inv_list_filter_from_order
    InvoiceListFilter.OFFLINE -> Res.string.inv_list_filter_offline
}

/** Two-line list row (design row anatomy): number · buyer · status · ₹ amount / date · items · hints. */
@Composable
private fun InvoiceRow(
    invoice: Invoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val offlineCd = stringResource(Res.string.inv_list_offline_cd)
    val buyer = invoice.customerName.ifBlank { "—" }
    val number = invoice.invoiceNumber.ifBlank { "—" }
    val amount = invoice.totalCost.toInr()
    val rowCd = "$number, $buyer, $amount, ${invoice.status.lowercase()}" +
        if (!invoice.synced) ", $offlineCd" else ""

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
                modifier = Modifier.size(36.dp).background(cs.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null,
                    tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp)
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
                    InvoiceStatusChip(invoice.status)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = listOfNotNull(
                            formatInvoiceListDate(invoice.invoiceDate),
                            stringResource(Res.string.inv_list_items, invoice.totalItems.toInt()),
                            if (!invoice.orderRefId.isNullOrBlank()) stringResource(Res.string.inv_list_from_order_hint) else null,
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
                if (!invoice.synced) {
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

/** Status chip with the design's container mapping (Draft/New/everything else). */
@Composable
internal fun InvoiceStatusChip(status: String) {
    val cs = MaterialTheme.colorScheme
    val (container, content) = when (status.uppercase()) {
        "DRAFT" -> cs.secondaryContainer to cs.onSecondaryContainer
        "NEW" -> cs.tertiaryContainer to cs.onTertiaryContainer
        else -> cs.primaryContainer to cs.onPrimaryContainer
    }
    Surface(color = container, shape = RoundedCornerShape(50)) {
        Text(
            text = status.lowercase().replaceFirstChar { it.uppercaseChar() },
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

/** `2026-06-11T13:30:00` → `11 Jun 2026` (falls back to the raw string on parse failure). */
internal fun formatInvoiceListDate(iso: String): String = try {
    val date = LocalDateTime.parse(iso.take(19)).date
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    "${date.day.toString().padStart(2, '0')} $month ${date.year}"
} catch (_: Exception) {
    iso.take(10)
}
