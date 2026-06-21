package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_add_entry
import ampairsapp.feature.payment.generated.resources.payment_add_for_party
import ampairsapp.feature.payment.generated.resources.payment_add_for_party_sub
import ampairsapp.feature.payment.generated.resources.payment_back
import ampairsapp.feature.payment.generated.resources.payment_current_balance
import ampairsapp.feature.payment.generated.resources.payment_edit
import ampairsapp.feature.payment.generated.resources.payment_no_data
import ampairsapp.feature.payment.generated.resources.payment_opening_balance
import ampairsapp.feature.payment.generated.resources.payment_party_statement
import ampairsapp.feature.payment.generated.resources.payment_receipts
import ampairsapp.feature.payment.generated.resources.payment_record_adjustment
import ampairsapp.feature.payment.generated.resources.payment_record_adjustment_sub
import ampairsapp.feature.payment.generated.resources.payment_record_payment
import ampairsapp.feature.payment.generated.resources.payment_record_payment_sub
import ampairsapp.feature.payment.generated.resources.payment_send_statement
import ampairsapp.feature.payment.generated.resources.payment_set_opening_balance_sub
import ampairsapp.feature.payment.generated.resources.payment_statement_running
import ampairsapp.feature.payment.generated.resources.payment_to_pay
import ampairsapp.feature.payment.generated.resources.payment_to_receive
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.AppLocale
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.payment.domain.Direction
import com.ampairs.payment.domain.LedgerSourceType
import com.ampairs.payment.domain.StatementBuilder
import com.ampairs.payment.domain.StatementLine
import com.ampairs.payment.ui.components.CollectionsColors
import com.ampairs.payment.ui.components.MonoAmount
import com.ampairs.payment.ui.components.SectionHeader
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/** Party ledger statement — opening line + each movement with the running balance (US2). Offline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyStatementScreen(
    partyUid: String,
    onOpenPayments: () -> Unit = {},
    onRecordPayment: () -> Unit = {},
    onAdjustment: () -> Unit = {},
    onOpeningBalance: () -> Unit = {},
    onSendStatement: () -> Unit = {},
    onEditPayment: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: PartyStatementViewModel = assistedMetroViewModel<PartyStatementViewModel, PartyStatementViewModel.Factory>(
        key = partyUid,
    ) { create(partyUid) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    var sheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.payment_back))
                    }
                },
                title = { Text(state.partyName.ifBlank { stringResource(Res.string.payment_party_statement) }) },
                actions = {
                    IconButton(onClick = onSendStatement) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(Res.string.payment_send_statement))
                    }
                    IconButton(onClick = onOpenPayments) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = stringResource(Res.string.payment_receipts))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { sheetOpen = true },
                icon = { Icon(Icons.Filled.AddCard, contentDescription = null) },
                text = { Text(stringResource(Res.string.payment_add_entry)) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        },
    ) { padding ->
        PartyStatementContent(
            state = state,
            locale = locale,
            showInlineActions = false,
            onRecordPayment = onRecordPayment,
            onAdjustment = onAdjustment,
            onOpeningBalance = onOpeningBalance,
            onSendStatement = onSendStatement,
            onEditPayment = onEditPayment,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }, sheetState = sheetState) {
            AddEntrySheet(
                partyName = state.partyName.ifBlank { partyUid },
                onRecordPayment = { sheetOpen = false; onRecordPayment() },
                onAdjustment = { sheetOpen = false; onAdjustment() },
                onOpeningBalance = { sheetOpen = false; onOpeningBalance() },
            )
        }
    }
}

/**
 * Statement body shared by the phone screen and the desktop master–detail pane. When
 * [showInlineActions] is true (desktop), the per-customer actions render as inline buttons instead of
 * relying on a FAB + bottom sheet.
 */
