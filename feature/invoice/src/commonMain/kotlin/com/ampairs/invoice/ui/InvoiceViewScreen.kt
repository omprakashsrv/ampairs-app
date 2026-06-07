package com.ampairs.invoice.ui

import ampairsapp.feature.invoice.generated.resources.Res
import ampairsapp.feature.invoice.generated.resources.inv_view_cd_back
import ampairsapp.feature.invoice.generated.resources.inv_view_title
import ampairsapp.feature.invoice.generated.resources.inv_view_cd_edit
import ampairsapp.feature.invoice.generated.resources.inv_view_col_particulars
import ampairsapp.feature.invoice.generated.resources.inv_view_col_qty
import ampairsapp.feature.invoice.generated.resources.inv_view_col_rate
import ampairsapp.feature.invoice.generated.resources.inv_view_col_total
import ampairsapp.feature.invoice.generated.resources.inv_view_discount
import ampairsapp.feature.invoice.generated.resources.inv_view_from
import ampairsapp.feature.invoice.generated.resources.inv_view_items
import ampairsapp.feature.invoice.generated.resources.inv_view_save
import ampairsapp.feature.invoice.generated.resources.inv_view_to
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.invoice.viewmodel.InvoiceViewViewModel
import com.ampairs.ui.components.TableCell
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceViewScreen(
    invoiceId: String,
    onNavigateBack: (invoiceId: String?) -> Unit,
    viewModel: InvoiceViewViewModel = assistedMetroViewModel<InvoiceViewViewModel, InvoiceViewViewModel.Factory> { create(invoiceId) }
) {
    val invoice = viewModel.invoice
    var showPreview by remember { mutableStateOf(false) }

    if (showPreview) {
        TaxInvoicePreviewScreen(invoice = invoice, onClose = { showPreview = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = stringResource(Res.string.inv_view_title)
                    Text(
                        text = if (!invoice.invoiceNumber.isNullOrEmpty()) "$title ${invoice.invoiceNumber}" else title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(null) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.inv_view_cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPreview = true }) {
                        Icon(
                            imageVector = Icons.Filled.Print,
                            contentDescription = "Print"
                        )
                    }
                    IconButton(onClick = { onNavigateBack(invoice.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(Res.string.inv_view_cd_edit)
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
                    stringResource(Res.string.inv_view_items, invoice.totalItems),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                invoice.discount?.sumOf { it.value }?.let { discountTotal ->
                    if (discountTotal > 0) {
                        Text(
                            "${stringResource(Res.string.inv_view_discount)}: ${discountTotal.toDecimal()}",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "₹${invoice.totalCost.toDecimal()}",
                        modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(Res.string.inv_view_save))
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
                                    text = stringResource(Res.string.inv_view_from),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = invoice.fromCustomer?.name ?: "—",
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
                                    text = stringResource(Res.string.inv_view_to),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = invoice.toCustomer?.name ?: "—",
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
                            .padding(vertical = 8.dp)
                    ) {
                        TableCell(
                            text = stringResource(Res.string.inv_view_col_particulars),
                            weight = 0.4f,
                            title = true,
                            alignment = TextAlign.Start
                        )
                        TableCell(text = stringResource(Res.string.inv_view_col_rate), weight = 0.22f, title = true)
                        TableCell(text = stringResource(Res.string.inv_view_col_qty), weight = 0.18f, title = true)
                        TableCell(
                            text = stringResource(Res.string.inv_view_col_total),
                            weight = 0.3f,
                            title = true,
                            alignment = TextAlign.End
                        )
                    }
                }
                HorizontalDivider()
            }

            items(invoice.items.size) { index ->
                val invoiceItem = invoice.items[index]
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TableCell(
                            text = invoiceItem.description,
                            weight = 0.4f,
                            alignment = TextAlign.Start
                        )
                        TableCell(text = invoiceItem.price.toDecimal(), weight = 0.22f)
                        TableCell(text = invoiceItem.quantity.toDecimal(), weight = 0.18f)
                        TableCell(
                            text = invoiceItem.totalCost.toDecimal(),
                            weight = 0.3f,
                            alignment = TextAlign.End,
                            title = true
                        )
                    }
                    if (invoiceItem.discountPercent > 0.0) {
                        Text(
                            text = "${stringResource(Res.string.inv_view_discount)}: ${invoiceItem.discountPercent.toDecimal()}% · ${invoiceItem.discount.sumOf { it.value }.toDecimal()}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(9, 121, 105)
                        )
                    }
                    HorizontalDivider()
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
