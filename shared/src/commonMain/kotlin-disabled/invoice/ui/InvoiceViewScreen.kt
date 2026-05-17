package com.ampairs.invoice.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.invoice.viewmodel.InvoiceViewViewModel
import com.ampairs.ui.components.TableCell
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceViewScreen(invoiceId: String, onNavigateBack: (invoiceId: String?) -> Unit) {

    val viewModel: InvoiceViewViewModel = koinInject { parametersOf(invoiceId) }

    val invoice = viewModel.invoice
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        "Invoice Number : " + invoice.invoiceNumber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigateBack(invoice.id) }) {
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
                    viewModel.invoice.totalItems.toString() + " Items",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                viewModel.invoice.discount?.sumOf { it.value }?.let {
                    Text(
                        "Discount : " + it.toDecimal(),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        viewModel.invoice.totalCost.toDecimal(),
                        modifier = Modifier.align(Alignment.End).padding(12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (invoice.invoiceNumber.isNullOrEmpty()) {
                    ElevatedButton(onClick = {
                        viewModel.saveInvoice()
                    }) {
                        if (viewModel.savingInvoice)
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .progressSemantics()
                                    .size(24.dp)
                            )
                        else Text("Save")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxWidth()) {
            Text(
                text = "From :  " + invoice.fromCustomer?.name,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "To     :  " + invoice.toCustomer?.name,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.labelSmall
            )
            HorizontalDivider()
            Row {
                TableCell(
                    text = "Particulars", weight = 0.4f, title = true, alignment = TextAlign.Start
                )
                TableCell(
                    text = "Rate", weight = 0.22f, title = true
                )
                TableCell(
                    text = "Qty", weight = 0.18f, title = true
                )
                TableCell(
                    text = "Total", weight = 0.3f, title = true, alignment = TextAlign.End
                )
            }
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                invoice.items.size.let { invoiceItems ->
                    items(invoiceItems) {
                        val invoiceItem = invoice.items.get(it)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TableCell(
                                text = invoiceItem.description,
                                weight = 0.4f,
                                alignment = TextAlign.Start
                            )
                            TableCell(
                                text = invoiceItem.price.toDecimal(),
                                weight = 0.22f,
                            )
                            TableCell(
                                text = invoiceItem.quantity.toDecimal(),
                                weight = 0.18f,
                            )
                            TableCell(
                                text = invoiceItem.totalCost.toDecimal(),
                                weight = 0.3f,
                                alignment = TextAlign.End,
                                title = true
                            )
                        }
                        if (invoiceItem.discountPercent > 0.0) {
                            Text(
                                "Discount : " + invoiceItem.discountPercent.toDecimal() + "%" + " : " + invoiceItem.discount.sumOf { it.value }
                                    .toDecimal(),
                                Modifier
                                    .padding(10.dp, 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(9, 121, 105),
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}