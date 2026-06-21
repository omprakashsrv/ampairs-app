package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_clearance_bounced
import ampairsapp.feature.payment.generated.resources.payment_clearance_cleared
import ampairsapp.feature.payment.generated.resources.payment_clearance_pending
import ampairsapp.feature.payment.generated.resources.payment_mark_bounced
import ampairsapp.feature.payment.generated.resources.payment_mark_cleared
import ampairsapp.feature.payment.generated.resources.payment_no_data
import ampairsapp.feature.payment.generated.resources.payment_record_payment
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.payment.domain.ClearanceStatus
import com.ampairs.payment.domain.PaymentVoucher
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * A party's recorded receipts/payments with clearance status (US6). Pending cheques / online
 * receipts can be marked CLEARED or BOUNCED; a bounce posts a contra entry restoring the dues.
 * All offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyPaymentsScreen(
    partyUid: String,
    viewModel: PartyPaymentsViewModel = assistedMetroViewModel<PartyPaymentsViewModel, PartyPaymentsViewModel.Factory>(
        key = partyUid,
    ) { create(partyUid) },
) {
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val partyName by viewModel.partyName.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(partyName.ifBlank { stringResource(Res.string.payment_record_payment) }) })
        },
    ) { padding ->
        if (payments.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(Res.string.payment_no_data), modifier = Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(payments, key = { it.uid }) { voucher ->
                    PaymentRow(
                        voucher = voucher,
                        onMarkCleared = { viewModel.markCleared(voucher.uid) },
                        onMarkBounced = { viewModel.markBounced(voucher.uid) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentRow(
    voucher: PaymentVoucher,
    onMarkCleared: () -> Unit,
    onMarkBounced: () -> Unit,
) {
    val locale = LocalAppLocale.current
    val statusLabel = when (voucher.clearanceStatus) {
        ClearanceStatus.PENDING -> stringResource(Res.string.payment_clearance_pending)
        ClearanceStatus.CLEARED -> stringResource(Res.string.payment_clearance_cleared)
        ClearanceStatus.BOUNCED -> stringResource(Res.string.payment_clearance_bounced)
        ClearanceStatus.CANCELLED -> voucher.clearanceStatus.name
    }
    ListItem(
        headlineContent = { Text("${voucher.paymentMode.name} · ${voucher.direction.name}") },
        supportingContent = {
            Column {
                Text(voucher.voucherDate.substringBefore('T'))
                Text(statusLabel, style = MaterialTheme.typography.bodySmall)
                if (voucher.clearanceStatus == ClearanceStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onMarkCleared) { Text(stringResource(Res.string.payment_mark_cleared)) }
                        TextButton(onClick = onMarkBounced) { Text(stringResource(Res.string.payment_mark_bounced)) }
                    }
                }
            }
        },
        trailingContent = { Text(formatMoney(voucher.totalAmount.toDouble(), locale)) },
    )
}
