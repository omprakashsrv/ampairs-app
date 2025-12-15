package com.ampairs.product.ui.variant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Variant Form Screen - Create or edit a product variant
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantFormScreen(
    productId: String,
    variantId: String?,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VariantFormViewModel = koinInject { parametersOf(productId, variantId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val attributeOptions by viewModel.attributeOptions.collectAsState()
    val focusManager = LocalFocusManager.current

    // Handle success
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearSuccessMessage()
            onSaveSuccess()
        }
    }

    // Show error message
    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Variant" else "New Variant") }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveSuccess,
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { viewModel.saveVariant(onSaveSuccess) },
                        modifier = Modifier.weight(1f),
                        enabled = formState.isValid && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Basic Information Section
                FormSection(title = "Basic Information") {
                    OutlinedTextField(
                        value = formState.sku,
                        onValueChange = { viewModel.updateForm(formState.copy(sku = it)) },
                        label = { Text("SKU *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = formState.skuError != null,
                        supportingText = formState.skuError?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = formState.variantName,
                        onValueChange = { viewModel.updateForm(formState.copy(variantName = it)) },
                        label = { Text("Variant Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = formState.nameError != null,
                        supportingText = formState.nameError?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }

                // Attributes Section
                FormSection(title = "Attributes") {
                    if (formState.attributeError != null) {
                        Text(
                            text = formState.attributeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Attribute 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttributeNameField(
                            value = formState.attribute1Name ?: "",
                            onValueChange = { viewModel.updateForm(formState.copy(attribute1Name = it)) },
                            availableNames = attributeOptions.availableAttributeNames,
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next
                        )
                        AttributeValueField(
                            value = formState.attribute1Value ?: "",
                            onValueChange = { viewModel.updateForm(formState.copy(attribute1Value = it)) },
                            attributeName = formState.attribute1Name,
                            availableValues = formState.attribute1Name?.let {
                                attributeOptions.getValuesForAttribute(it)
                            } ?: emptyList(),
                            onAttributeNameSelected = { viewModel.loadAttributeValues(it) },
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next
                        )
                    }

                    // Attribute 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttributeNameField(
                            value = formState.attribute2Name ?: "",
                            onValueChange = { viewModel.updateForm(formState.copy(attribute2Name = it)) },
                            availableNames = attributeOptions.availableAttributeNames,
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next
                        )
                        AttributeValueField(
                            value = formState.attribute2Value ?: "",
                            onValueChange = { viewModel.updateForm(formState.copy(attribute2Value = it)) },
                            attributeName = formState.attribute2Name,
                            availableValues = formState.attribute2Name?.let {
                                attributeOptions.getValuesForAttribute(it)
                            } ?: emptyList(),
                            onAttributeNameSelected = { viewModel.loadAttributeValues(it) },
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next
                        )
                    }

                    // Attribute 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AttributeNameField(
                            value = formState.attribute3Name ?: "",
                            onValueChange = { viewModel.updateForm(formState.copy(attribute3Name = it)) },
                            availableNames = attributeOptions.availableAttributeNames,
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next
                        )
                        AttributeValueField(
                            value = formState.attribute3Value ?: "",
                            onValueChange = { viewModel.updateForm(formState.copy(attribute3Value = it)) },
                            attributeName = formState.attribute3Name,
                            availableValues = formState.attribute3Name?.let {
                                attributeOptions.getValuesForAttribute(it)
                            } ?: emptyList(),
                            onAttributeNameSelected = { viewModel.loadAttributeValues(it) },
                            modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next
                        )
                    }
                }

                // Pricing Section
                FormSection(
                    title = "Pricing (Optional Overrides)",
                    subtitle = "Leave blank to use base product pricing"
                ) {
                    if (formState.priceError != null) {
                        Text(
                            text = formState.priceError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = formState.mrp?.toString() ?: "",
                        onValueChange = {
                            viewModel.updateForm(formState.copy(mrp = it.toDoubleOrNull()))
                        },
                        label = { Text("MRP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = formState.dealerPrice?.toString() ?: "",
                        onValueChange = {
                            viewModel.updateForm(formState.copy(dealerPrice = it.toDoubleOrNull()))
                        },
                        label = { Text("Dealer Price") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = formState.sellingPrice?.toString() ?: "",
                        onValueChange = {
                            viewModel.updateForm(formState.copy(sellingPrice = it.toDoubleOrNull()))
                        },
                        label = { Text("Selling Price") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }

                // Stock Management Section
                FormSection(title = "Stock Management") {
                    OutlinedTextField(
                        value = formState.stockQuantity.toString(),
                        onValueChange = {
                            viewModel.updateForm(formState.copy(stockQuantity = it.toDoubleOrNull() ?: 0.0))
                        },
                        label = { Text("Stock Quantity *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    OutlinedTextField(
                        value = formState.lowStockAlert?.toString() ?: "",
                        onValueChange = {
                            viewModel.updateForm(formState.copy(lowStockAlert = it.toDoubleOrNull()))
                        },
                        label = { Text("Low Stock Alert") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }

                // Status Section
                FormSection(title = "Status") {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (formState.active) "Active" else "Inactive",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Status") },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Active") },
                                onClick = {
                                    viewModel.updateForm(formState.copy(active = true))
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Inactive") },
                                onClick = {
                                    viewModel.updateForm(formState.copy(active = false))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Bottom spacing for keyboard
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeNameField(
    value: String,
    onValueChange: (String) -> Unit,
    availableNames: List<String>,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Name") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        if (availableNames.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableNames.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onValueChange(name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeValueField(
    value: String,
    onValueChange: (String) -> Unit,
    attributeName: String?,
    availableValues: List<String>,
    onAttributeNameSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Load values when attribute name is selected
    LaunchedEffect(attributeName) {
        attributeName?.let { onAttributeNameSelected(it) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Value") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        if (availableValues.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableValues.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value) },
                        onClick = {
                            onValueChange(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}
