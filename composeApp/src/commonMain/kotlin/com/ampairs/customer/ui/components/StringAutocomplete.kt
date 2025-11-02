package com.ampairs.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StringAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    label: String = "Field",
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }
    val focusManager = LocalFocusManager.current

    // Filter suggestions based on input
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) {
            suggestions.take(10) // Show top 10 suggestions when empty
        } else {
            suggestions.filter {
                it.contains(value, ignoreCase = true)
            }.take(10)
        }
    }

    // Reset selected index when filtered suggestions change
    LaunchedEffect(filteredSuggestions) {
        selectedIndex = -1
    }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && filteredSuggestions.isNotEmpty() && hasFocus,
            onExpandedChange = { newExpanded ->
                expanded = newExpanded
                if (!newExpanded) {
                    focusManager.clearFocus()
                }
            }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                    expanded = true
                },
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                    .onFocusChanged { focusState ->
                        hasFocus = focusState.isFocused
                        if (focusState.isFocused) {
                            expanded = true
                        } else if (!focusState.isFocused) {
                            expanded = false
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && expanded && filteredSuggestions.isNotEmpty()) {
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    selectedIndex = (selectedIndex + 1).coerceAtMost(filteredSuggestions.size - 1)
                                    true
                                }
                                Key.DirectionUp -> {
                                    selectedIndex = (selectedIndex - 1).coerceAtLeast(-1)
                                    true
                                }
                                Key.Enter -> {
                                    if (selectedIndex >= 0 && selectedIndex < filteredSuggestions.size) {
                                        val selectedSuggestion = filteredSuggestions[selectedIndex]
                                        onValueChange(selectedSuggestion)
                                        expanded = false
                                        selectedIndex = -1
                                        focusManager.moveFocus(FocusDirection.Next)
                                    }
                                    true
                                }
                                Key.Escape -> {
                                    expanded = false
                                    selectedIndex = -1
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (expanded && filteredSuggestions.isNotEmpty()) {
                            expanded = false
                        }
                        focusManager.moveFocus(FocusDirection.Next)
                    },
                    onDone = {
                        expanded = false
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                isError = isError,
                supportingText = supportingText
            )

            ExposedDropdownMenu(
                expanded = expanded && filteredSuggestions.isNotEmpty() && hasFocus,
                onDismissRequest = {
                    expanded = false
                    selectedIndex = -1
                }
            ) {
                filteredSuggestions.forEachIndexed { index, suggestion ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onValueChange(suggestion)
                            expanded = false
                            selectedIndex = -1
                            focusManager.moveFocus(FocusDirection.Next)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (index == selectedIndex)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            )
                    )
                }
            }
        }
    }
}