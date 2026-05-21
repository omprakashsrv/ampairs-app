package com.ampairs.tax.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.tax.calculation.model.*
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.util.formatDecimal
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Tax Calculator Screen
 *
 * Offline-first tax calculator that uses workspace tax codes and calculation engine
 * Features:
 * - Select from user's subscribed tax codes
 * - Live tax calculation with component-wise breakdown
 * - Support for different transaction types (B2B, B2C, etc.)
 * - Jurisdiction-based calculation (intra-state, inter-state)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxCalculatorScreen(
    viewModel: TaxCalculatorViewModel = metroViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val workspaceCodes by viewModel.workspaceCodes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Calculator") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Tax Calculation Input",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Tax Code Selection
                    TaxCodeSelector(
                        selectedTaxCode = uiState.selectedTaxCode,
                        workspaceCodes = workspaceCodes,
                        onTaxCodeSelected = { viewModel.onTaxCodeSelected(it) }
                    )

                    // Base Amount
                    OutlinedTextField(
                        value = uiState.baseAmount,
                        onValueChange = { viewModel.onBaseAmountChange(it) },
                        label = { Text("Base Amount") },
                        placeholder = { Text("Enter amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quantity
                    OutlinedTextField(
                        value = uiState.quantity,
                        onValueChange = { viewModel.onQuantityChange(it) },
                        label = { Text("Quantity") },
                        placeholder = { Text("Enter quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Transaction Type
                    TransactionTypeSelector(
                        selectedType = uiState.transactionType,
                        onTypeSelected = { viewModel.onTransactionTypeChange(it) }
                    )

                    // Source Location (Seller)
                    LocationInput(
                        title = "Source Location (Seller)",
                        state = uiState.sourceState,
                        country = uiState.sourceCountry,
                        onStateChange = { viewModel.onSourceStateChange(it) },
                        onCountryChange = { viewModel.onSourceCountryChange(it) }
                    )

                    // Destination Location (Buyer)
                    LocationInput(
                        title = "Destination Location (Buyer)",
                        state = uiState.destinationState,
                        country = uiState.destinationCountry,
                        onStateChange = { viewModel.onDestinationStateChange(it) },
                        onCountryChange = { viewModel.onDestinationCountryChange(it) }
                    )

                    // Calculate Button
                    Button(
                        onClick = { viewModel.calculateTax() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isCalculating && uiState.isValid
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isCalculating) "Calculating..." else "Calculate Tax")
                    }

                    // Error Message
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Clear Button
                    if (uiState.calculationResult != null) {
                        OutlinedButton(
                            onClick = { viewModel.clearCalculation() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear")
                        }
                    }
                }
            }

            // Result Card
            if (uiState.calculationResult != null) {
                TaxCalculationResultCard(
                    result = uiState.calculationResult!!,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxCodeSelector(
    selectedTaxCode: TaxCode?,
    workspaceCodes: List<TaxCode>,
    onTaxCodeSelected: (TaxCode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Tax Code",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedTaxCode?.let { "${it.code} - ${it.description}" } ?: "Select tax code",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (workspaceCodes.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No tax codes available") },
                        onClick = { expanded = false },
                        enabled = false
                    )
                } else {
                    workspaceCodes.forEach { code ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = code.code,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = code.shortDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onTaxCodeSelected(code)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Transaction Type",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedType.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                TransactionType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = {
                            onTypeSelected(type)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationInput(
    title: String,
    state: String,
    country: String,
    onStateChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state,
                onValueChange = onStateChange,
                label = { Text("State") },
                placeholder = { Text("MH, CA, etc.") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = country,
                onValueChange = onCountryChange,
                label = { Text("Country") },
                placeholder = { Text("IN, US, etc.") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TaxCalculationResultCard(
    result: TaxCalculationResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tax Calculation Result",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Divider()

            // Tax Code Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tax Code:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = result.taxCode,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Base Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Base Amount:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${result.baseAmount.formatDecimal()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Divider()

            // Tax Components Breakdown
            Text(
                text = "Tax Components",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            result.taxComponents.forEach { component ->
                TaxComponentRow(component = component)
            }

            Divider()

            // Total Tax
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Tax:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "₹${result.totalTaxAmount.formatDecimal()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Grand Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Grand Total:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "₹${result.totalAmount.formatDecimal()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TaxComponentRow(
    component: TaxComponentResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = component.componentName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${component.ratePercentage}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "on ₹${component.taxableAmount.formatDecimal()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${component.taxAmount.formatDecimal()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
