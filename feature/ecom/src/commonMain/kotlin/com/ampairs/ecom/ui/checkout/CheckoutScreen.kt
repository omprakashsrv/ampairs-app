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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_add_address
import ampairsapp.feature.ecom.generated.resources.ecom_checkout_title
import ampairsapp.feature.ecom.generated.resources.ecom_order_notes
import ampairsapp.feature.ecom.generated.resources.ecom_place_order
import ampairsapp.feature.ecom.generated.resources.ecom_select_address
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.domain.asRupee
import com.ampairs.ecom.ui.components.EcomDimens
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun CheckoutScreen(
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onAddAddress: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    viewModel: CheckoutViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CheckoutEvent.OrderPlaced -> onOrderPlaced(event.orderRef)
                is CheckoutEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
                    Text(state.subtotal.asRupee(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
