package com.ampairs.tax.ui.configuration

import androidx.compose.foundation.layout.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Tax Module Configuration Screen
 *
 * Manages workspace tax settings:
 * - Country selection
 * - Tax strategy
 * - Default tax code system
 * - Industry settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxConfigurationScreen(
    onNavigateBack: () -> Unit,
    viewModel: TaxConfigurationViewModel = metroViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Tax configuration saved successfully")
            viewModel.clearSaveSuccess()
        }
    }

    // Handle save error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tax Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Settings, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isLoading) {
                        IconButton(
                            onClick = { viewModel.saveConfiguration() },
                            enabled = !uiState.isSaving
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Configuration Status
                    if (uiState.isNewConfiguration) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Setup Required",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Tax configuration is required to use tax features. Please configure your tax settings below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    val formState = uiState.formState

                    // Basic Settings Section
                    SectionHeader("Basic Settings")

                    // Country Selection
                    CountryDropdown(
                        selectedCountry = formState.countryCode,
                        onCountrySelected = {
                            viewModel.updateFormState(formState.copy(countryCode = it))
                        },
                        enabled = !uiState.isSaving
                    )

                    // Tax Strategy Selection
                    TaxStrategyDropdown(
                        selectedStrategy = formState.taxStrategy,
                        countryCode = formState.countryCode,
                        onStrategySelected = {
                            viewModel.updateFormState(formState.copy(taxStrategy = it))
                        },
                        enabled = !uiState.isSaving
                    )

                    // Default Tax Code System
                    TaxCodeSystemDropdown(
                        selectedSystem = formState.defaultTaxCodeSystem,
                        countryCode = formState.countryCode,
                        onSystemSelected = {
                            viewModel.updateFormState(formState.copy(defaultTaxCodeSystem = it))
                        },
                        enabled = !uiState.isSaving
                    )

                    // Optional Settings Section
                    SectionHeader("Optional Settings")

                    // Industry Selection
                    OutlinedTextField(
                        value = formState.industry ?: "",
                        onValueChange = {
                            viewModel.updateFormState(formState.copy(industry = it.takeIf { it.isNotBlank() }))
                        },
                        label = { Text("Industry") },
                        placeholder = { Text("e.g., Retail, Manufacturing, Services") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isSaving,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    // Auto Subscribe Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-subscribe to new tax codes",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Automatically subscribe to newly added tax codes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = formState.autoSubscribeNewCodes,
                            onCheckedChange = {
                                viewModel.updateFormState(formState.copy(autoSubscribeNewCodes = it))
                            },
                            enabled = !uiState.isSaving
                        )
                    }

                    // Save Button
                    Button(
                        onClick = { viewModel.saveConfiguration() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && formState.isValid()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (uiState.isNewConfiguration) "Create Configuration" else "Save Changes")
                    }

                    // Add bottom spacing
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(
    selectedCountry: String,
    onCountrySelected: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val countries = listOf(
        "IN" to "India",
        "US" to "United States",
        "GB" to "United Kingdom",
        "AU" to "Australia",
        "CA" to "Canada",
        "SG" to "Singapore",
        "AE" to "United Arab Emirates"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = countries.find { it.first == selectedCountry }?.second ?: selectedCountry,
            onValueChange = {},
            readOnly = true,
            label = { Text("Country *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            countries.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onCountrySelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxStrategyDropdown(
    selectedStrategy: String,
    countryCode: String,
    onStrategySelected: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val strategies = remember(countryCode) {
        when (countryCode) {
            "IN" -> listOf("INDIA_GST" to "India GST")
            "US" -> listOf("US_SALES_TAX" to "US Sales Tax")
            "GB" -> listOf("UK_VAT" to "UK VAT")
            "AU" -> listOf("AUSTRALIA_GST" to "Australia GST")
            "CA" -> listOf("CANADA_GST_HST" to "Canada GST/HST")
            "SG" -> listOf("SINGAPORE_GST" to "Singapore GST")
            "AE" -> listOf("UAE_VAT" to "UAE VAT")
            else -> listOf("STANDARD" to "Standard Tax")
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = strategies.find { it.first == selectedStrategy }?.second ?: selectedStrategy,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tax Strategy *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            strategies.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onStrategySelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxCodeSystemDropdown(
    selectedSystem: String,
    countryCode: String,
    onSystemSelected: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val systems = remember(countryCode) {
        when (countryCode) {
            "IN" -> listOf(
                "HSN_CODE" to "HSN Code (Goods)",
                "SAC_CODE" to "SAC Code (Services)"
            )
            "US" -> listOf("NAICS" to "NAICS", "SIC" to "SIC")
            "GB" -> listOf("COMMODITY_CODE" to "Commodity Code")
            else -> listOf("STANDARD" to "Standard Code System")
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = systems.find { it.first == selectedSystem }?.second ?: selectedSystem,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tax Code System *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            systems.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSystemSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}
