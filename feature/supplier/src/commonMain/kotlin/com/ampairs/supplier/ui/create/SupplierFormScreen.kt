package com.ampairs.supplier.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import ampairsapp.feature.supplier.generated.resources.Res
import ampairsapp.feature.supplier.generated.resources.supplier_details_back
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_address
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_city
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_country
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_credit_days
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_credit_limit
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_email
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_gst
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_landline
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_name
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_pan
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_phone
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_pincode
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_ref_id
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_state
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_street
import ampairsapp.feature.supplier.generated.resources.supplier_form_field_street2
import ampairsapp.feature.supplier.generated.resources.supplier_form_save
import ampairsapp.feature.supplier.generated.resources.supplier_form_section_address
import ampairsapp.feature.supplier.generated.resources.supplier_form_section_contact
import ampairsapp.feature.supplier.generated.resources.supplier_form_section_tax
import ampairsapp.feature.supplier.generated.resources.supplier_form_title_create
import ampairsapp.feature.supplier.generated.resources.supplier_form_title_edit
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFormScreen(
    supplierId: String?,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SupplierFormViewModel = assistedMetroViewModel<SupplierFormViewModel, SupplierFormViewModel.Factory>(
        key = supplierId ?: "new"
    ) { create(supplierId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val form = uiState.form

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) stringResource(Res.string.supplier_form_title_edit)
                        else stringResource(Res.string.supplier_form_title_create)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSaveSuccess) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.supplier_details_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onSuccess = onSaveSuccess) },
                        enabled = !uiState.isSaving,
                    ) {
                        Text(stringResource(Res.string.supplier_form_save))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            FormField(
                value = form.name,
                onValueChange = { v -> viewModel.updateForm { it.copy(name = v, nameError = null) } },
                label = stringResource(Res.string.supplier_form_field_name),
                error = form.nameError,
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.refId,
                onValueChange = { v -> viewModel.updateForm { it.copy(refId = v) } },
                label = stringResource(Res.string.supplier_form_field_ref_id),
                imeAction = ImeAction.Next,
            )

            SectionLabel(stringResource(Res.string.supplier_form_section_contact))
            FormField(
                value = form.phone,
                onValueChange = { v -> viewModel.updateForm { it.copy(phone = v) } },
                label = stringResource(Res.string.supplier_form_field_phone),
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.landline,
                onValueChange = { v -> viewModel.updateForm { it.copy(landline = v) } },
                label = stringResource(Res.string.supplier_form_field_landline),
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.email,
                onValueChange = { v -> viewModel.updateForm { it.copy(email = v, emailError = null) } },
                label = stringResource(Res.string.supplier_form_field_email),
                error = form.emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )

            SectionLabel(stringResource(Res.string.supplier_form_section_address))
            FormField(
                value = form.address,
                onValueChange = { v -> viewModel.updateForm { it.copy(address = v) } },
                label = stringResource(Res.string.supplier_form_field_address),
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.street,
                onValueChange = { v -> viewModel.updateForm { it.copy(street = v) } },
                label = stringResource(Res.string.supplier_form_field_street),
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.street2,
                onValueChange = { v -> viewModel.updateForm { it.copy(street2 = v) } },
                label = stringResource(Res.string.supplier_form_field_street2),
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.city,
                onValueChange = { v -> viewModel.updateForm { it.copy(city = v) } },
                label = stringResource(Res.string.supplier_form_field_city),
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.state,
                onValueChange = { v -> viewModel.updateForm { it.copy(state = v) } },
                label = stringResource(Res.string.supplier_form_field_state),
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.pincode,
                onValueChange = { v -> viewModel.updateForm { it.copy(pincode = v) } },
                label = stringResource(Res.string.supplier_form_field_pincode),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.country,
                onValueChange = { v -> viewModel.updateForm { it.copy(country = v) } },
                label = stringResource(Res.string.supplier_form_field_country),
                imeAction = ImeAction.Next,
            )

            SectionLabel(stringResource(Res.string.supplier_form_section_tax))
            FormField(
                value = form.gstNumber,
                onValueChange = { v -> viewModel.updateForm { it.copy(gstNumber = v) } },
                label = stringResource(Res.string.supplier_form_field_gst),
                imeAction = ImeAction.Next,
            )
            FormField(
                value = form.panNumber,
                onValueChange = { v -> viewModel.updateForm { it.copy(panNumber = v) } },
                label = stringResource(Res.string.supplier_form_field_pan),
                imeAction = ImeAction.Next,
            )
            FormField(
                value = if (form.creditLimit > 0) form.creditLimit.toString() else "",
                onValueChange = { v -> viewModel.updateForm { it.copy(creditLimit = v.toDoubleOrNull() ?: 0.0) } },
                label = stringResource(Res.string.supplier_form_field_credit_limit),
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            )
            FormField(
                value = if (form.creditDays > 0) form.creditDays.toString() else "",
                onValueChange = { v -> viewModel.updateForm { it.copy(creditDays = v.toIntOrNull() ?: 0) } },
                label = stringResource(Res.string.supplier_form_field_credit_days),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            )
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
    )
}
