package com.ampairs.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.PopupProperties
import com.ampairs.customer.domain.State
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun StateAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    onStateSelected: (State) -> Unit,
    states: List<State>,
    modifier: Modifier = Modifier,
    label: String = "State",
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }
    val focusManager = LocalFocusManager.current

    // Filter states based on input
    val filteredStates = remember(value, states) {
        if (value.isBlank()) {
            states.take(10) // Show top 10 states when empty
        } else {
            states.filter {
                it.name.contains(value, ignoreCase = true)
            }.take(10)
        }
    }

    // Reset selected index when filtered states change
    LaunchedEffect(filteredStates) {
        selectedIndex = -1
    }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && filteredStates.isNotEmpty() && hasFocus,
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
                        if (keyEvent.type == KeyEventType.KeyDown && expanded && filteredStates.isNotEmpty()) {
                            when (keyEvent.key) {
                                Key.DirectionDown -> {
                                    selectedIndex = (selectedIndex + 1).coerceAtMost(filteredStates.size - 1)
                                    true
                                }
                                Key.DirectionUp -> {
                                    selectedIndex = (selectedIndex - 1).coerceAtLeast(-1)
                                    true
                                }
                                Key.Enter -> {
                                    if (selectedIndex >= 0 && selectedIndex < filteredStates.size) {
                                        val selectedState = filteredStates[selectedIndex]
                                        onValueChange(selectedState.name)
                                        onStateSelected(selectedState)
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
                        if (expanded && filteredStates.isNotEmpty()) {
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
                expanded = expanded && filteredStates.isNotEmpty() && hasFocus,
                onDismissRequest = {
                    expanded = false
                    selectedIndex = -1
                }
            ) {
                filteredStates.forEachIndexed { index, state ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = state.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onValueChange(state.name)
                            onStateSelected(state)
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

@Composable
fun StateAutocompletePreview() {
    val sampleStates = listOf(
        State(id = "1", name = "Andhra Pradesh"),
        State(id = "2", name = "Karnataka"),
        State(id = "3", name = "Kerala"),
        State(id = "4", name = "Tamil Nadu")
    )

    var selectedState by remember { mutableStateOf("") }

    StateAutocomplete(
        value = selectedState,
        onValueChange = { selectedState = it },
        onStateSelected = { state -> selectedState = state.name },
        states = sampleStates,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    )
}