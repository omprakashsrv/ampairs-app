package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_amount_paid
import ampairsapp.feature.payment.generated.resources.payment_amount_received
import ampairsapp.feature.payment.generated.resources.payment_apply_to_bills_title
import ampairsapp.feature.payment.generated.resources.payment_applied_to_bills
import ampairsapp.feature.payment.generated.resources.payment_auto_allocate
import ampairsapp.feature.payment.generated.resources.payment_back
import ampairsapp.feature.payment.generated.resources.payment_bank_name
import ampairsapp.feature.payment.generated.resources.payment_cash_no_reference
import ampairsapp.feature.payment.generated.resources.payment_change
import ampairsapp.feature.payment.generated.resources.payment_cheque_date
import ampairsapp.feature.payment.generated.resources.payment_cheque_number
import ampairsapp.feature.payment.generated.resources.payment_cheque_pending_note
import ampairsapp.feature.payment.generated.resources.payment_choose_mode
import ampairsapp.feature.payment.generated.resources.payment_done
import ampairsapp.feature.payment.generated.resources.payment_drawee_bank
import ampairsapp.feature.payment.generated.resources.payment_kept_as_advance
import ampairsapp.feature.payment.generated.resources.payment_left_on_account
import ampairsapp.feature.payment.generated.resources.payment_cancel
import ampairsapp.feature.payment.generated.resources.payment_next_apply_to_bills
import ampairsapp.feature.payment.generated.resources.payment_next_choose_mode
import ampairsapp.feature.payment.generated.resources.payment_note_optional
import ampairsapp.feature.payment.generated.resources.payment_ok
import ampairsapp.feature.payment.generated.resources.payment_pick_date
import ampairsapp.feature.payment.generated.resources.payment_on_account_credit
import ampairsapp.feature.payment.generated.resources.payment_open_bills_oldest
import ampairsapp.feature.payment.generated.resources.payment_payment_mode_title
import ampairsapp.feature.payment.generated.resources.payment_received
import ampairsapp.feature.payment.generated.resources.payment_record_payment
import ampairsapp.feature.payment.generated.resources.payment_recorded
import ampairsapp.feature.payment.generated.resources.payment_reference_number
import ampairsapp.feature.payment.generated.resources.payment_save_payment
import ampairsapp.feature.payment.generated.resources.payment_saved_locally
import ampairsapp.feature.payment.generated.resources.payment_share_receipt
import ampairsapp.feature.payment.generated.resources.payment_status_fully_paid
import ampairsapp.feature.payment.generated.resources.payment_status_part_paid
import ampairsapp.feature.payment.generated.resources.payment_status_unpaid
import ampairsapp.feature.payment.generated.resources.payment_step_of
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.currencySymbol
import com.ampairs.payment.domain.PaymentDirection
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import com.ampairs.payment.ui.components.AmountKeypad
import com.ampairs.payment.ui.components.CollectionsColors
import com.ampairs.payment.ui.components.DirectionToggle
import com.ampairs.payment.ui.components.ModePickerGrid
import com.ampairs.payment.ui.components.icon
import com.ampairs.payment.ui.components.MoneyText
import com.ampairs.payment.ui.components.MonoAmount
import com.ampairs.payment.ui.components.PartyAvatar
import com.ampairs.payment.ui.components.SectionHeader
import com.ampairs.payment.ui.components.label
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

private enum class PayStep { AMOUNT, MODE, ALLOCATE }

