package com.ampairs.unit.ui.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitFormScreen(
    unitId: String? = null,
    onSaveSuccess: () -> kotlin.Unit,
    modifier: Modifier = Modifier,
    viewModel: UnitFormViewModel = assistedMetroViewModel<UnitFormViewModel, UnitFormViewModel.Factory> { create(unitId) }
) {
    val formState by viewModel.formState.collectAsState()
    val focusManager = LocalFocusManager.current

    val isEditing = unitId != null

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (isEditing) "Edit Unit" else "New Unit") },
            actions = {
                TextButton(
                    onClick = {
                        viewModel.saveUnit(onSaveSuccess)
                    },
                    enabled = !formState.isLoading &&
                             formState.name.isNotBlank() &&
                             formState.shortName.isNotBlank()
                ) {
                    if (formState.isLoading) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show error if present
            formState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Basic Information Section
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Basic Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Name Field (Required)
                    OutlinedTextField(
                        value = formState.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Unit Name *") },
                        placeholder = { Text("e.g., Kilogram, Liter, Piece") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = formState.error != null && formState.name.isBlank()
                    )

                    // Short Name Field (Required)
                    OutlinedTextField(
                        value = formState.shortName,
                        onValueChange = viewModel::updateShortName,
                        label = { Text("Short Name/Symbol *") },
                        placeholder = { Text("e.g., kg, L, pcs") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        isError = formState.error != null && formState.shortName.isBlank()
                    )

                    // Decimal Places Field (Required)
                    OutlinedTextField(
                        value = formState.decimalPlaces,
                        onValueChange = viewModel::updateDecimalPlaces,
                        label = { Text("Decimal Places *") },
                        placeholder = { Text("e.g., 0, 2, 3") },
                        supportingText = { Text("Number of decimal places for this unit") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                }
            }

            // Additional Information Section
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Additional Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Description Field
                    OutlinedTextField(
                        value = formState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("Description") },
                        placeholder = { Text("Optional description for this unit") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Category Field
                    OutlinedTextField(
                        value = formState.category,
                        onValueChange = viewModel::updateCategory,
                        label = { Text("Category") },
                        placeholder = { Text("e.g., Weight, Volume, Length, Count") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    // Active Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Status",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Enable this unit for use in the system",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = formState.active,
                            onCheckedChange = viewModel::updateActive
                        )
                    }
                }
            }

            // Bottom Save Button
            Button(
                onClick = {
                    viewModel.saveUnit(onSaveSuccess)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = !formState.isLoading &&
                         formState.name.isNotBlank() &&
                         formState.shortName.isNotBlank()
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text(if (isEditing) "Update Unit" else "Create Unit")
                }
            }

            // Bottom padding
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
