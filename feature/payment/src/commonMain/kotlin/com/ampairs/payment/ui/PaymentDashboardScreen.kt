package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_adjustment
import ampairsapp.feature.payment.generated.resources.payment_collections_title
import ampairsapp.feature.payment.generated.resources.payment_no_data
import ampairsapp.feature.payment.generated.resources.payment_opening_balance
import ampairsapp.feature.payment.generated.resources.payment_record_payment
import ampairsapp.feature.payment.generated.resources.payment_search_customer
import ampairsapp.feature.payment.generated.resources.payment_to_pay
import ampairsapp.feature.payment.generated.resources.payment_to_receive
import ampairsapp.feature.payment.generated.resources.payment_total_payable
import ampairsapp.feature.payment.generated.resources.payment_total_receivable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.payment.domain.AgingSummary
import com.ampairs.payment.domain.Direction
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Collections dashboard — the landing screen for the Payments module (Route.Payment).
 * Lists each party's current closing balance with a receivable/payable indicator.
 *
 * NOTE (spec 013, WIP): shows the party uid for now; resolving the customer display name and the
 * record-payment / statement / aging flows are follow-ups on the mobile UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDashboardScreen(
    onRecordPayment: () -> Unit,
    onNewAdjustment: () -> Unit,
    onSetOpeningBalance: () -> Unit,
    onOpenParty: (String) -> Unit,
    viewModel: PaymentDashboardViewModel = metroViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val filteredRows by viewModel.filteredRows.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val aging by viewModel.aging.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.payment_collections_title)) })
        },
        floatingActionButton = {
            Box {
                ExtendedFloatingActionButton(
                    onClick = { menuOpen = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.payment_record_payment)) },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.payment_record_payment)) },
                        onClick = { menuOpen = false; onRecordPayment() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.payment_adjustment)) },
                        onClick = { menuOpen = false; onNewAdjustment() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.payment_opening_balance)) },
                        onClick = { menuOpen = false; onSetOpeningBalance() },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            aging?.let { AgingSummaryCard(it, locale) }
            if (rows.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text(stringResource(Res.string.payment_search_customer)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            if (filteredRows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(Res.string.payment_no_data))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredRows, key = { it.balance.uid }) { row ->
                        val receivable = row.balance.closingDirection == Direction.DR
                        ListItem(
                            headlineContent = { Text(row.name) },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        if (receivable) Res.string.payment_to_receive
                                        else Res.string.payment_to_pay,
                                    ),
                                )
                            },
                            trailingContent = {
                                Text(formatMoney(row.balance.cachedClosingBalance.abs().toDouble(), locale))
                            },
                            modifier = Modifier.clickable { onOpenParty(row.balance.partyUid) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun AgingSummaryCard(aging: AgingSummary, locale: AppLocale) {
    Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.payment_total_receivable))
                Text(
                    formatMoney(aging.totalReceivable.toDouble(), locale),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.payment_total_payable))
                Text(
                    formatMoney(aging.totalPayable.toDouble(), locale),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            aging.buckets.filter { it.amount.isPositive }.forEach { bucket ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(bucket.label, style = MaterialTheme.typography.bodySmall)
                    Text(
                        formatMoney(bucket.amount.toDouble(), locale),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