/**
 * Record a collection (payment) against a party — the designed 3-step hero flow:
 * amount (keypad) → mode (+ mode-specific fields) → allocate to bills, then a "saved locally" finish.
 * Persists offline via [RecordPaymentViewModel] and updates the party balance immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    partyUid: String,
    onSaved: () -> Unit,
    voucherUid: String = "",
    onBack: () -> Unit = onSaved,
    viewModel: RecordPaymentViewModel = assistedMetroViewModel<RecordPaymentViewModel, RecordPaymentViewModel.Factory>(
        key = "$partyUid:$voucherUid",
    ) { create(partyUid, voucherUid) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(PayStep.AMOUNT) }
    var saved by rememberSaveable { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                // Desktop: close straight back to the list. Touch: show the saved-locally confirmation.
                is RecordPaymentEvent.Saved -> if (isDesktopPlatform()) onSaved() else saved = true
                is RecordPaymentEvent.Error -> errorMessage = event.message
            }
        }
    }

    if (saved) {
        PaymentSavedScreen(state = state, onDone = onSaved)
        return
    }

    // Desktop has a keyboard and room: log a payment in one form, no multi-step wizard.
    if (isDesktopPlatform()) {
        RecordPaymentDesktopForm(
            state = state,
            onBack = onBack,
            onDirection = viewModel::onDirectionChange,
            onAmountText = viewModel::onAmountChange,
            onModeChange = viewModel::onModeChange,
            onReference = viewModel::onReferenceChange,
            onChequeNumber = viewModel::onChequeNumberChange,
            onBankName = viewModel::onBankNameChange,
            onInstrumentDate = viewModel::onInstrumentDateChange,
            onNote = viewModel::onNarrationChange,
            onAutoAllocate = viewModel::autoAllocate,
            onClearAllocations = { state.openBills.forEach { viewModel.onAllocationChange(it.bill.billUid, "0") } },
            onSave = viewModel::save,
            errorMessage = errorMessage,
            onErrorDismiss = { errorMessage = null },
        )
        return
    }

    val received = state.direction == PaymentDirection.RECEIVED
    val stepIndex = step.ordinal + 1
    val title = when (step) {
        PayStep.AMOUNT -> stringResource(Res.string.payment_record_payment)
        PayStep.MODE -> stringResource(Res.string.payment_payment_mode_title)
        PayStep.ALLOCATE -> stringResource(Res.string.payment_apply_to_bills_title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        when (step) {
                            PayStep.AMOUNT -> onBack()
                            PayStep.MODE -> step = PayStep.AMOUNT
                            PayStep.ALLOCATE -> step = PayStep.MODE
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.payment_back))
                    }
                },
                title = { Text(title) },
                actions = {
                    Text(
                        stringResource(Res.string.payment_step_of, stepIndex),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (step) {
                PayStep.AMOUNT -> StepAmount(
                    state = state,
                    received = received,
                    onChange = onBack,
                    onDirection = viewModel::onDirectionChange,
                    onAmountText = viewModel::onAmountChange,
                    onNext = { step = PayStep.MODE },
                )
                PayStep.MODE -> StepMode(
                    state = state,
                    onModeChange = viewModel::onModeChange,
                    onReference = viewModel::onReferenceChange,
                    onChequeNumber = viewModel::onChequeNumberChange,
                    onBankName = viewModel::onBankNameChange,
                    onInstrumentDate = viewModel::onInstrumentDateChange,
                    onNote = viewModel::onNarrationChange,
                    onNext = { step = PayStep.ALLOCATE },
                )
                PayStep.ALLOCATE -> StepAllocate(
                    state = state,
                    onAutoAllocate = viewModel::autoAllocate,
                    onClearAllocations = { state.openBills.forEach { viewModel.onAllocationChange(it.bill.billUid, "0") } },
                    onSave = viewModel::save,
                )
            }
            errorMessage?.let { msg ->
                ErrorBanner(msg) { errorMessage = null }
            }
        }
    }
}

/** Single-page desktop variant — keyboard-first, no multi-step wizard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordPaymentDesktopForm(
    state: RecordPaymentUiState,
    onBack: () -> Unit,
    onDirection: (PaymentDirection) -> Unit,
    onAmountText: (String) -> Unit,
    onModeChange: (com.ampairs.payment.domain.PaymentMode) -> Unit,
    onReference: (String) -> Unit,
    onChequeNumber: (String) -> Unit,
    onBankName: (String) -> Unit,
    onInstrumentDate: (String) -> Unit,
    onNote: (String) -> Unit,
    onAutoAllocate: () -> Unit,
    onClearAllocations: () -> Unit,
    onSave: () -> Unit,
    errorMessage: String?,
    onErrorDismiss: () -> Unit,
) {
    val locale = LocalAppLocale.current
    val symbol = currencySymbol(locale.currencyCode)
    val received = state.direction == PaymentDirection.RECEIVED
    var auto by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(state.amountText, state.openBills.size) {
        if (auto) onAutoAllocate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.payment_back))
                    }
                },
                title = { Text(stringResource(Res.string.payment_record_payment)) },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                PartyHeaderChip(name = state.partyName.ifBlank { state.partyUid })

                DirectionToggle(
                    receivedSelected = received,
                    onReceived = { onDirection(PaymentDirection.RECEIVED) },
                    onPaid = { onDirection(PaymentDirection.PAID) },
                )

                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { onAmountText(it.filter { c -> c.isDigit() || c == '.' }) },
                    label = { Text(stringResource(if (received) Res.string.payment_amount_received else Res.string.payment_amount_paid)) },
                    prefix = { Text(symbol) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                ModeDropdown(modes = state.enabledModes, selected = state.mode, onSelect = onModeChange)
                ModeSpecificFields(
                    state = state,
                    onReference = onReference,
                    onChequeNumber = onChequeNumber,
                    onBankName = onBankName,
                    onInstrumentDate = onInstrumentDate,
                )

                OutlinedTextField(
                    value = state.narration,
                    onValueChange = onNote,
                    label = { Text(stringResource(Res.string.payment_note_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.openBills.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SummaryRow(stringResource(Res.string.payment_received), state.amount.toDouble(), MaterialTheme.colorScheme.onSurface)
                        SummaryRow(stringResource(Res.string.payment_applied_to_bills), state.allocatedTotal.toDouble(), CollectionsColors.receivable)
                        SummaryRow(stringResource(Res.string.payment_left_on_account), state.onAccount.toDouble(), CollectionsColors.payable)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            stringResource(Res.string.payment_auto_allocate),
                            modifier = Modifier.weight(1f).padding(start = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = auto,
                            onCheckedChange = { auto = it; if (it) onAutoAllocate() else onClearAllocations() },
                        )
                    }
                    SectionHeader(stringResource(Res.string.payment_open_bills_oldest), modifier = Modifier.padding(0.dp))
                    state.openBills.forEach { row -> AllocationBillRow(row) }
                }

                Button(
                    onClick = onSave,
                    enabled = !state.saving && state.amount.isPositive,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(20.dp))
                        Text(stringResource(Res.string.payment_save_payment))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            errorMessage?.let { msg -> ErrorBanner(msg, onErrorDismiss) }
        }
    }
}

@Composable
private fun StepAmount(
    state: RecordPaymentUiState,
    received: Boolean,
    onChange: () -> Unit,
    onDirection: (PaymentDirection) -> Unit,
    onAmountText: (String) -> Unit,
    onNext: () -> Unit,
) {
    val locale = LocalAppLocale.current
    val symbol = currencySymbol(locale.currencyCode)
    val amountLabel = stringResource(if (received) Res.string.payment_amount_received else Res.string.payment_amount_paid)
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Party header chip.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PartyAvatar(name = state.partyName.ifBlank { state.partyUid }, size = 40.dp)
            Text(
                state.partyName.ifBlank { state.partyUid },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 11.dp),
            )
            TextButton(onClick = onChange) { Text(stringResource(Res.string.payment_change)) }
        }

        Spacer(Modifier.height(14.dp))
        DirectionToggle(
            receivedSelected = received,
            onReceived = { onDirection(PaymentDirection.RECEIVED) },
            onPaid = { onDirection(PaymentDirection.PAID) },
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                amountLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
            )
            Text(
                text = symbol + state.amountText.ifBlank { "0" },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 46.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        AmountKeypad(
            onDigit = { c -> onAmountText(state.amountText + c) },
            onDot = { if (!state.amountText.contains('.')) onAmountText(state.amountText + ".") },
            onBackspace = { onAmountText(state.amountText.dropLast(1)) },
        )

        Button(
            onClick = onNext,
            enabled = state.amount.isPositive,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 8.dp),
        ) {
            Text(stringResource(Res.string.payment_next_choose_mode))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp).size(20.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepMode(
    state: RecordPaymentUiState,
    onModeChange: (com.ampairs.payment.domain.PaymentMode) -> Unit,
    onReference: (String) -> Unit,
    onChequeNumber: (String) -> Unit,
    onBankName: (String) -> Unit,
    onInstrumentDate: (String) -> Unit,
    onNote: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReceivedSummaryBanner(state)
        SectionHeader(stringResource(Res.string.payment_choose_mode), modifier = Modifier.padding(0.dp))
        ModePickerGrid(modes = state.enabledModes, selected = state.mode, onSelect = onModeChange)

        ModeSpecificFields(
            state = state,
            onReference = onReference,
            onChequeNumber = onChequeNumber,
            onBankName = onBankName,
            onInstrumentDate = onInstrumentDate,
        )

        OutlinedTextField(
            value = state.narration,
            onValueChange = onNote,
            label = { Text(stringResource(Res.string.payment_note_optional)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onNext,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(Res.string.payment_next_apply_to_bills))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 8.dp).size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Mode-specific inputs (cheque fields / instant info / reference) — shared by wizard + desktop form. */
