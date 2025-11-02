package com.ampairs.tax.ui.rates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.TaxType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun TaxRateFormScreen(
    taxRateId: String? = null,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaxRateFormViewModel = koinViewModel { parametersOf(taxRateId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTaxTypeDropdown by remember { mutableStateOf(false) }
    var showBusinessTypeDropdown by remember { mutableStateOf(false) }

    val isEditing = taxRateId != null

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (isEditing) "Edit Tax Rate" else "New Tax Rate") },
            actions = {
                TextButton(
                    onClick = {
                        viewModel.saveTaxRate(onSaveSuccess)
                    },
                    enabled = uiState.canSave
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val focusManager = LocalFocusManager.current

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show error if present
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // HSN Code Field
                OutlinedTextField(
                    value = uiState.hsnCode,
                    onValueChange = { viewModel.updateHsnCode(it) },
                    label = { Text("HSN Code") },
                    placeholder = { Text("Enter 4-8 digit HSN code") },
                    supportingText = {
                        Text("Valid formats: 4, 6, or 8 digits (e.g., 1234, 123456, 12345678)")
                    },
                    isError = uiState.hsnCode.isNotBlank() && !uiState.isValidHsnCode,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tax Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = showTaxTypeDropdown,
                    onExpandedChange = { showTaxTypeDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = getTaxTypeDisplayName(uiState.taxType),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Tax Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTaxTypeDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = showTaxTypeDropdown,
                        onDismissRequest = { showTaxTypeDropdown = false }
                    ) {
                        TaxType.entries.forEach { taxType ->
                            DropdownMenuItem(
                                text = { Text(getTaxTypeDisplayName(taxType)) },
                                onClick = {
                                    viewModel.updateTaxType(taxType)
                                    showTaxTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Rate Percentage Field
                OutlinedTextField(
                    value = uiState.ratePercentageText,
                    onValueChange = { viewModel.updateRatePercentage(it) },
                    label = { Text("Tax Rate (%)") },
                    placeholder = { Text("Enter tax rate percentage") },
                    supportingText = {
                        Text("Enter percentage between 0 and 100")
                    },
                    isError = uiState.ratePercentageText.isNotBlank() && !uiState.isValidRate,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    suffix = { Text("%") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Cess Rate Field (Optional)
                OutlinedTextField(
                    value = uiState.cessRateText,
                    onValueChange = { viewModel.updateCessRate(it) },
                    label = { Text("Cess Rate (%) - Optional") },
                    placeholder = { Text("Enter cess rate percentage") },
                    supportingText = {
                        Text("Optional: Additional cess percentage")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    suffix = { Text("%") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Cess Amount Per Unit Field (Optional)
                OutlinedTextField(
                    value = uiState.cessAmountPerUnitText,
                    onValueChange = { viewModel.updateCessAmountPerUnit(it) },
                    label = { Text("Cess Amount per Unit - Optional") },
                    placeholder = { Text("Enter fixed cess amount per unit") },
                    supportingText = {
                        Text("Optional: Fixed amount per unit instead of percentage")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    prefix = { Text("₹") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Business Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = showBusinessTypeDropdown,
                    onExpandedChange = { showBusinessTypeDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = getBusinessTypeDisplayName(uiState.businessType),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Business Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBusinessTypeDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = showBusinessTypeDropdown,
                        onDismissRequest = { showBusinessTypeDropdown = false }
                    ) {
                        BusinessType.entries.forEach { businessType ->
                            DropdownMenuItem(
                                text = { Text(getBusinessTypeDisplayName(businessType)) },
                                onClick = {
                                    viewModel.updateBusinessType(businessType)
                                    showBusinessTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                // Geographical Zone Field
                OutlinedTextField(
                    value = uiState.geographicalZone,
                    onValueChange = { viewModel.updateGeographicalZone(it) },
                    label = { Text("Geographical Zone") },
                    placeholder = { Text("e.g., PAN_INDIA, MUMBAI, DELHI") },
                    supportingText = {
                        Text("Specify the geographical applicability")
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Effective Date Section
                Text(
                    text = "Effective Period",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Effective From Date (simplified - using current date)
                OutlinedTextField(
                    value = formatDate(uiState.effectiveFrom),
                    onValueChange = { },
                    label = { Text("Effective From") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            // TODO: Show date picker
                            viewModel.updateEffectiveFrom(Clock.System.now().toEpochMilliseconds())
                        }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Active Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Switch(
                        checked = uiState.isActive,
                        onCheckedChange = { viewModel.updateIsActive(it) }
                    )
                }

                // Preview Tax Rate
                if (uiState.ratePercentage > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Tax Rate Preview",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.formattedTaxRate,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total: ${uiState.totalTaxRate}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Save Button Section
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveTaxRate {
                                onSaveSuccess()
                            }
                        },
                        enabled = uiState.canSave,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        } else {
                            Text("Save Tax Rate")
                        }
                    }
                }
            }
        }
    }
}

private fun getTaxTypeDisplayName(taxType: TaxType): String {
    return when (taxType) {
        TaxType.GST -> "GST"
        TaxType.CGST -> "CGST"
        TaxType.SGST -> "SGST"
        TaxType.IGST -> "IGST"
        TaxType.CESS -> "Cess"
        TaxType.VAT -> "VAT"
        TaxType.EXCISE -> "Excise"
    }
}

private fun getBusinessTypeDisplayName(businessType: BusinessType): String {
    return when (businessType) {
        BusinessType.REGULAR -> "Regular"
        BusinessType.COMPOSITION -> "Composition"
        BusinessType.EXEMPT -> "Exempt"
        BusinessType.NIL_RATED -> "Nil Rated"
        BusinessType.ZERO_RATED -> "Zero Rated"
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Select date"

    // Simple date formatting - in a real app, use proper date formatting
    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
    return date.toString().take(10) // Just show YYYY-MM-DD
}