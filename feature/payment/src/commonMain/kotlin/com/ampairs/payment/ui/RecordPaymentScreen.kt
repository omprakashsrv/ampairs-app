package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_amount
import ampairsapp.feature.payment.generated.resources.payment_bank_name
import ampairsapp.feature.payment.generated.resources.payment_cheque_number
import ampairsapp.feature.payment.generated.resources.payment_instrument_date
import ampairsapp.feature.payment.generated.resources.payment_mode
import ampairsapp.feature.payment.generated.resources.payment_narration
import ampairsapp.feature.payment.generated.resources.payment_record_payment
import ampairsapp.feature.payment.generated.resources.payment_reference_number
import ampairsapp.feature.payment.generated.resources.payment_save
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Record a collection (payment) against a party. Amount + mode (+ mode-specific reference / cheque
 * fields) + optional narration; Save persists offline via [RecordPaymentViewModel] and updates the
 * party balance immediately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    partyUid: String,
    onSaved: () -> Unit,
    voucherUid: String = "",
    viewModel: RecordPaymentViewModel = assistedMetroViewModel<RecordPaymentViewModel, RecordPaymentViewModel.Factory>(
        key = "$partyUid:$voucherUid",
    ) { create(partyUid, voucherUid) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecordPaymentEvent.Saved -> onSaved()
                is RecordPaymentEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.payment_record_payment)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.partyName.ifBlank { partyUid })

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(Res.string.payment_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            var modeMenu by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { modeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text("${stringResource(Res.string.payment_mode)}: ${state.mode.name}")
            }
            DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                state.enabledModes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name) },
                        onClick = {
                            viewModel.onModeChange(mode)
                            modeMenu = false
                        },
                    )
                }
            }

            if (state.mode.isCheque) {
                OutlinedTextField(
                    value = state.chequeNumber,
                    onValueChange = viewModel::onChequeNumberChange,
                    label = { Text(stringResource(Res.string.payment_cheque_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.bankName,
                    onValueChange = viewModel::onBankNameChange,
                    label = { Text(stringResource(Res.string.payment_bank_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.instrumentDate,
                    onValueChange = viewModel::onInstrumentDateChange,
                    label = { Text(stringResource(Res.string.payment_instrument_date)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (!state.mode.isInstant) {
                OutlinedTextField(
                    value = state.referenceNumber,
                    onValueChange = viewModel::onReferenceChange,
                    label = { Text(stringResource(Res.string.payment_reference_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = state.narration,
                onValueChange = viewModel::onNarrationChange,
                label = { Text(stringResource(Res.string.payment_narration)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(Res.string.payment_save))
                }
            }
        }
    }
}
