package com.ampairs.tax.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.IndianStates
import com.ampairs.tax.domain.TaxBreakdownItem
import com.ampairs.tax.domain.TaxCalculationResult
import com.ampairs.tax.domain.TransactionType
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxCalculatorScreen(
    modifier: Modifier = Modifier,
    viewModel: TaxCalculatorViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tax Calculator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Input Form
        TaxCalculationForm(
            formState = uiState.formState,
            onFormChange = viewModel::updateForm,
            isCalculating = uiState.isCalculating,
            onCalculate = viewModel::calculateTax
        )

        // Error Display
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Calculation Result
        uiState.calculationResult?.let { result ->
            TaxCalculationResultCard(result = result)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxCalculationForm(
    formState: TaxCalculationFormState,
    onFormChange: (TaxCalculationFormState) -> Unit,
    isCalculating: Boolean,
    onCalculate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Calculation Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            // HSN Code
            OutlinedTextField(
                value = formState.hsnCode,
                onValueChange = { onFormChange(formState.copy(hsnCode = it)) },
                label = { Text("HSN Code *") },
                placeholder = { Text("e.g., 1001") },
                modifier = Modifier.fillMaxWidth(),
                isError = formState.hsnCodeError != null,
                supportingText = formState.hsnCodeError?.let { { Text(it) } }
            )

            // Base Amount and Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = formState.baseAmount,
                    onValueChange = { onFormChange(formState.copy(baseAmount = it)) },
                    label = { Text("Base Amount *") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₹") },
                    isError = formState.baseAmountError != null,
                    supportingText = formState.baseAmountError?.let { { Text(it) } }
                )

                OutlinedTextField(
                    value = formState.quantity,
                    onValueChange = { onFormChange(formState.copy(quantity = it)) },
                    label = { Text("Quantity") },
                    placeholder = { Text("1") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // State Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateDropdown(
                    label = "Source State *",
                    selectedState = formState.sourceState,
                    onStateChange = { onFormChange(formState.copy(sourceState = it)) },
                    modifier = Modifier.weight(1f),
                    isError = formState.sourceStateError != null
                )

                StateDropdown(
                    label = "Destination State *",
                    selectedState = formState.destinationState,
                    onStateChange = { onFormChange(formState.copy(destinationState = it)) },
                    modifier = Modifier.weight(1f),
                    isError = formState.destinationStateError != null
                )
            }

            // Business Type and Transaction Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BusinessTypeDropdown(
                    selectedBusinessType = formState.businessType,
                    onBusinessTypeChange = { onFormChange(formState.copy(businessType = it)) },
                    modifier = Modifier.weight(1f)
                )

                TransactionTypeDropdown(
                    selectedTransactionType = formState.transactionType,
                    onTransactionTypeChange = { onFormChange(formState.copy(transactionType = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Calculate Button
            Button(
                onClick = onCalculate,
                enabled = formState.isValid && !isCalculating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCalculating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCalculating) "Calculating..." else "Calculate Tax")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateDropdown(
    label: String,
    selectedState: String,
    onStateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = IndianStates.getStateName(selectedState) ?: selectedState.ifEmpty { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            isError = isError
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            IndianStates.stateCodes.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onStateChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessTypeDropdown(
    selectedBusinessType: BusinessType,
    onBusinessTypeChange: (BusinessType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedBusinessType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Business Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BusinessType.entries.forEach { businessType ->
                DropdownMenuItem(
                    text = { Text(businessType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onBusinessTypeChange(businessType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTypeDropdown(
    selectedTransactionType: TransactionType,
    onTransactionTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedTransactionType.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Transaction Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TransactionType.entries.forEach { transactionType ->
                DropdownMenuItem(
                    text = { Text(transactionType.name) },
                    onClick = {
                        onTransactionTypeChange(transactionType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TaxCalculationResultCard(
    result: TaxCalculationResult,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tax Calculation Result",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Summary
            TaxSummarySection(result = result)

            // Detailed Breakdown
            if (result.taxBreakdown.isNotEmpty()) {
                TaxBreakdownSection(breakdown = result.taxBreakdown)
            }
        }
    }
}

@Composable
private fun TaxSummarySection(
    result: TaxCalculationResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        TaxSummaryRow(label = "Base Amount", value = "₹${result.baseAmount}")
        TaxSummaryRow(label = "Quantity", value = result.quantity.toString())

        if (result.isIntraState) {
            TaxSummaryRow(label = "CGST", value = "₹${result.cgstAmount}")
            TaxSummaryRow(label = "SGST", value = "₹${result.sgstAmount}")
        } else {
            TaxSummaryRow(label = "IGST", value = "₹${result.igstAmount}")
        }

        if (result.cessAmount > 0) {
            TaxSummaryRow(label = "CESS", value = "₹${result.cessAmount}")
        }

        HorizontalDivider()

        TaxSummaryRow(
            label = "Total Tax",
            value = "₹${result.totalTaxAmount}",
            isTotal = true
        )

        TaxSummaryRow(
            label = "Total Amount",
            value = "₹${result.totalAmount}",
            isTotal = true
        )
    }
}

@Composable
private fun TaxSummaryRow(
    label: String,
    value: String,
    isTotal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = if (isTotal) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium,
            color = if (isTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TaxBreakdownSection(
    breakdown: List<TaxBreakdownItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Detailed Breakdown",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        breakdown.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.taxType.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "₹${item.taxAmount}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${item.ratePercentage}% on ₹${item.taxableAmount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.description.isNotEmpty()) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}