package com.ampairs.order.ui

import ampairsapp.feature.order.generated.resources.Res
import ampairsapp.feature.order.generated.resources.ord_conv_cancel
import ampairsapp.feature.order.generated.resources.ord_conv_confirm
import ampairsapp.feature.order.generated.resources.ord_conv_line1
import ampairsapp.feature.order.generated.resources.ord_conv_line2
import ampairsapp.feature.order.generated.resources.ord_conv_line3
import ampairsapp.feature.order.generated.resources.ord_conv_line4
import ampairsapp.feature.order.generated.resources.ord_conv_note
import ampairsapp.feature.order.generated.resources.ord_conv_title
import ampairsapp.feature.order.generated.resources.ord_view_bill_to
import ampairsapp.feature.order.generated.resources.ord_view_cd_back
import ampairsapp.feature.order.generated.resources.ord_view_cd_edit
import ampairsapp.feature.order.generated.resources.ord_view_col_particulars
import ampairsapp.feature.order.generated.resources.ord_view_col_qty
import ampairsapp.feature.order.generated.resources.ord_view_col_rate
import ampairsapp.feature.order.generated.resources.ord_view_col_total
import ampairsapp.feature.order.generated.resources.ord_view_create_invoice
import ampairsapp.feature.order.generated.resources.ord_view_discount
import ampairsapp.feature.order.generated.resources.ord_view_draft
import ampairsapp.feature.order.generated.resources.ord_view_draft_hint
import ampairsapp.feature.order.generated.resources.ord_view_edit
import ampairsapp.feature.order.generated.resources.ord_view_inter_foot
import ampairsapp.feature.order.generated.resources.ord_view_intra_foot
import ampairsapp.feature.order.generated.resources.ord_view_line_meta
import ampairsapp.feature.order.generated.resources.ord_view_unregistered
import ampairsapp.feature.order.generated.resources.ord_view_from_seller
import ampairsapp.feature.order.generated.resources.ord_view_grand
import ampairsapp.feature.order.generated.resources.ord_view_gstin
import ampairsapp.feature.order.generated.resources.ord_view_qty_caption
import ampairsapp.feature.order.generated.resources.ord_view_save
import ampairsapp.feature.order.generated.resources.ord_view_taxable
import ampairsapp.feature.order.generated.resources.ord_view_title
import ampairsapp.feature.order.generated.resources.ord_view_view_invoice
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.common.format.toInr
import com.ampairs.invoice.editor.DocSyncChip
import com.ampairs.order.domain.TaxSpec
import com.ampairs.order.viewmodel.OrderViewViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * Order view v2 (docs/design/order-list-and-view) — the post-save landing. Reads like a document:
 * status band (+ linked-invoice chip), parties, calm line table, GST totals from the stored
 * taxInfos, mono grand total. Actions: Edit, idempotent Create invoice → View invoice.
 */