@Composable
private fun ModeSpecificFields(
    state: RecordPaymentUiState,
    onReference: (String) -> Unit,
    onChequeNumber: (String) -> Unit,
    onBankName: (String) -> Unit,
    onInstrumentDate: (String) -> Unit,
) {
    when {
        state.mode.isCheque -> {
            OutlinedTextField(
                value = state.chequeNumber,
                onValueChange = onChequeNumber,
                label = { Text(stringResource(Res.string.payment_cheque_number)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ChequeDateField(value = state.instrumentDate, onChange = onInstrumentDate)
            OutlinedTextField(
                value = state.bankName,
                onValueChange = onBankName,
                label = { Text(stringResource(Res.string.payment_drawee_bank)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            NoteCard(
                icon = Icons.Filled.Info,
                text = stringResource(Res.string.payment_cheque_pending_note),
                container = MaterialTheme.colorScheme.secondaryContainer,
                onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        state.mode.isInstant -> {
            NoteCard(
                icon = Icons.Filled.Info,
                text = stringResource(Res.string.payment_cash_no_reference),
                container = MaterialTheme.colorScheme.surfaceContainer,
                onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {
            OutlinedTextField(
                value = state.referenceNumber,
                onValueChange = onReference,
                label = { Text(stringResource(Res.string.payment_reference_number)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StepAllocate(
    state: RecordPaymentUiState,
    onAutoAllocate: () -> Unit,
    onClearAllocations: () -> Unit,
    onSave: () -> Unit,
) {
    var auto by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(state.amountText, state.openBills.size) {
        if (auto) onAutoAllocate()
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Summary card.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SummaryRow(stringResource(Res.string.payment_received), state.amount.toDouble(), MaterialTheme.colorScheme.onSurface)
            SummaryRow(stringResource(Res.string.payment_applied_to_bills), state.allocatedTotal.toDouble(), CollectionsColors.receivable)
            SummaryRow(stringResource(Res.string.payment_left_on_account), state.onAccount.toDouble(), CollectionsColors.payable)
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(Res.string.payment_auto_allocate),
                modifier = Modifier.weight(1f).padding(start = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = auto,
                onCheckedChange = {
                    auto = it
                    if (it) onAutoAllocate() else onClearAllocations()
                },
            )
        }

        SectionHeader(stringResource(Res.string.payment_open_bills_oldest), modifier = Modifier.padding(top = 10.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.openBills, key = { it.bill.billUid }) { row ->
                AllocationBillRow(row)
            }
        }

        if (state.onAccount.isPositive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val locale = LocalAppLocale.current
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        stringResource(Res.string.payment_kept_as_advance, com.ampairs.common.locale.formatMoney(state.onAccount.toDouble(), locale)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        stringResource(Res.string.payment_on_account_credit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onSave,
            enabled = !state.saving,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (state.saving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(20.dp))
                Text(stringResource(Res.string.payment_save_payment))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MonoAmount(amount = amount, color = color)
    }
}

@Composable
private fun AllocationBillRow(row: AllocationRow) {
    val locale = LocalAppLocale.current
    val outstanding = row.bill.outstanding.minor.coerceAtLeast(1)
    val fraction = (row.allocatedMinor.toFloat() / outstanding).coerceIn(0f, 1f)
    val full = row.allocatedMinor >= row.bill.outstanding.minor && row.allocatedMinor > 0
    val part = row.allocatedMinor in 1 until row.bill.outstanding.minor
    val statusLabel = when {
        full -> stringResource(Res.string.payment_status_fully_paid)
        part -> stringResource(Res.string.payment_status_part_paid)
        else -> stringResource(Res.string.payment_status_unpaid)
    }
    val statusColor = when {
        full -> CollectionsColors.receivable
        part -> CollectionsColors.payable
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = if (row.allocatedMinor > 0) CollectionsColors.receivable else MaterialTheme.colorScheme.outlineVariant,
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.bill.billNo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                MonoAmount(amount = row.bill.total.toDouble(), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.bill.billDate.substringBefore('T'), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    statusLabel + " · " + com.ampairs.common.locale.formatMoney(com.ampairs.payment.domain.Money(row.allocatedMinor).toDouble(), locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .background(CollectionsColors.receivable, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun ReceivedSummaryBanner(state: RecordPaymentUiState) {
    val locale = LocalAppLocale.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (state.direction == PaymentDirection.RECEIVED) Icons.Filled.CheckCircle else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            com.ampairs.common.locale.formatMoney(state.amount.toDouble(), locale) + " · " + state.partyName.ifBlank { state.partyUid },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}

/** Read-only cheque-date field backed by a Material 3 date picker (no manual typing). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChequeDateField(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.payment_cheque_date)) },
            trailingIcon = { Icon(Icons.Filled.Event, contentDescription = stringResource(Res.string.payment_pick_date)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Transparent overlay so a tap anywhere on the field opens the picker.
        Box(modifier = Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = isoDateToMillis(value))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onChange(millisToIsoDate(it)) }
                    open = false
                }) { Text(stringResource(Res.string.payment_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(Res.string.payment_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Parse a stored `yyyy-MM-dd` (or ISO timestamp prefix) to UTC-midnight millis, or null if blank/invalid. */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun isoDateToMillis(value: String): Long? {
    val datePart = value.substringBefore('T').trim()
    if (datePart.isBlank()) return null
    return try {
        kotlinx.datetime.LocalDate.parse(datePart)
            .atStartOfDayIn(kotlinx.datetime.TimeZone.UTC)
            .toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }
}

/** Selected millis (UTC) → `yyyy-MM-dd`. */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun millisToIsoDate(millis: Long): String =
    kotlin.time.Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        .date
        .toString()

@Composable
private fun ModeDropdown(
    modes: List<com.ampairs.payment.domain.PaymentMode>,
    selected: com.ampairs.payment.domain.PaymentMode,
    onSelect: (com.ampairs.payment.domain.PaymentMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(selected.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                "  " + selected.label(),
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label()) },
                    leadingIcon = { Icon(mode.icon(), contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = { onSelect(mode); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun NoteCard(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, container: Color, onContainer: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = onContainer)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = onContainer, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(14.dp))
                .clickable(onClick = onDismiss)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

@Composable
private fun PaymentSavedScreen(state: RecordPaymentUiState, onDone: () -> Unit) {
    val locale = LocalAppLocale.current
    val received = state.direction == PaymentDirection.RECEIVED
    Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(84.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(48.dp))
            }
            Text(stringResource(Res.string.payment_recorded), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 22.dp))
            MoneyText(amount = state.amount.toDouble(), isReceivable = received, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(
                state.partyName.ifBlank { state.partyUid } + " · " + state.mode.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.payment_saved_locally), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(start = 8.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SummaryRow(stringResource(Res.string.payment_applied_to_bills), state.allocatedTotal.toDouble(), MaterialTheme.colorScheme.onSurface)
                SummaryRow(stringResource(Res.string.payment_left_on_account), state.onAccount.toDouble(), CollectionsColors.payable)
            }
        }
        Button(onClick = onDone, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(20.dp))
            Text(stringResource(Res.string.payment_share_receipt))
        }
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(stringResource(Res.string.payment_done))
        }
    }
}
