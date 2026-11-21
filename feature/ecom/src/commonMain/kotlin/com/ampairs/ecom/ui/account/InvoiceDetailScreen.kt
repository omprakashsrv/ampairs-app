package com.ampairs.ecom.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import ampairsapp.feature.ecom.generated.resources.ecom_invoice_for_order
import ampairsapp.feature.ecom.generated.resources.ecom_invoice_qty
import ampairsapp.feature.ecom.generated.resources.ecom_invoice_subtotal
import ampairsapp.feature.ecom.generated.resources.ecom_invoice_tax
import ampairsapp.feature.ecom.generated.resources.ecom_invoice_title
import ampairsapp.feature.ecom.generated.resources.ecom_invoice_total
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatDate
import com.ampairs.common.locale.formatMoney
import com.ampairs.ecom.api.model.BuyerInvoiceDetail
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun InvoiceDetailScreen(
    invoiceUid: String,
    viewModel: InvoiceDetailViewModel = assistedMetroViewModel<InvoiceDetailViewModel, InvoiceDetailViewModel.Factory>(
        key = invoiceUid,
    ) { create(invoiceUid) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        state.invoice == null -> AccountReadError(notLinked = state.notLinked, onRetry = viewModel::load)
        else -> InvoiceDetailContent(state.invoice!!)
    }
}

@Composable
private fun InvoiceDetailContent(invoice: BuyerInvoiceDetail) {
    val locale = LocalAppLocale.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "${stringResource(Res.string.ecom_invoice_title)} ${invoice.invoiceNumber}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            formatDate(invoice.invoiceDate, locale),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(invoice.status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        invoice.orderRef?.let {
            Text(
                stringResource(Res.string.ecom_invoice_for_order, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 14.dp))

        invoice.lines.forEach { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(line.description, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${stringResource(Res.string.ecom_invoice_qty, line.quantity.trimNum())} · ${formatMoney(line.unitPrice, locale)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatMoney(line.lineTotal, locale), style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 14.dp))

        TotalRow(stringResource(Res.string.ecom_invoice_subtotal), formatMoney(invoice.subtotal, locale))
        TotalRow(stringResource(Res.string.ecom_invoice_tax), formatMoney(invoice.taxTotal, locale))
        TotalRow(stringResource(Res.string.ecom_invoice_total), formatMoney(invoice.total, locale), emphasize = true)
    }
}

@Composable
private fun TotalRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Drop a trailing ".0" so quantities read "10" not "10.0" (no String.format on KMP). */
private fun Double.trimNum(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()