@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun OrderViewScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    onEdit: (orderId: String) -> Unit = {},
    onOpenInvoice: (invoiceId: String) -> Unit = {},
    viewModel: OrderViewViewModel = assistedMetroViewModel<OrderViewViewModel, OrderViewViewModel.Factory>(key = orderId) { create(orderId) }
) {
    val order = viewModel.order
    val cs = MaterialTheme.colorScheme
    val mono = FontFamily.Monospace
    var showConvertConfirm by remember { mutableStateOf(false) }
    val converted = !order.invoiceRefId.isNullOrEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.ord_view_title) + " · ",
                            maxLines = 1,
                        )
                        Text(
                            text = order.orderNumber?.ifBlank { null }
                                ?: stringResource(Res.string.ord_view_draft),
                            fontFamily = mono,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.ord_view_cd_back)
                        )
                    }
                },
                actions = {
                    DocSyncChip(viewModel.syncUi, onRetry = viewModel::retrySync)
                    IconButton(onClick = { onEdit(order.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(Res.string.ord_view_cd_edit)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = cs.surfaceContainer) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.totalCost.toInr(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = mono,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedButton(
                    onClick = { onEdit(order.id) },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  " + stringResource(Res.string.ord_view_edit))
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
                                color = cs.onPrimary
                            )
                        } else {
                            Text(stringResource(Res.string.ord_view_save))
                        }
                    }
                } else if (converted) {
                    FilledTonalButton(
                        onClick = { order.invoiceRefId?.let(onOpenInvoice) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  " + stringResource(Res.string.ord_view_view_invoice))
                    }
                } else {
                    Button(
                        onClick = { showConvertConfirm = true },
                        enabled = !viewModel.savingOrder,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (viewModel.savingOrder) {
                            CircularProgressIndicator(
                                modifier = Modifier.progressSemantics().size(18.dp),
                                strokeWidth = 2.dp,
                                color = cs.onPrimary
                            )
                        } else {
                            Text(stringResource(Res.string.ord_view_create_invoice))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                // ── status band ──
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        StatusChip(order.status.name, invoiced = converted)
                        if (!order.orderNumber.isNullOrBlank()) {
                            Text(
                                order.orderNumber ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = mono,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                stringResource(Res.string.ord_view_draft_hint),
                                style = MaterialTheme.typography.labelMedium,
                                fontStyle = FontStyle.Italic,
                                color = cs.onSurfaceVariant,
                            )
                        }
                        Text(
                            "· " + formatInstantDate(order.orderDate),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = mono,
                            color = cs.onSurfaceVariant,
                        )
                        Text(
                            stringResource(Res.string.ord_view_qty_caption, order.totalItems, order.totalQuantity.toDecimal()),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (converted) {
                            AssistChip(
                                onClick = { order.invoiceRefId?.let(onOpenInvoice) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = {
                                    Text(
                                        stringResource(Res.string.ord_view_view_invoice),
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                }

                // ── parties ──
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        PartyCard(
                            title = stringResource(Res.string.ord_view_from_seller),
                            name = order.fromCustomer?.name ?: "—",
                            gstin = order.fromCustomer?.gstNumber,
                            meta = order.fromCustomer?.state,
                            modifier = Modifier.weight(1f),
                        )
                        PartyCard(
                            title = stringResource(Res.string.ord_view_bill_to),
                            name = order.toCustomer?.name ?: "—",
                            gstin = order.toCustomer?.gstNumber,
                            meta = listOfNotNull(
                                order.toCustomer?.phone?.takeIf { it.isNotBlank() },
                                order.toCustomer?.state?.takeIf { it.isNotBlank() },
                            ).joinToString(" · ").ifBlank { null },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── line table ──
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        HeaderCell("#", 0.07f)
                        HeaderCell(stringResource(Res.string.ord_view_col_particulars), 0.43f, TextAlign.Start)
                        HeaderCell(stringResource(Res.string.ord_view_col_qty), 0.14f, TextAlign.End)
                        HeaderCell(stringResource(Res.string.ord_view_col_rate), 0.16f, TextAlign.End)
                        HeaderCell(stringResource(Res.string.ord_view_col_total), 0.20f, TextAlign.End)
                    }
                    HorizontalDivider(color = cs.outline)
                }
                items(order.items.size) { index ->
                    val item = order.items[index]
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            BodyCell("${index + 1}", 0.07f)
                            Column(modifier = Modifier.weight(0.43f)) {
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    stringResource(
                                        Res.string.ord_view_line_meta,
                                        item.quantity.toDecimal(),
                                        item.unitName.ifBlank { "—" },
                                        item.product?.taxCode?.ifBlank { "—" } ?: "—",
                                        "${item.taxInfos.sumOf { it.percentage }.toDecimal()}%",
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                if (item.discountPercent > 0.0 || item.discount.sumOf { it.value } > 0.0) {
                                    Text(
                                        "${stringResource(Res.string.ord_view_discount)} " +
                                            (if (item.discountPercent > 0.0) "−${item.discountPercent.toDecimal()}% · " else "−") +
                                            item.discount.sumOf { it.value }.toInr(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.secondary,
                                    )
                                }
                            }
                            BodyCell("${item.quantity.toDecimal()} ${item.unitName}".trim(), 0.14f, TextAlign.End)
                            BodyCell(item.price.toInr(), 0.16f, TextAlign.End, mono = true)
                            BodyCell(item.totalCost.toInr(), 0.20f, TextAlign.End, mono = true, bold = true)
                        }
                        HorizontalDivider(color = cs.outlineVariant)
                    }
                }

                // ── totals ──
                item {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    ) {
                        Column(modifier = Modifier.widthIn(min = 240.dp)) {
                            TotalsRow(stringResource(Res.string.ord_view_taxable), order.basePrice.toInr())
                            // per-rate component groups (orders.jsx vtotals): "CGST 2.5%  ₹195.75"
                            val groups = order.items.flatMap { it.taxInfos }
                                .groupBy { it.name to it.percentage }
                                .map { (k, v) -> Triple(k.first, k.second, v.sumOf { ti -> ti.value ?: 0.0 }) }
                                .sortedWith(compareBy({ taxNameOrder(it.first) }, { it.second }))
                            if (groups.isNotEmpty()) {
                                groups.forEach { (name, pct, amount) ->
                                    TotalsRow("$name ${pct.toDecimal()}%", amount.toInr())
                                }
                            } else {
                                order.taxInfos.orEmpty().forEach { ti ->
                                    TotalsRow(ti.name, (ti.value ?: 0.0).toInr())
                                }
                            }
                            order.discount?.sumOf { it.value }?.takeIf { it > 0 }?.let {
                                TotalsRow(stringResource(Res.string.ord_view_discount), "− ${it.toInr()}")
                            }
                            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = cs.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    stringResource(Res.string.ord_view_grand),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    order.totalCost.toInr(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontFamily = mono,
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(Res.string.ord_view_qty_caption, order.totalItems, order.totalQuantity.toDecimal()) +
                            " · " + stringResource(
                                if (order.taxSpec == TaxSpec.INTRA) Res.string.ord_view_intra_foot
                                else Res.string.ord_view_inter_foot
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showConvertConfirm) {
        AlertDialog(
            onDismissRequest = { showConvertConfirm = false },
            title = { Text(stringResource(Res.string.ord_conv_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        stringResource(Res.string.ord_conv_line1),
                        stringResource(Res.string.ord_conv_line2),
                        stringResource(Res.string.ord_conv_line3),
                        stringResource(Res.string.ord_conv_line4),
                    ).forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("  $line", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(
                        stringResource(Res.string.ord_conv_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showConvertConfirm = false
                    viewModel.createInvoice()
                }) { Text(stringResource(Res.string.ord_conv_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showConvertConfirm = false }) {
                    Text(stringResource(Res.string.ord_conv_cancel))
                }
            }
        )
    }
}

@Composable
private fun PartyCard(title: String, name: String, gstin: String?, meta: String? = null, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (!gstin.isNullOrBlank()) stringResource(Res.string.ord_view_gstin, gstin)
                else stringResource(Res.string.ord_view_unregistered),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (!meta.isNullOrBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: TextAlign = TextAlign.Center,
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BodyCell(
    text: String,
    weight: Float,
    align: TextAlign = TextAlign.Center,
    mono: Boolean = false,
    bold: Boolean = false,
) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = if (mono) FontFamily.Monospace else null,
        fontWeight = if (bold) FontWeight.SemiBold else null,
        textAlign = align,
        maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TotalsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatInstantDate(instant: kotlin.time.Instant): String {
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "${date.day.toString().padStart(2, '0')} $month ${date.year}"
}

private fun taxNameOrder(name: String): Int = when (name) {
    "CGST" -> 0; "SGST" -> 1; "IGST" -> 2; else -> 3
}
