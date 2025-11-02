package com.ampairs.tax.ui.hsn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.tax.domain.HsnCategory
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HsnCodeFormScreen(
    hsnCodeId: String? = null,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HsnCodeFormViewModel = koinViewModel { parametersOf(hsnCodeId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val isEditing = hsnCodeId != null

    // Show error as snackbar
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            // Error will be shown in UI
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (isEditing) "Edit HSN Code" else "New HSN Code") },
            actions = {
                TextButton(
                    onClick = {
                        viewModel.saveHsnCode(onSaveSuccess)
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
                    placeholder = { Text("Enter 4-8 digit code") },
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

                // Description Field
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    placeholder = { Text("Enter product description") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Chapter Field
                OutlinedTextField(
                    value = uiState.chapter,
                    onValueChange = { viewModel.updateChapter(it) },
                    label = { Text("Chapter") },
                    placeholder = { Text("Enter chapter number") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Heading Field
                OutlinedTextField(
                    value = uiState.heading,
                    onValueChange = { viewModel.updateHeading(it) },
                    label = { Text("Heading") },
                    placeholder = { Text("Enter heading") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = getCategoryDisplayName(uiState.category),
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        HsnCategory.entries.forEach { categoryOption ->
                            DropdownMenuItem(
                                text = { Text(getCategoryDisplayName(categoryOption)) },
                                onClick = {
                                    viewModel.updateCategory(categoryOption)
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

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

                // Preview formatted code
                if (uiState.hsnCode.isNotBlank()) {
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
                                text = "Formatted Code Preview",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.formattedHsnCode,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
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
                            viewModel.saveHsnCode(onSaveSuccess)
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
                            Text("Save HSN Code")
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryDisplayName(category: HsnCategory): String {
    return when (category) {
        HsnCategory.GENERAL -> "General"
        HsnCategory.AGRICULTURE -> "Agriculture"
        HsnCategory.TEXTILES -> "Textiles"
        HsnCategory.CHEMICALS -> "Chemicals"
        HsnCategory.MACHINERY -> "Machinery"
        HsnCategory.ELECTRONICS -> "Electronics"
        HsnCategory.VEHICLES -> "Vehicles"
        HsnCategory.PRECIOUS_METALS -> "Precious Metals"
        HsnCategory.FOOD_BEVERAGES -> "Food & Beverages"
        HsnCategory.TOBACCO -> "Tobacco"
        HsnCategory.CONSTRUCTION -> "Construction"
        HsnCategory.HEALTHCARE -> "Healthcare"
    }
}

