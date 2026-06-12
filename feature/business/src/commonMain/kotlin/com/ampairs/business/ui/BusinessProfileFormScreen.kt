package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.business.ui.components.BusinessFormSection
import com.ampairs.business.ui.components.BusinessSaveBar
import com.ampairs.business.ui.components.BusinessScreenContent
import dev.zacsweers.metrox.viewmodel.metroViewModel

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/**
 * Business Profile & Registration — grouped Material 3 section cards (Company,
 * Owner & Contact, Registered Address, Online) with inline validation and a
 * dirty-state save bar (Ampairs design handoff).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileFormScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessProfileViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(
                if (uiState.wasCreateOperation) "Business profile created successfully"
                else "Business profile saved successfully"
            )
            viewModel.clearSaveSuccess()
        }
    }

    val form = uiState.formState
    // Baseline captured each time the loaded profile changes (load / save); edits make it dirty.
    val baseline = remember(uiState.profile) { uiState.formState }
    val isCreateMode = uiState.profile == null
    val emailError = if (form.email.isNotBlank() && !EMAIL_REGEX.matches(form.email))
        "Enter a valid email address" else null
    val nameError = if (form.name.isBlank()) "Business name is required" else null
    val valid = nameError == null && emailError == null
    val dirty = form != baseline

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (dirty) {
                BusinessSaveBar(
                    saving = uiState.isSaving,
                    enabled = valid,
                    onDiscard = { viewModel.updateFormState(baseline) },
                    onSave = { viewModel.saveProfile() },
                )
            }
        }
    ) { paddingValues ->
        BusinessScreenContent(
            modifier = Modifier.padding(paddingValues),
            maxContentWidth = 720.dp,
        ) {
            when {
                uiState.isLoading && uiState.profile == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                uiState.error != null && uiState.profile == null -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                else -> {
                    if (isCreateMode) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "You're creating a new business profile for your workspace",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    BusinessFormSection(
                        icon = Icons.Filled.Business,
                        title = "Company",
                        subtitle = "Appears on invoices, orders and tax documents",
                        error = nameError?.let { "1 error" },
                    ) {
                        OutlinedTextField(
                            value = form.name,
                            onValueChange = { viewModel.updateFormState(form.copy(name = it)) },
                            label = { Text("Business Name *") },
                            isError = nameError != null,
                            supportingText = nameError?.let { msg -> { Text(msg) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        BusinessTypeDropdown(
                            selectedType = form.businessType,
                            onTypeSelected = { viewModel.updateFormState(form.copy(businessType = it)) },
                        )
                        OutlinedTextField(
                            value = form.description,
                            onValueChange = { viewModel.updateFormState(form.copy(description = it)) },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    }

                    BusinessFormSection(
                        icon = Icons.Filled.AccountCircle,
                        title = "Owner & Contact",
                        subtitle = "Primary point of contact for this business",
                        error = emailError?.let { "1 error" },
                    ) {
                        OutlinedTextField(
                            value = form.ownerName,
                            onValueChange = { viewModel.updateFormState(form.copy(ownerName = it)) },
                            label = { Text("Owner Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        OutlinedTextField(
                            value = form.phone,
                            onValueChange = { viewModel.updateFormState(form.copy(phone = it)) },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        OutlinedTextField(
                            value = form.email,
                            onValueChange = { viewModel.updateFormState(form.copy(email = it)) },
                            label = { Text("Email") },
                            isError = emailError != null,
                            supportingText = emailError?.let { msg -> { Text(msg) } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    }

                    BusinessFormSection(
                        icon = Icons.Filled.LocationOn,
                        title = "Registered Address",
                        subtitle = "Printed on GST invoices",
                    ) {
                        OutlinedTextField(
                            value = form.addressLine1,
                            onValueChange = { viewModel.updateFormState(form.copy(addressLine1 = it)) },
                            label = { Text("Address Line 1") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        OutlinedTextField(
                            value = form.addressLine2,
                            onValueChange = { viewModel.updateFormState(form.copy(addressLine2 = it)) },
                            label = { Text("Address Line 2") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = form.city,
                                onValueChange = { viewModel.updateFormState(form.copy(city = it)) },
                                label = { Text("City") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            OutlinedTextField(
                                value = form.state,
                                onValueChange = { viewModel.updateFormState(form.copy(state = it)) },
                                label = { Text("State") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = form.postalCode,
                                onValueChange = { viewModel.updateFormState(form.copy(postalCode = it)) },
                                label = { Text("Postal Code") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            OutlinedTextField(
                                value = form.country,
                                onValueChange = { viewModel.updateFormState(form.copy(country = it)) },
                                label = { Text("Country") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = form.latitude,
                                onValueChange = { viewModel.updateFormState(form.copy(latitude = it)) },
                                label = { Text("Latitude") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            OutlinedTextField(
                                value = form.longitude,
                                onValueChange = { viewModel.updateFormState(form.copy(longitude = it)) },
                                label = { Text("Longitude") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                        }
                    }

                    BusinessFormSection(
                        icon = Icons.Filled.Language,
                        title = "Online",
                        subtitle = "Optional",
                    ) {
                        OutlinedTextField(
                            value = form.website,
                            onValueChange = { viewModel.updateFormState(form.copy(website = it)) },
                            label = { Text("Website") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = form.active,
                                onCheckedChange = { viewModel.updateFormState(form.copy(active = it)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val businessTypes = listOf(
        "RETAIL", "WHOLESALE", "SERVICE", "MANUFACTURING", "RESTAURANT", "ECOMMERCE", "OTHER"
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Business Type *") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            businessTypes.forEach { type ->
                DropdownMenuItem(text = { Text(type) }, onClick = { onTypeSelected(type); expanded = false })
            }
        }
    }
}
