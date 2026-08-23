package com.ampairs.ecom.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_aging_title
import ampairsapp.feature.ecom.generated.resources.ecom_amount_due
import ampairsapp.feature.ecom.generated.resources.ecom_bill_due
import ampairsapp.feature.ecom.generated.resources.ecom_bill_overdue
import ampairsapp.feature.ecom.generated.resources.ecom_in_credit
import ampairsapp.feature.ecom.generated.resources.ecom_no_statement
import ampairsapp.feature.ecom.generated.resources.ecom_open_bills
import ampairsapp.feature.ecom.generated.resources.ecom_outstanding_balance
import ampairsapp.feature.ecom.generated.resources.ecom_settled_up
import ampairsapp.feature.ecom.generated.resources.ecom_statement_activity
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatDate
import com.ampairs.common.locale.formatMoney
import com.ampairs.ecom.api.model.BuyerOpenBill
import com.ampairs.ecom.api.model.BuyerOutstanding
import com.ampairs.ecom.api.model.BuyerStatementLine
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource

/**
 * Buyer account statement — money position rendered in the same visual language as the seller-side
 * payment party statement: a colour-coded balance hero card, an ageing bar, open bills, and a
 * ledger-style activity list with directional icon badges and running balances.
 */
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
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
        ) {
            state.outstanding?.let { BalanceHeroCard(it) }

            state.outstanding?.let { out ->
                val agingSegments = out.aging.filter { it.amount != 0.0 }
                if (agingSegments.isNotEmpty()) {
                    MoneySectionHeader(stringResource(Res.string.ecom_aging_title))
                    val palette = StatementColors.agingBuckets
                    AgingBar(
                        segments = agingSegments.mapIndexed { i, b ->
                            AgingSegment(b.label, b.amount, palette[i % palette.size])
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                if (out.openBills.isNotEmpty()) {
                    MoneySectionHeader(stringResource(Res.string.ecom_open_bills))
                    out.openBills.forEach { OpenBillRow(it, locale) }
                }
            }

            MoneySectionHeader(stringResource(Res.string.ecom_statement_activity))
            val lines = state.statement?.lines.orEmpty()
            if (lines.isEmpty()) {
                Text(
                    stringResource(Res.string.ecom_no_statement),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                lines.forEach { StatementRow(it, locale) }
            }
        }
    }
}

@Composable
private fun BalanceHeroCard(out: BuyerOutstanding) {
    val settled = out.currentBalance == 0.0
    // DR = the buyer owes the seller (amount due); CR = the buyer is in credit. Mirror the payment
    // statement's colour rule: the "you owe" state uses the primary container, credit the tertiary.
    val due = out.balanceDirection.equals("DR", ignoreCase = true)
    val container = when {
        settled -> MaterialTheme.colorScheme.surfaceContainer
        due -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onContainer = when {
        settled -> MaterialTheme.colorScheme.onSurface
        due -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val caption = when {
        settled -> stringResource(Res.string.ecom_settled_up)
        due -> stringResource(Res.string.ecom_amount_due)
        else -> stringResource(Res.string.ecom_in_credit)
    }
    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(container, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text(
            stringResource(Res.string.ecom_outstanding_balance).uppercase() + " · " + caption.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = onContainer,
            letterSpacing = 0.4.sp,
        )
        MonoAmount(
            amount = abs(out.currentBalance),
            color = onContainer,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun OpenBillRow(bill: BuyerOpenBill, locale: AppLocale) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(bill.billNo.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val sub = if (bill.daysOverdue > 0) {
                stringResource(Res.string.ecom_bill_overdue, bill.daysOverdue)
            } else {
                stringResource(Res.string.ecom_bill_due, formatDate(bill.dueDate, locale))
            }
            val overdue = bill.daysOverdue > 0
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MonoAmount(amount = bill.outstanding, fontSize = 15.sp)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
}

@Composable
private fun StatementRow(line: BuyerStatementLine, locale: AppLocale) {
    val isDebit = line.debit > 0.0
    val delta = if (isDebit) line.debit else line.credit
    val deltaColor = if (isDebit) StatementColors.debit else StatementColors.credit
    val icon: ImageVector = if (isDebit) Icons.Filled.NorthEast else Icons.Filled.SouthWest
    val iconBg = if (isDebit) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val title = line.reference?.takeIf { it.isNotBlank() } ?: line.narration?.takeIf { it.isNotBlank() } ?: line.kind
    val ref = listOfNotNull(
        line.kind.takeIf { it.isNotBlank() },
        formatDate(line.date, locale).takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(34.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = deltaColor, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                MonoAmount(amount = delta, color = deltaColor, prefix = if (isDebit) "+ " else "– ")
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(ref, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(Res.string.ecom_running_balance, formatMoney(abs(line.runningBalance), locale)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
}
