package com.ampairs.business.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.business.ui.components.BusinessFormSection
import com.ampairs.business.ui.components.BusinessSaveBar
import com.ampairs.business.ui.components.BusinessScreenContent
import dev.zacsweers.metrox.viewmodel.metroViewModel

private val WEEK_DAYS = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

/**
 * Business Operations — localisation + a business-hours editor, in grouped
 * Material 3 section cards with a dirty-state save bar (Ampairs design handoff).
 *
 * The backend models uniform hours (one open/close applied to a set of operating
 * days), so the editor offers a week-strip preview, a shared open/close time, and
 * per-day open/closed switches that drive the operating-days set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessOperationsScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessOperationsViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Operations settings saved successfully")
            viewModel.clearSaveSuccess()
        }
    }

    val fs = uiState.formState
    val ops = uiState.operations
    val dirty = ops != null && (
        fs.timezone != ops.timezone ||
            fs.currency != ops.currency ||
            fs.language != ops.language ||
            fs.dateFormat != ops.dateFormat ||
            fs.timeFormat != ops.timeFormat ||
            fs.openingHours != (ops.openingHours ?: "") ||
            fs.closingHours != (ops.closingHours ?: "") ||
            fs.selectedDays != ops.operatingDays
        )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (dirty) {
                BusinessSaveBar(
                    saving = uiState.isSaving,
                    enabled = true,
                    onDiscard = {
                        ops?.let { o ->
                            viewModel.updateFormState(
                                BusinessOperationsFormState(
                                    timezone = o.timezone,
                                    currency = o.currency,
                                    language = o.language,
                                    dateFormat = o.dateFormat,
                                    timeFormat = o.timeFormat,
                                    openingHours = o.openingHours ?: "",
                                    closingHours = o.closingHours ?: "",
                                    selectedDays = o.operatingDays,
                                )
                            )
                        }
                    },
                    onSave = { viewModel.saveOperations() },
                )
            }
        }
    ) { paddingValues ->
        BusinessScreenContent(
            modifier = Modifier.padding(paddingValues),
            maxContentWidth = 720.dp,
        ) {
            when {
                uiState.isLoading && uiState.operations == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null && uiState.operations == null -> {
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
                    BusinessFormSection(
                        icon = Icons.Filled.Language,
                        title = "Localisation",
                        subtitle = "Used for currency, dates and tax formatting",
                    ) {
                        CurrencyDropdown(
                            selectedCurrency = fs.currency,
                            onCurrencySelected = { viewModel.updateFormState(fs.copy(currency = it)) },
                        )
                        TimezoneDropdown(
                            selectedTimezone = fs.timezone,
                            onTimezoneSelected = { viewModel.updateFormState(fs.copy(timezone = it)) },
                        )
                        LanguageDropdown(
                            selectedLanguage = fs.language,
                            onLanguageSelected = { viewModel.updateFormState(fs.copy(language = it)) },
                        )
                        DateFormatDropdown(
                            selectedFormat = fs.dateFormat,
                            onFormatSelected = { viewModel.updateFormState(fs.copy(dateFormat = it)) },
                        )
                        TimeFormatDropdown(
                            selectedFormat = fs.timeFormat,
                            onFormatSelected = { viewModel.updateFormState(fs.copy(timeFormat = it)) },
                        )
                    }

                    BusinessFormSection(
                        icon = Icons.Filled.CalendarToday,
                        title = "Business Hours",
                        subtitle = "Shown to customers and used on order timestamps",
                    ) {
                        HoursEditor(
                            openingHours = fs.openingHours,
                            closingHours = fs.closingHours,
                            openDays = fs.selectedDays,
                            onOpeningChange = { viewModel.updateFormState(fs.copy(openingHours = it)) },
                            onClosingChange = { viewModel.updateFormState(fs.copy(closingHours = it)) },
                            onToggleDay = { day, open ->
                                val next = if (open) fs.selectedDays + day else fs.selectedDays - day
                                viewModel.updateFormState(fs.copy(selectedDays = next))
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/* ── Business-hours editor ─────────────────────────────────────────────────── */

@Composable
private fun HoursEditor(
    openingHours: String,
    closingHours: String,
    openDays: List<String>,
    onOpeningChange: (String) -> Unit,
    onClosingChange: (String) -> Unit,
    onToggleDay: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WeekStrip(openDays = openDays, openingHours = openingHours)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = openingHours,
                onValueChange = onOpeningChange,
                label = { Text("Opens") },
                placeholder = { Text("09:00") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = closingHours,
                onValueChange = onClosingChange,
                label = { Text("Closes") },
                placeholder = { Text("18:00") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }

        Text(
            text = "OPEN ON",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WEEK_DAYS.forEach { day ->
            val open = openDays.contains(day)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Switch(checked = open, onCheckedChange = { onToggleDay(day, it) })
                Text(
                    text = day,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (open) {
                        val from = openingHours.ifBlank { "—" }
                        val to = closingHours.ifBlank { "—" }
                        "$from – $to"
                    } else "Closed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(openDays: List<String>, openingHours: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        WEEK_DAYS.forEach { day ->
            val open = openDays.contains(day)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (open) cs.primaryContainer else cs.surfaceContainerHigh)
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = day.take(3),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (open) cs.onPrimaryContainer else cs.onSurfaceVariant,
                )
                Text(
                    text = if (open) openingHours.ifBlank { "—" } else "Closed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (open) cs.onPrimaryContainer else cs.onSurfaceVariant,
                )
            }
        }
    }
}

/* ── Dropdowns (unchanged behaviour, restyled inside section cards) ─────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezoneDropdown(
    selectedTimezone: String,
    onTimezoneSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val timezones = listOf(
        "UTC", "America/New_York", "America/Chicago", "America/Los_Angeles",
        "Europe/London", "Europe/Paris", "Asia/Dubai", "Asia/Kolkata",
        "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney"
    )
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedTimezone,
            onValueChange = {},
            readOnly = true,
            label = { Text("Time zone") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            timezones.forEach { timezone ->
                DropdownMenuItem(text = { Text(timezone) }, onClick = { onTimezoneSelected(timezone); expanded = false })
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
        "INR" to "Indian Rupee (₹)", "USD" to "US Dollar ($)", "EUR" to "Euro (€)",
        "GBP" to "British Pound (£)", "AUD" to "Australian Dollar (A$)",
        "CAD" to "Canadian Dollar (C$)", "SGD" to "Singapore Dollar (S$)", "AED" to "UAE Dirham (د.إ)"
    )
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = currencies.find { it.first == selectedCurrency }?.second ?: selectedCurrency,
            onValueChange = {},
            readOnly = true,
            label = { Text("Currency") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            currencies.forEach { (code, display) ->
                DropdownMenuItem(text = { Text(display) }, onClick = { onCurrencySelected(code); expanded = false })
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
        "en" to "English", "hi" to "Hindi", "es" to "Spanish", "fr" to "French",
        "de" to "German", "ja" to "Japanese", "zh" to "Chinese"
    )
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = languages.find { it.first == selectedLanguage }?.second ?: selectedLanguage,
            onValueChange = {},
            readOnly = true,
            label = { Text("Language") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onLanguageSelected(code); expanded = false })
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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedFormat,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date format") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            formats.forEach { format ->
                DropdownMenuItem(text = { Text(format) }, onClick = { onFormatSelected(format); expanded = false })
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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedFormat,
            onValueChange = {},
            readOnly = true,
            label = { Text("Time format") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            formats.forEach { format ->
                DropdownMenuItem(text = { Text(format) }, onClick = { onFormatSelected(format); expanded = false })
            }
        }
    }
}
