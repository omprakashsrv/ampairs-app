package com.ampairs.invoice.ui

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.inv_view_cd_back
import ampairsapp.feature.invoice.generated.resources.inv_view_cd_edit
import ampairsapp.feature.invoice.generated.resources.inv_view_draft
import ampairsapp.feature.invoice.generated.resources.inv_view_print_cd
import ampairsapp.feature.invoice.generated.resources.inv_view_qty_caption
import ampairsapp.feature.invoice.generated.resources.inv_view_save
import ampairsapp.feature.invoice.generated.resources.inv_view_title
import ampairsapp.feature.invoice.generated.resources.inv_view_view_order
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.common.format.toDecimal
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.invoice.editor.DocSyncChip
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.ampairs.invoice.viewmodel.InvoiceViewViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Invoice view v2 (docs/design/order-list-and-view, mirrored) — the post-save landing.
 * The body IS the printable GST tax invoice ([TaxInvoiceView] paper, centered ≤760dp); the app
 * chrome adds the mono number, a live sync chip, Print/Edit, a status band and the linked-order
 * chip (invoices converted from an order navigate back to it).
 */
@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun InvoiceViewScreen(
    invoiceId: String,
    onNavigateBack: () -> Unit,
    onEdit: (invoiceId: String) -> Unit = {},
    onOpenOrder: (orderId: String) -> Unit = {},
    viewModel: InvoiceViewViewModel = assistedMetroViewModel<InvoiceViewViewModel, InvoiceViewViewModel.Factory>(key = invoiceId) { create(invoiceId) }
) {
    val invoice = viewModel.invoice
    val locale = LocalAppLocale.current
    val cs = MaterialTheme.colorScheme
    val mono = FontFamily.Monospace
    var showPreview by remember { mutableStateOf(false) }
    val fromOrder = !invoice.orderRefId.isNullOrBlank()

    if (showPreview) {
        TaxInvoicePreviewScreen(invoice = invoice, onClose = { showPreview = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.inv_view_title) + " · ",
                            maxLines = 1,
                        )
                        Text(
                            text = invoice.invoiceNumber?.ifBlank { null }
                                ?: stringResource(Res.string.inv_view_draft),
                            fontFamily = mono,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    ScreenBackButton(onClick = onNavigateBack, contentDescription = stringResource(Res.string.inv_view_cd_back))
                },
                actions = {
                    DocSyncChip(viewModel.syncUi, onRetry = viewModel::retrySync)
                    IconButton(onClick = { showPreview = true }) {
                        Icon(
                            imageVector = Icons.Filled.Print,
                            contentDescription = stringResource(Res.string.inv_view_print_cd)
                        )
                    }
                    IconButton(onClick = { onEdit(invoice.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(Res.string.inv_view_cd_edit)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = cs.surfaceContainer) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        formatMoney(invoice.totalCost, locale),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = mono,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (invoice.invoiceNumber.isNullOrEmpty()) {
                    Button(
                        onClick = { viewModel.saveInvoice() },
                        enabled = !viewModel.savingInvoice,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (viewModel.savingInvoice) {
                            CircularProgressIndicator(
                                modifier = Modifier.progressSemantics().size(18.dp),
                                strokeWidth = 2.dp,
                                color = cs.onPrimary
                            )
                        } else {
                            Text(stringResource(Res.string.inv_view_save))
                        }
                    }
                } else if (fromOrder) {
                    FilledTonalButton(
                        onClick = { invoice.orderRefId?.let(onOpenOrder) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  " + stringResource(Res.string.inv_view_view_order))
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
                        InvoiceStatusChip(invoice.status.name)
                        Text(
                            formatInvoiceInstantDate(invoice.invoiceDate),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = mono,
                            color = cs.onSurfaceVariant,
                        )
                        Text(
                            stringResource(Res.string.inv_view_qty_caption, invoice.totalItems, invoice.totalQuantity.toDecimal()),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (fromOrder) {
                            AssistChip(
                                onClick = { invoice.orderRefId?.let(onOpenOrder) },
                                leadingIcon = {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text(stringResource(Res.string.inv_view_view_order), maxLines = 1) },
                            )
                        }
                    }
                }
                // ── the printable GST invoice paper itself ──
                item {
                    TaxInvoiceView(invoice = invoice, workspaceName = "")
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatInvoiceInstantDate(instant: kotlin.time.Instant): String {
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "${date.day.toString().padStart(2, '0')} $month ${date.year}"
}
