package com.ampairs.business.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

/**
 * Business Operations Settings Screen.
 *
 * Manages:
 * - Timezone configuration
 * - Currency settings
 * - Language preference
 * - Date/time formats
 * - Business hours
 * - Operating days
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessOperationsScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessOperationsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Operations settings saved successfully")
            viewModel.clearSaveSuccess()
        }
    }

    // Handle refresh state
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    isRefreshing = true
                    viewModel.refresh()
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Operational Settings",
                    style = MaterialTheme.typography.headlineMedium
                )

                when {
                    uiState.isLoading && uiState.operations == null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.error != null -> {
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
                        val formState = uiState.formState

                        // Regional Settings Section
                        SectionHeader("Regional Settings")

                        TimezoneDropdown(
                            selectedTimezone = formState.timezone,
                            onTimezoneSelected = { viewModel.updateFormState(formState.copy(timezone = it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        CurrencyDropdown(
                            selectedCurrency = formState.currency,
                            onCurrencySelected = { viewModel.updateFormState(formState.copy(currency = it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        LanguageDropdown(
                            selectedLanguage = formState.language,
                            onLanguageSelected = { viewModel.updateFormState(formState.copy(language = it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Format Settings Section
                        SectionHeader("Format Settings")

                        DateFormatDropdown(
                            selectedFormat = formState.dateFormat,
                            onFormatSelected = { viewModel.updateFormState(formState.copy(dateFormat = it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TimeFormatDropdown(
                            selectedFormat = formState.timeFormat,
                            onFormatSelected = { viewModel.updateFormState(formState.copy(timeFormat = it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Business Hours Section
                        SectionHeader("Business Hours")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = formState.openingHours,
                                onValueChange = { viewModel.updateFormState(formState.copy(openingHours = it)) },
                                label = { Text("Opening Time") },
                                placeholder = { Text("09:00") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = formState.closingHours,
                                onValueChange = { viewModel.updateFormState(formState.copy(closingHours = it)) },
                                label = { Text("Closing Time") },
                                placeholder = { Text("18:00") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // Operating Days Section
                        SectionHeader("Operating Days")

                        OperatingDaysSelector(
                            selectedDays = formState.selectedDays,
                            onDaysChanged = { viewModel.updateFormState(formState.copy(selectedDays = it)) }
                        )

                        // Save Button
                        Button(
                            onClick = { viewModel.saveOperations() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save Settings")
                            }
                        }

                        // Add bottom spacing
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezoneDropdown(
    selectedTimezone: String,
    onTimezoneSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val timezones = listOf(
        "UTC",
        "America/New_York",
        "America/Chicago",
        "America/Los_Angeles",
        "Europe/London",
        "Europe/Paris",
        "Asia/Dubai",
        "Asia/Kolkata",
        "Asia/Singapore",
        "Asia/Tokyo",
        "Australia/Sydney"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedTimezone,
            onValueChange = {},
            readOnly = true,
            label = { Text("Timezone") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            timezones.forEach { timezone ->
                DropdownMenuItem(
                    text = { Text(timezone) },
                    onClick = {
                        onTimezoneSelected(timezone)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = listOf(
        "INR" to "Indian Rupee (₹)",
        "USD" to "US Dollar ($)",
        "EUR" to "Euro (€)",
        "GBP" to "British Pound (£)",
        "AUD" to "Australian Dollar (A$)",
        "CAD" to "Canadian Dollar (C$)",
        "SGD" to "Singapore Dollar (S$)",
        "AED" to "UAE Dirham (د.إ)"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = currencies.find { it.first == selectedCurrency }?.second ?: selectedCurrency,
            onValueChange = {},
            readOnly = true,
            label = { Text("Currency") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currencies.forEach { (code, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        onCurrencySelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf(
        "en" to "English",
        "hi" to "Hindi",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "ja" to "Japanese",
        "zh" to "Chinese"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = languages.find { it.first == selectedLanguage }?.second ?: selectedLanguage,
            onValueChange = {},
            readOnly = true,
            label = { Text("Language") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onLanguageSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFormatDropdown(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val formats = listOf("DD-MM-YYYY", "MM-DD-YYYY", "YYYY-MM-DD")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedFormat,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date Format") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format) },
                    onClick = {
                        onFormatSelected(format)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeFormatDropdown(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val formats = listOf("12H", "24H")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedFormat,
            onValueChange = {},
            readOnly = true,
            label = { Text("Time Format") },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format) },
                    onClick = {
                        onFormatSelected(format)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OperatingDaysSelector(
    selectedDays: List<String>,
    onDaysChanged: (List<String>) -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(day, style = MaterialTheme.typography.bodyLarge)
                Checkbox(
                    checked = selectedDays.contains(day),
                    onCheckedChange = { checked ->
                        val newDays = if (checked) {
                            selectedDays + day
                        } else {
                            selectedDays - day
                        }
                        onDaysChanged(newDays)
                    }
                )
            }
        }
    }
}
