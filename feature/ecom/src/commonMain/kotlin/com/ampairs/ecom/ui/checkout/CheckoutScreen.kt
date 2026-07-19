package com.ampairs.ecom.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_add_address
import ampairsapp.feature.ecom.generated.resources.ecom_checkout_title
import ampairsapp.feature.ecom.generated.resources.ecom_link_candidate_address
import ampairsapp.feature.ecom.generated.resources.ecom_link_candidate_cancel
import ampairsapp.feature.ecom.generated.resources.ecom_link_candidate_confirm
import ampairsapp.feature.ecom.generated.resources.ecom_link_candidate_gstin
import ampairsapp.feature.ecom.generated.resources.ecom_link_candidate_name
import ampairsapp.feature.ecom.generated.resources.ecom_link_candidate_phone
import ampairsapp.feature.ecom.generated.resources.ecom_link_candidate_subtitle
import ampairsapp.feature.ecom.generated.resources.ecom_link_candidate_title
import ampairsapp.feature.ecom.generated.resources.ecom_not_linked_got_it
import ampairsapp.feature.ecom.generated.resources.ecom_not_linked_title
import ampairsapp.feature.ecom.generated.resources.ecom_order_notes
import ampairsapp.feature.ecom.generated.resources.ecom_place_order
import ampairsapp.feature.ecom.generated.resources.ecom_select_address
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.ecom.api.model.LinkCandidateResponse
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.ui.components.EcomDimens
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onAddAddress: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    viewModel: CheckoutViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalAppLocale.current
    var notLinkedMessage by remember { mutableStateOf<String?>(null) }
    var linkCandidate by remember { mutableStateOf<LinkCandidateResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CheckoutEvent.OrderPlaced -> onOrderPlaced(event.orderRef)
                is CheckoutEvent.Error -> snackbar.showSnackbar(event.message)
                is CheckoutEvent.NotLinked -> notLinkedMessage = event.message
                is CheckoutEvent.LinkCandidateFound -> linkCandidate = event.candidate
            }
        }
    }

    notLinkedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { notLinkedMessage = null },
            title = { Text(stringResource(Res.string.ecom_not_linked_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { notLinkedMessage = null }) {
                    Text(stringResource(Res.string.ecom_not_linked_got_it))
                }
            },
        )
    }

    linkCandidate?.let { candidate ->
        LinkCandidateSheet(
            candidate = candidate,
            onConfirm = {
                linkCandidate = null
                viewModel.confirmLink(candidate.customerId)
            },
            onDismiss = { linkCandidate = null },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ScreenBackButton(onClick = onBack, contentDescription = "Back")
            Text(stringResource(Res.string.ecom_checkout_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(stringResource(Res.string.ecom_select_address), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))

            state.addresses.forEach { address ->
                AddressOption(
                    address = address,
                    selected = state.selectedAddressId == address.uid,
                    onSelect = { viewModel.selectAddress(address.uid) },
                )
            }

            OutlinedButton(onClick = onAddAddress, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(Res.string.ecom_add_address), modifier = Modifier.padding(start = 8.dp))
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = { Text(stringResource(Res.string.ecom_order_notes)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                minLines = 2,
            )
        }

        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("${state.itemCount} items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(state.subtotal, locale), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Button(onClick = viewModel::placeOrder, enabled = !state.isPlacing && state.selectedAddressId != null) {
                    if (state.isPlacing) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    Text(stringResource(Res.string.ecom_place_order))
                }
            }
        }
    }
}

@Composable
private fun AddressOption(address: CustomerAddressEntity, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, EcomDimens.cornerMd)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, EcomDimens.cornerMd)
            .clickable(onClick = onSelect)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column {
            Text(address.label ?: address.address_line1, style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(address.address_line1, address.address_line2, address.city, address.state, address.pin_code).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkCandidateSheet(candidate: LinkCandidateResponse, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(Res.string.ecom_link_candidate_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
            Text(
                stringResource(Res.string.ecom_link_candidate_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Column(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, EcomDimens.cornerMd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CandidateDetailRow(stringResource(Res.string.ecom_link_candidate_name), candidate.name)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                CandidateDetailRow(stringResource(Res.string.ecom_link_candidate_phone), candidate.phone)
                candidate.gstNumber?.takeIf { it.isNotBlank() }?.let { gstin ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CandidateDetailRow(stringResource(Res.string.ecom_link_candidate_gstin), gstin)
                }
                candidate.address?.takeIf { it.isNotBlank() }?.let { address ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CandidateDetailRow(stringResource(Res.string.ecom_link_candidate_address), address)
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.ecom_link_candidate_cancel))
                }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.ecom_link_candidate_confirm))
                }
            }
        }
    }
}

@Composable
private fun CandidateDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
