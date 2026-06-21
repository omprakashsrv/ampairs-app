package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_no_data
import ampairsapp.feature.payment.generated.resources.payment_opening_balance
import ampairsapp.feature.payment.generated.resources.payment_party_statement
import ampairsapp.feature.payment.generated.resources.payment_to_pay
import ampairsapp.feature.payment.generated.resources.payment_to_receive
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.payment.domain.Direction
import com.ampairs.payment.domain.StatementLine
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/** Party ledger statement — opening line + each movement with the running balance (US2). Offline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyStatementScreen(
    partyUid: String,
    viewModel: PartyStatementViewModel = assistedMetroViewModel<PartyStatementViewModel, PartyStatementViewModel.Factory>(
        key = partyUid,
    ) { create(partyUid) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    val statement = state.statement

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(state.partyName.ifBlank { stringResource(Res.string.payment_party_statement) }) })
        },
    ) { padding ->
        if (statement == null || statement.lines.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.payment_no_data), modifier = Modifier.padding(24.dp))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Closing balance header.
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(
                                if (statement.closingDirection == Direction.DR) Res.string.payment_to_receive
                                else Res.string.payment_to_pay,
                            ),
                        )
                        Text(formatMoney(statement.closingBalance.abs().toDouble(), locale))
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(statement.lines) { line ->
                        StatementRow(line = line)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatementRow(line: StatementLine) {
    val locale = LocalAppLocale.current
    val label = when {
        line.isOpening -> stringResource(Res.string.payment_opening_balance)
        else -> line.entryType?.name ?: line.narration.orEmpty()
    }
    val amount = if (line.debit.isPositive) line.debit else line.credit
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(line.entryDate.substringBefore('T')) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(amount.toDouble(), locale))
                Text(
                    formatMoney(line.runningBalance.abs().toDouble(), locale),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                )
            }
        },
    )
}
