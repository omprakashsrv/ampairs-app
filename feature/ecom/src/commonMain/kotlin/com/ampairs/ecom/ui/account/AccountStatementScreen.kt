package com.ampairs.ecom.ui.account

import androidx.compose.foundation.background
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
import ampairsapp.feature.ecom.generated.resources.ecom_bill_due
import ampairsapp.feature.ecom.generated.resources.ecom_bill_overdue
import ampairsapp.feature.ecom.generated.resources.ecom_no_statement
import ampairsapp.feature.ecom.generated.resources.ecom_open_bills
import ampairsapp.feature.ecom.generated.resources.ecom_outstanding_balance
import ampairsapp.feature.ecom.generated.resources.ecom_statement_activity
import ampairsapp.feature.ecom.generated.resources.ecom_statement_title
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatDate
import com.ampairs.common.locale.formatMoney
import com.ampairs.ecom.api.model.BuyerOpenBill
import com.ampairs.ecom.api.model.BuyerStatementLine
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountStatementScreen(
    viewModel: StatementViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current

    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        state.outstanding == null && state.statement == null ->
            AccountReadError(notLinked = state.notLinked, onRetry = viewModel::load)
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(
                stringResource(Res.string.ecom_statement_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )

            state.outstanding?.let { out ->
                Column(
                    Modifier.fillMaxWidth().padding(top = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                        .padding(16.dp),
                ) {
                    Text(
                        stringResource(Res.string.ecom_outstanding_balance),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${formatMoney(out.currentBalance, locale)} ${out.balanceDirection}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (out.openBills.isNotEmpty()) {
                    SectionHeader(stringResource(Res.string.ecom_open_bills))
                    out.openBills.forEach { OpenBillRow(it) }
                }
            }

            SectionHeader(stringResource(Res.string.ecom_statement_activity))
            val lines = state.statement?.lines.orEmpty()
            if (lines.isEmpty()) {
                Text(
                    stringResource(Res.string.ecom_no_statement),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                lines.forEach { StatementRow(it) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun OpenBillRow(bill: BuyerOpenBill) {
    val locale = LocalAppLocale.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(bill.billNo.orEmpty(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            val sub = if (bill.daysOverdue > 0) {
                stringResource(Res.string.ecom_bill_overdue, bill.daysOverdue)
            } else {
                stringResource(Res.string.ecom_bill_due, formatDate(bill.dueDate, locale))
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatMoney(bill.outstanding, locale), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
}

@Composable
private fun StatementRow(line: BuyerStatementLine) {
    val locale = LocalAppLocale.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(line.reference ?: line.kind, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${line.kind} · ${formatDate(line.date, locale)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val amount = if (line.debit > 0.0) line.debit else line.credit
            val prefix = if (line.credit > 0.0) "-" else ""
            Text("$prefix${formatMoney(amount, locale)}", style = MaterialTheme.typography.bodyMedium)
            Text(formatMoney(line.runningBalance, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
}
