package com.ampairs.ecom.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_add_address
import ampairsapp.feature.ecom.generated.resources.ecom_address_city
import ampairsapp.feature.ecom.generated.resources.ecom_address_default
import ampairsapp.feature.ecom.generated.resources.ecom_address_label
import ampairsapp.feature.ecom.generated.resources.ecom_address_line1
import ampairsapp.feature.ecom.generated.resources.ecom_address_line2
import ampairsapp.feature.ecom.generated.resources.ecom_address_phone
import ampairsapp.feature.ecom.generated.resources.ecom_address_pin
import ampairsapp.feature.ecom.generated.resources.ecom_address_state
import ampairsapp.feature.ecom.generated.resources.ecom_addresses_title
import ampairsapp.feature.ecom.generated.resources.ecom_no_addresses
import ampairsapp.feature.ecom.generated.resources.ecom_save
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.ui.components.EcomDimens
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddressesScreen(
    onBack: () -> Unit,
    viewModel: AddressesViewModel = metroViewModel(),
) {
    val addresses by viewModel.addresses.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<AddressDraft?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(stringResource(Res.string.ecom_addresses_title), style = MaterialTheme.typography.titleLarge)
        }

        val draft = editing
        if (draft != null) {
            AddressForm(
                draft = draft,
                onChange = { editing = it },
                onSave = { viewModel.save(draft); editing = null },
                onCancel = { editing = null },
            )
            return
        }

        if (addresses.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.ecom_no_addresses), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                addresses.forEach { address ->
                    AddressItem(address, onEdit = { editing = address.toDraft() }, onDelete = { viewModel.delete(address.uid) })
                }
            }
        }

        OutlinedButton(onClick = { editing = AddressDraft() }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(Res.string.ecom_add_address), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AddressItem(address: CustomerAddressEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).background(MaterialTheme.colorScheme.surfaceContainer, EcomDimens.cornerMd).clickable(onClick = onEdit).padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(address.label ?: address.address_line1, style = MaterialTheme.typography.titleSmall)
            Text(
                listOfNotNull(address.address_line1, address.address_line2, address.city, address.state, address.pin_code).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun AddressForm(draft: AddressDraft, onChange: (AddressDraft) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Field(draft.label, stringResource(Res.string.ecom_address_label)) { onChange(draft.copy(label = it)) }
        Field(draft.line1, stringResource(Res.string.ecom_address_line1)) { onChange(draft.copy(line1 = it)) }
        Field(draft.line2, stringResource(Res.string.ecom_address_line2)) { onChange(draft.copy(line2 = it)) }
        Field(draft.city, stringResource(Res.string.ecom_address_city)) { onChange(draft.copy(city = it)) }
        Field(draft.state, stringResource(Res.string.ecom_address_state)) { onChange(draft.copy(state = it)) }
        Field(draft.pinCode, stringResource(Res.string.ecom_address_pin)) { onChange(draft.copy(pinCode = it)) }
        Field(draft.phone, stringResource(Res.string.ecom_address_phone)) { onChange(draft.copy(phone = it)) }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft.isDefault, onCheckedChange = { onChange(draft.copy(isDefault = it)) })
            Text(stringResource(Res.string.ecom_address_default))
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onSave, enabled = draft.line1.isNotBlank() && draft.city.isNotBlank() && draft.state.isNotBlank() && draft.pinCode.isNotBlank(), modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.ecom_save))
            }
        }
    }
}

@Composable
private fun Field(value: String, label: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

private fun CustomerAddressEntity.toDraft() = AddressDraft(
    uid = uid,
    label = label.orEmpty(),
    line1 = address_line1,
    line2 = address_line2.orEmpty(),
    city = city,
    state = state,
    pinCode = pin_code,
    phone = phone.orEmpty(),
    isDefault = is_default == 1,
)