@Composable
fun PartyStatementContent(
    state: PartyStatementUiState,
    locale: AppLocale,
    showInlineActions: Boolean,
    onRecordPayment: () -> Unit,
    onAdjustment: () -> Unit,
    onOpeningBalance: () -> Unit,
    onSendStatement: () -> Unit,
    modifier: Modifier = Modifier,
    onReceipts: () -> Unit = {},
    onEditPayment: (String) -> Unit = {},
) {
    val statement = state.statement
    if (statement == null || statement.lines.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.payment_no_data), modifier = Modifier.padding(24.dp))
        }
        return
    }
    val receivable = statement.closingDirection == Direction.DR
    val container = if (receivable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val onContainer = if (receivable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer

    Column(modifier = modifier) {
        // Balance hero card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .background(container, RoundedCornerShape(20.dp))
                .padding(18.dp),
        ) {
            Text(
                stringResource(Res.string.payment_current_balance).uppercase() + " · " +
                    stringResource(if (receivable) Res.string.payment_to_receive else Res.string.payment_to_pay).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = onContainer,
                letterSpacing = 0.4.sp,
            )
            MonoAmount(
                amount = statement.closingBalance.abs().toDouble(),
                color = onContainer,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Actions.
        if (showInlineActions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                androidx.compose.material3.Button(onClick = onRecordPayment) {
                    Icon(Icons.Filled.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + stringResource(Res.string.payment_record_payment))
                }
                OutlinedButton(onClick = onAdjustment) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + stringResource(Res.string.payment_record_adjustment))
                }
                OutlinedButton(onClick = onOpeningBalance) {
                    Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + stringResource(Res.string.payment_opening_balance))
                }
                OutlinedButton(onClick = onReceipts) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + stringResource(Res.string.payment_receipts))
                }
                OutlinedButton(onClick = onSendStatement) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + stringResource(Res.string.payment_send_statement))
                }
            }
        }

        SectionHeader(stringResource(Res.string.payment_statement_running))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(statement.lines) { line -> LedgerStatementRow(line, locale, onEditPayment) }
        }
    }
}

@Composable
private fun LedgerStatementRow(line: StatementLine, locale: AppLocale, onEditPayment: (String) -> Unit) {
    val editable = line.sourceType == LedgerSourceType.PAYMENT &&
        line.sourceUid.isNotBlank() &&
        !line.reversed &&
        !line.isReversal
    val isDebit = line.debit.isPositive
    val delta = if (isDebit) line.debit else line.credit
    val deltaColor = if (isDebit) CollectionsColors.payable else CollectionsColors.receivable
    val icon: ImageVector = when {
        line.isOpening -> Icons.Filled.Flag
        isDebit -> Icons.Filled.NorthEast
        else -> Icons.Filled.SouthWest
    }
    val iconBg = when {
        line.isOpening -> MaterialTheme.colorScheme.surfaceContainerHigh
        isDebit -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val title = when {
        line.isOpening -> stringResource(Res.string.payment_opening_balance)
        else -> line.entryType?.name?.humanize() ?: line.narration.orEmpty()
    }
    val ref = listOfNotNull(
        line.entryDate.substringBefore('T').takeIf { it.isNotBlank() },
        line.voucherNo.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (editable) Modifier.clickable { onEditPayment(line.sourceUid) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(34.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = deltaColor, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                MonoAmount(
                    amount = delta.toDouble(),
                    color = deltaColor,
                    prefix = if (isDebit) "+ " else "– ",
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ref, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Bal " + formatMoney(line.runningBalance.abs().toDouble(), locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (editable) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = stringResource(Res.string.payment_edit),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp).size(18.dp),
            )
        }
    }
}

@Composable
private fun AddEntrySheet(
    partyName: String,
    onRecordPayment: () -> Unit,
    onAdjustment: () -> Unit,
    onOpeningBalance: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text(stringResource(Res.string.payment_add_for_party), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.payment_add_for_party_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AddEntryRow(
            icon = Icons.Filled.AddCard,
            container = MaterialTheme.colorScheme.primaryContainer,
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
            title = stringResource(Res.string.payment_record_payment),
            sub = stringResource(Res.string.payment_record_payment_sub),
            onClick = onRecordPayment,
        )
        AddEntryRow(
            icon = Icons.Filled.Tune,
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
            title = stringResource(Res.string.payment_record_adjustment),
            sub = stringResource(Res.string.payment_record_adjustment_sub),
            onClick = onAdjustment,
        )
        AddEntryRow(
            icon = Icons.Filled.Flag,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
            title = stringResource(Res.string.payment_opening_balance),
            sub = stringResource(Res.string.payment_set_opening_balance_sub),
            onClick = onOpeningBalance,
        )
    }
}

@Composable
private fun AddEntryRow(
    icon: ImageVector,
    container: Color,
    onContainer: Color,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(44.dp).background(container, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = onContainer)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun String.humanize(): String =
    lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
