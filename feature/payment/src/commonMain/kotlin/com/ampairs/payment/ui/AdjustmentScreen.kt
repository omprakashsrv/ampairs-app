package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_adjustment
import ampairsapp.feature.payment.generated.resources.payment_adjustment_type
import ampairsapp.feature.payment.generated.resources.payment_amount
import ampairsapp.feature.payment.generated.resources.payment_back
import ampairsapp.feature.payment.generated.resources.payment_narration
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ampairs.payment.domain.AdjustmentType
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/** Create an adjustment (return / credit-debit note / write-off / purchase) against a party. Offline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustmentScreen(
    partyUid: String,
    onSaved: () -> Unit,
    onBack: () -> Unit = onSaved,
    viewModel: AdjustmentViewModel = assistedMetroViewModel<AdjustmentViewModel, AdjustmentViewModel.Factory>(
        key = partyUid,
    ) { create(partyUid) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AdjustmentEvent.Saved -> onSaved()
                is AdjustmentEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.payment_back))
                    }
                },
                title = { Text(stringResource(Res.string.payment_adjustment)) },
            )
        },
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
            PartyHeaderChip(name = state.partyName.ifBlank { partyUid })

            var typeMenu by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text("${stringResource(Res.string.payment_adjustment_type)}: ${state.type.name}")
            }
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                AdjustmentType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            viewModel.onTypeChange(type)
                            typeMenu = false
                        },
                    )
                }
            }

            OutlinedTextField(
                value = state.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(Res.string.payment_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

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
