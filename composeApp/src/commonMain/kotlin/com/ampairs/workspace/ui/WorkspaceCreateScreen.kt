package com.ampairs.workspace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.ampairs.workspace.viewmodel.WorkspaceCreateViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceCreateScreen(
    onNavigateBack: () -> Unit,
    onWorkspaceCreated: (String) -> Unit,
    workspaceId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: WorkspaceCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val isEditMode = workspaceId != null

    // Initialize workspace data for edit mode
    LaunchedEffect(workspaceId) {
        workspaceId?.let { id ->
            viewModel.loadWorkspaceForEdit(id)
        }
    }

    // Handle successful workspace creation/update
    LaunchedEffect(state.createdWorkspaceId) {
        state.createdWorkspaceId?.let { id ->
            onWorkspaceCreated(id)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = if (isEditMode) "Edit workspace" else "Create a new workspace",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isEditMode) 
                "Update your workspace information. Changes will be saved automatically."
            else 
                "Set up your workspace with basic information. You can always change these settings later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error message
        state.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }

        // Workspace Name
        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Workspace Name *") },
            placeholder = { Text("My Company") },
            isError = state.validationErrors.containsKey("name"),
            supportingText = {
                state.validationErrors["name"]?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Workspace Slug
        OutlinedTextField(
            value = state.slug,
            onValueChange = { viewModel.updateSlug(it) },
            label = { Text("Workspace Slug *") },
            placeholder = { Text("my-company") },
            isError = state.validationErrors.containsKey("slug"),
            supportingText = {
                state.validationErrors["slug"]?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                } ?: Text(
                    text = "This will be used in your workspace URL: workspace/${state.slug}",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            trailingIcon = {
                when {
                    state.isSlugChecking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    state.slug.length >= 2 && state.isSlugAvailable && !state.validationErrors.containsKey(
                        "slug"
                    ) -> {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Available",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    state.slug.length >= 2 && !state.isSlugAvailable -> {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Not available",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Description
        OutlinedTextField(
            value = state.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Description (optional)") },
            placeholder = { Text("Brief description of your workspace...") },
            isError = state.validationErrors.containsKey("description"),
            supportingText = {
                state.validationErrors["description"]?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        // Workspace Type
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = state.workspaceType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Workspace Type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("BUSINESS", "PERSONAL", "TEAM", "ORGANIZATION").forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            viewModel.updateWorkspaceType(type)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create/Update Button
        Button(
            onClick = { 
                if (isEditMode) {
                    viewModel.updateWorkspace()
                } else {
                    viewModel.createWorkspace()
                }
            },
            enabled = !state.isLoading && state.name.isNotEmpty() && state.slug.isNotEmpty() && state.isSlugAvailable,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "Updating..." else "Creating...")
            } else {
                Text(if (isEditMode) "Update Workspace" else "Create Workspace")
            }
        }

        // Cancel Button
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            Text("Cancel")
        }

        // Delete Button (only in edit mode)
        if (isEditMode) {
            OutlinedButton(
                onClick = { viewModel.showDeleteDialog() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && !state.isDeleting,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Workspace")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Delete Confirmation Dialog
    if (state.showDeleteDialog) {
        WorkspaceDeleteConfirmationDialog(
            workspaceName = state.name,
            workspaceSlug = state.slug,
            confirmationSlug = state.deleteConfirmationSlug,
            onConfirmationSlugChange = viewModel::updateDeleteConfirmationSlug,
            isDeleting = state.isDeleting,
            deleteError = state.deleteError,
            isConfirmationValid = viewModel.isDeleteConfirmationValid,
            onConfirm = {
                viewModel.archiveWorkspace {
                    onNavigateBack()
                }
            },
            onDismiss = viewModel::hideDeleteDialog
        )
    }
}

@Composable
private fun WorkspaceDeleteConfirmationDialog(
    workspaceName: String,
    workspaceSlug: String,
    confirmationSlug: String,
    onConfirmationSlugChange: (String) -> Unit,
    isDeleting: Boolean,
    deleteError: String?,
    isConfirmationValid: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Delete Workspace?")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "You are about to archive the workspace \"$workspaceName\". " +
                            "This action will mark it as archived and it can be restored later.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "To confirm, please type the workspace slug:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = workspaceSlug,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = confirmationSlug,
                    onValueChange = onConfirmationSlugChange,
                    label = { Text("Type workspace slug") },
                    singleLine = true,
                    enabled = !isDeleting,
                    isError = confirmationSlug.isNotEmpty() && !isConfirmationValid,
                    supportingText = {
                        if (confirmationSlug.isNotEmpty() && !isConfirmationValid) {
                            Text("Slug doesn't match", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                deleteError?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isConfirmationValid && !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel")
            }
        }
    )
}