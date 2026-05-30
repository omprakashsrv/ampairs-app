package com.ampairs.customer.ui.state

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.customer.domain.State
import com.ampairs.customer.domain.MasterState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_cancel
import ampairsapp.feature.customer.generated.resources.customer_delete
import ampairsapp.feature.customer.generated.resources.customer_state_list_title
import ampairsapp.feature.customer.generated.resources.customer_state_import_cd
import ampairsapp.feature.customer.generated.resources.customer_state_search_label
import ampairsapp.feature.customer.generated.resources.customer_state_search_empty
import ampairsapp.feature.customer.generated.resources.customer_state_list_empty
import ampairsapp.feature.customer.generated.resources.customer_state_import_btn
import ampairsapp.feature.customer.generated.resources.customer_state_delete_title
import ampairsapp.feature.customer.generated.resources.customer_state_delete_confirm
import ampairsapp.feature.customer.generated.resources.customer_state_import_dialog_title
import ampairsapp.feature.customer.generated.resources.customer_state_import_empty
import ampairsapp.feature.customer.generated.resources.customer_state_import_selected
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateListScreen(
    onStateClick: (String) -> Unit,
    onImportStates: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StateListViewModel = metroViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var showImportDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.customer_state_list_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FloatingActionButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(Res.string.customer_state_import_cd)
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    label = { Text(stringResource(Res.string.customer_state_search_label)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    singleLine = true
                )
            }
        }

        uiState.error?.let { error ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            uiState.isLoading && uiState.states.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.states.isEmpty() && !uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) stringResource(Res.string.customer_state_search_empty)
                                   else stringResource(Res.string.customer_state_list_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Button(onClick = { showImportDialog = true }) {
                                Text(stringResource(Res.string.customer_state_import_btn))
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = uiState.states,
                        key = { it.id }
                    ) { state ->
                        StateListItem(
                            state = state,
                            onStateClick = { onStateClick(state.id) },
                            onDeleteClick = { viewModel.deleteState(state.id) }
                        )
                    }

                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        StateImportDialog(
            uiState = uiState,
            onDismiss = { showImportDialog = false },
            onImportState = { stateCode ->
                viewModel.importState(stateCode)
                showImportDialog = false
            },
            onBulkImport = { stateCodes ->
                viewModel.bulkImportStates(stateCodes)
                showImportDialog = false
            },
            onLoadAvailableStates = viewModel::loadAvailableStatesForImport
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateListItem(
    state: State,
    onStateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onStateClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.customer_state_delete_title),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.customer_state_delete_title)) },
            text = { Text(stringResource(Res.string.customer_state_delete_confirm, state.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text(stringResource(Res.string.customer_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.customer_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateImportDialog(
    uiState: StateListUiState,
    onDismiss: () -> Unit,
    onImportState: (String) -> Unit,
    onBulkImport: (List<String>) -> Unit,
    onLoadAvailableStates: () -> Unit
) {
    var selectedStates by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        onLoadAvailableStates()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.customer_state_import_dialog_title)) },
        text = {
            when {
                uiState.isLoadingImportStates -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.availableStatesForImport.isEmpty() && !uiState.isLoadingImportStates -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.customer_state_import_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.availableStatesForImport) { masterState ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedStates.contains(masterState.stateCode),
                                    onCheckedChange = { isChecked ->
                                        selectedStates = if (isChecked) {
                                            selectedStates + masterState.stateCode
                                        } else {
                                            selectedStates - masterState.stateCode
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = masterState.getDisplayName(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        if (selectedStates.isNotEmpty()) {
                            onBulkImport(selectedStates.toList())
                        }
                    },
                    enabled = selectedStates.isNotEmpty() && !uiState.isLoadingImportStates
                ) {
                    Text(stringResource(Res.string.customer_state_import_selected, selectedStates.size))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.customer_cancel))
            }
        }
    )
}
