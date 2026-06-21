package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_amount
import ampairsapp.feature.payment.generated.resources.payment_date
import ampairsapp.feature.payment.generated.resources.payment_opening_balance
import ampairsapp.feature.payment.generated.resources.payment_save
import ampairsapp.feature.payment.generated.resources.payment_to_pay
import ampairsapp.feature.payment.generated.resources.payment_to_receive
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.payment.domain.Direction
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/** Set / edit a party's opening balance (cutover). Offline; recomputes the closing balance locally. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningBalanceScreen(
    partyUid: String,
    onSaved: () -> Unit,
    viewModel: OpeningBalanceViewModel = assistedMetroViewModel<OpeningBalanceViewModel, OpeningBalanceViewModel.Factory>(
        key = partyUid,
    ) { create(partyUid) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OpeningBalanceEvent.Saved -> onSaved()
                is OpeningBalanceEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.payment_opening_balance)) }) },
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.direction == Direction.DR,
                    onClick = { viewModel.onDirectionChange(Direction.DR) },
                    label = { Text(stringResource(Res.string.payment_to_receive)) },
                )
                FilterChip(
                    selected = state.direction == Direction.CR,
                    onClick = { viewModel.onDirectionChange(Direction.CR) },
                    label = { Text(stringResource(Res.string.payment_to_pay)) },
                )
            }

            OutlinedTextField(
                value = state.asOf,
                onValueChange = viewModel::onAsOfChange,
                label = { Text(stringResource(Res.string.payment_date)) },
                singleLine = true,
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
