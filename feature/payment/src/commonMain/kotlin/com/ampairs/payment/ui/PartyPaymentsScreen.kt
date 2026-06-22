package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_back
import ampairsapp.feature.payment.generated.resources.payment_cancel_voucher
import ampairsapp.feature.payment.generated.resources.payment_clearance_bounced
import ampairsapp.feature.payment.generated.resources.payment_edit
import ampairsapp.feature.payment.generated.resources.payment_mark_cleared
import ampairsapp.feature.payment.generated.resources.payment_no_data
import ampairsapp.feature.payment.generated.resources.payment_receipts
import ampairsapp.feature.payment.generated.resources.payment_recorded_receipts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.payment.domain.ClearanceStatus
import com.ampairs.payment.domain.PaymentVoucher
import com.ampairs.payment.ui.components.ClearanceChip
import com.ampairs.payment.ui.components.SectionHeader
import com.ampairs.payment.ui.components.label
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
    onEditVoucher: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: PartyPaymentsViewModel = assistedMetroViewModel<PartyPaymentsViewModel, PartyPaymentsViewModel.Factory>(
        key = partyUid,
    ) { create(partyUid) },
) {
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val partyName by viewModel.partyName.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.payment_back))
                    }
                },
                title = {
                    Column {
                        Text(stringResource(Res.string.payment_receipts))
                        if (partyName.isNotBlank()) {
                            Text(
                                partyName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (payments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.payment_no_data), modifier = Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item { SectionHeader(stringResource(Res.string.payment_recorded_receipts)) }
                items(payments, key = { it.uid }) { voucher ->
                    ReceiptCard(
                        voucher = voucher,
                        onMarkCleared = { viewModel.markCleared(voucher.uid) },
                        onMarkBounced = { viewModel.markBounced(voucher.uid) },
                        onEdit = { onEditVoucher(voucher.uid) },
                        onCancel = { viewModel.cancel(voucher.uid) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptCard(
    voucher: PaymentVoucher,
    onMarkCleared: () -> Unit,
    onMarkBounced: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    val locale = LocalAppLocale.current
    val meta = buildList {
        voucher.referenceNumber?.takeIf { it.isNotBlank() }?.let { add("#$it") }
        voucher.bankName?.takeIf { it.isNotBlank() }?.let { add(it) }
        voucher.voucherDate.substringBefore('T').takeIf { it.isNotBlank() }?.let { add(it) }
    }.joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatMoney(voucher.totalAmount.toDouble(), locale),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    voucher.paymentMode.label() + (if (meta.isNotBlank()) " · $meta" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ClearanceChip(voucher.clearanceStatus)
        }
        if (voucher.clearanceStatus == ClearanceStatus.PENDING) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    label = stringResource(Res.string.payment_mark_cleared),
                    icon = Icons.Filled.CheckCircle,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onMarkCleared,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = stringResource(Res.string.payment_clearance_bounced),
                    icon = Icons.Filled.Block,
                    container = MaterialTheme.colorScheme.errorContainer,
                    onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = onMarkBounced,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (!voucher.isTerminal) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text(stringResource(Res.string.payment_edit)) }
                TextButton(onClick = onCancel) { Text(stringResource(Res.string.payment_cancel_voucher)) }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .background(container, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(18.dp))
        Text(" $label", color = onContainer, style = MaterialTheme.typography.labelLarge)
    }
}
