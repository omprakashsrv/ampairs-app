package com.ampairs.ecom.ui.account

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
import ampairsapp.feature.ecom.generated.resources.ecom_invoices_title
import ampairsapp.feature.ecom.generated.resources.ecom_no_invoices
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatDate
import com.ampairs.common.locale.formatMoney
import com.ampairs.ecom.api.model.BuyerInvoiceSummary
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun InvoiceListScreen(
    onOpenInvoice: (String) -> Unit,
    viewModel: InvoiceListViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(Res.string.ecom_invoices_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(16.dp),
        )
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> AccountReadError(message = state.error, onRetry = viewModel::load)
            state.invoices.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(Res.string.ecom_no_invoices), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.invoices, key = { it.invoiceUid }) { invoice ->
                    InvoiceRow(invoice, onClick = { onOpenInvoice(invoice.invoiceUid) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: BuyerInvoiceSummary, onClick: () -> Unit) {
    val locale = LocalAppLocale.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(invoice.invoiceNumber, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                formatDate(invoice.invoiceDate, locale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            invoice.orderRef?.let {
                Text(
                    stringResource(Res.string.ecom_invoice_for_order, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatMoney(invoice.total, locale), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(invoice.status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
