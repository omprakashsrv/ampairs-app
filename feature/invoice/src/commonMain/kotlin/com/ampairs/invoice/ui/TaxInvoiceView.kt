package com.ampairs.invoice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampairs.common.format.toDecimal
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.print.amountInWords
import com.ampairs.invoice.print.buildInvoiceHtml
import com.ampairs.invoice.print.isInterState
import com.ampairs.invoice.print.rememberInvoicePrinter

/**
 * Full-screen, read-only GST tax-invoice "paper" preview with a Print action (spec 010, screen 10).
 * Print hands a self-contained HTML render to the platform print pipeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxInvoicePreviewScreen(
    invoice: Invoice,
    onClose: () -> Unit,
    workspaceName: String = "",
) {
    val print = rememberInvoicePrinter()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax invoice") },
                navigationIcon = {
                    ScreenBackButton(onClick = onClose, contentDescription = "Close")
                },
                actions = {
                    IconButton(onClick = { print(buildInvoiceHtml(invoice, workspaceName)) }) {
                        Icon(Icons.Filled.Print, contentDescription = "Print")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            TaxInvoiceView(invoice = invoice, workspaceName = workspaceName)
        }
    }
}

@Composable
fun TaxInvoiceView(
    invoice: Invoice,
    workspaceName: String,
    modifier: Modifier = Modifier,
) {
    // Seller is the implicit current workspace (name passed in); only the buyer is stored.
    val buyer = invoice.customer
    val inter = invoice.isInterState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "TAX INVOICE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Text(
                workspaceName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PartyBlock(
                    title = "Seller",
                    // Seller identity is snapshotted on the invoice at issue time (self-contained).
                    name = invoice.sellerName?.takeIf { it.isNotBlank() } ?: workspaceName,
                    address = invoice.sellerAddress.orEmpty(),
                    gst = invoice.sellerGst,
                    modifier = Modifier.weight(1f),
                )
                PartyBlock(
                    title = "Bill To",
                    name = buyer?.name ?: "—",
                    address = listOfNotNull(buyer?.address, buyer?.city, buyer?.state, buyer?.pincode)
                        .filter { it.isNotBlank() }.joinToString(", "),
                    gst = buyer?.gstNumber,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("No: ${invoice.invoiceNumber ?: "—"}", style = MaterialTheme.typography.labelMedium)
                Text(DateTimeAdapter.toDateTimeString(invoice.invoiceDate), style = MaterialTheme.typography.labelMedium)
                Text(
                    if (inter) "IGST" else "CGST+SGST",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            // Header row — split tax columns per scenario (spec 010 v2, invoice screen)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Cell("#", 0.06f)
                Cell("Item / HSN", 0.28f, align = TextAlign.Start)
                Cell("Qty", 0.12f, align = TextAlign.End)
                Cell("Taxable", 0.14f, align = TextAlign.End)
                if (inter) {
                    Cell("IGST", 0.20f, align = TextAlign.End)
                } else {
                    Cell("CGST", 0.10f, align = TextAlign.End)
                    Cell("SGST", 0.10f, align = TextAlign.End)
                }
                Cell("Amount", 0.20f, align = TextAlign.End)
            }
            HorizontalDivider()
            invoice.items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Cell("${index + 1}", 0.06f)
                    Cell(
                        item.description + (item.variantSku?.let { " ($it)" } ?: "") +
                            (item.product?.taxCode?.takeIf { it.isNotBlank() }?.let { "  $it" } ?: ""),
                        0.28f, align = TextAlign.Start
                    )
                    Cell("${item.quantity.toDecimal()} ${item.unitName}".trim(), 0.12f, align = TextAlign.End)
                    Cell(item.basePrice.toDecimal(), 0.14f, align = TextAlign.End)
                    if (inter) {
                        Cell(item.totalTax.toDecimal(), 0.20f, align = TextAlign.End)
                    } else {
                        val half = item.totalTax / 2.0
                        Cell(half.toDecimal(), 0.10f, align = TextAlign.End)
                        Cell((item.totalTax - half).toDecimal(), 0.10f, align = TextAlign.End)
                    }
                    Cell(item.totalCost.toDecimal(), 0.20f, align = TextAlign.End, bold = true)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(Modifier.height(12.dp))
            // Tax summary grouped by rate (spec 010 v2)
            val byRate = invoice.items
                .filter { it.taxInfos.isNotEmpty() }
                .groupBy { it.taxInfos.sumOf { ti -> ti.percentage } }
                .toList().sortedBy { it.first }
            if (byRate.isNotEmpty()) {
                Text(
                    "TAX SUMMARY (BY RATE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                byRate.forEach { (rate, items) ->
                    val taxable = items.sumOf { it.basePrice }
                    val tax = items.sumOf { it.totalTax }
                    val label = if (inter) {
                        "IGST ${rate.toDecimal()}% on ${taxable.toDecimal()}"
                    } else {
                        "CGST ${(rate / 2).toDecimal()}% + SGST ${(rate / 2).toDecimal()}% on ${taxable.toDecimal()}"
                    }
                    TotalRow(label, tax.toDecimal())
                }
                Spacer(Modifier.height(8.dp))
            }

            // Totals
            TotalRow("Taxable subtotal", invoice.basePrice.toDecimal())
            invoice.discount?.sumOf { it.value }?.takeIf { it > 0 }?.let {
                TotalRow("Discount", "- ${it.toDecimal()}")
            }
            TotalRow("Total tax", invoice.totalTax.toDecimal())
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            TotalRow("Grand total", "₹ ${invoice.totalCost.toDecimal()}", bold = true)
            Spacer(Modifier.height(8.dp))
            Text(
                "Amount in words: ${amountInWords(invoice.totalCost)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PartyBlock(title: String, name: String, address: String, gst: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (address.isNotBlank()) Text(address, style = MaterialTheme.typography.labelSmall)
            if (!gst.isNullOrBlank()) Text("GSTIN: $gst", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(
    text: String,
    weight: Float,
    align: TextAlign = TextAlign.Center,
    bold: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = align,
    )
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
        Text(value, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}
