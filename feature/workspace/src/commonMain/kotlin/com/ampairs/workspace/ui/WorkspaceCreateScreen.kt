package com.ampairs.workspace.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.workspace.viewmodel.WorkspaceCreateEvent
import com.ampairs.workspace.viewmodel.WorkspaceCreateViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlin.random.Random
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.workspace.generated.resources.Res
import ampairsapp.feature.workspace.generated.resources.workspace_create_title
import ampairsapp.feature.workspace.generated.resources.workspace_edit_title
import ampairsapp.feature.workspace.generated.resources.workspace_create_subtitle
import ampairsapp.feature.workspace.generated.resources.workspace_edit_subtitle
import ampairsapp.feature.workspace.generated.resources.workspace_dismiss
import ampairsapp.feature.workspace.generated.resources.workspace_name_label
import ampairsapp.feature.workspace.generated.resources.workspace_name_placeholder
import ampairsapp.feature.workspace.generated.resources.workspace_slug_label
import ampairsapp.feature.workspace.generated.resources.workspace_slug_placeholder
import ampairsapp.feature.workspace.generated.resources.workspace_slug_url_hint
import ampairsapp.feature.workspace.generated.resources.workspace_description_label
import ampairsapp.feature.workspace.generated.resources.workspace_description_placeholder
import ampairsapp.feature.workspace.generated.resources.workspace_type_label
import ampairsapp.feature.workspace.generated.resources.workspace_creating
import ampairsapp.feature.workspace.generated.resources.workspace_updating
import ampairsapp.feature.workspace.generated.resources.workspace_create_button
import ampairsapp.feature.workspace.generated.resources.workspace_update_button
import ampairsapp.feature.workspace.generated.resources.workspace_delete_button
import ampairsapp.feature.workspace.generated.resources.workspace_avatar_label
import ampairsapp.feature.workspace.generated.resources.workspace_avatar_upload
import ampairsapp.feature.workspace.generated.resources.workspace_avatar_remove
import ampairsapp.feature.workspace.generated.resources.workspace_avatar_hint
import ampairsapp.feature.workspace.generated.resources.workspace_delete_dialog_title
import ampairsapp.feature.workspace.generated.resources.workspace_delete_dialog_message
import ampairsapp.feature.workspace.generated.resources.workspace_delete_confirm_instruction
import ampairsapp.feature.workspace.generated.resources.workspace_delete_slug_label
import ampairsapp.feature.workspace.generated.resources.workspace_delete_slug_mismatch
import ampairsapp.feature.workspace.generated.resources.cd_selected_avatar
import ampairsapp.feature.workspace.generated.resources.cd_default_avatar
import ampairsapp.feature.workspace.generated.resources.cd_change_avatar
import ampairsapp.feature.workspace.generated.resources.cd_clear_avatar_selection
import ampairsapp.feature.workspace.generated.resources.cd_slug_available
import ampairsapp.feature.workspace.generated.resources.cd_slug_not_available
import ampairsapp.feature.workspace.generated.resources.cd_warning
import ampairsapp.feature.workspace.generated.resources.cd_workspace_avatar
import ampairsapp.feature.workspace.generated.resources.cancel
import ampairsapp.feature.workspace.generated.resources.delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceCreateScreen(
    onNavigateBack: () -> Unit,
    onWorkspaceCreated: (String) -> Unit,
    onEnterWorkspace: (workspaceId: String, workspaceSlug: String) -> Unit = { _, _ -> },
    workspaceId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: WorkspaceCreateViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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

    // Handle one-off navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkspaceCreateEvent.ArchiveSuccess,
                is WorkspaceCreateEvent.RestoreSuccess -> onNavigateBack()
                is WorkspaceCreateEvent.EnterCreatedWorkspace ->
                    onEnterWorkspace(event.workspaceId, event.workspaceSlug)
            }
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
            text = if (isEditMode) stringResource(Res.string.workspace_edit_title) else stringResource(Res.string.workspace_create_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isEditMode) stringResource(Res.string.workspace_edit_subtitle) else stringResource(Res.string.workspace_create_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar Section
        WorkspaceAvatarSection(
            avatarUrl = state.avatarUrl,
            workspaceId = state.workspaceId,
            workspaceName = state.name,
            selectedImageData = state.selectedAvatarData,
            isUploading = state.isUploadingAvatar,
            isEditMode = isEditMode,
            onPickAvatar = { viewModel.pickAvatar() },
            onUploadAvatar = { viewModel.uploadAvatar() },
            onDeleteAvatar = { viewModel.deleteAvatar() },
            onClearSelected = { viewModel.clearSelectedAvatar() }
        )

        // Avatar message/error
        state.avatarMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.avatarUploadError != null)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        color = if (state.avatarUploadError != null)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(
                        onClick = { viewModel.clearAvatarMessage() }
                    ) {
                        Text(stringResource(Res.string.workspace_dismiss))
                    }
                }
            }
        }

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
                        Text(stringResource(Res.string.workspace_dismiss))
                    }
                }
            }
        }

        // Workspace Name
        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.updateName(it) },
            label = { Text(stringResource(Res.string.workspace_name_label)) },
            placeholder = { Text(stringResource(Res.string.workspace_name_placeholder)) },
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
            label = { Text(stringResource(Res.string.workspace_slug_label)) },
            placeholder = { Text(stringResource(Res.string.workspace_slug_placeholder)) },
            isError = state.validationErrors.containsKey("slug"),
            supportingText = {
                state.validationErrors["slug"]?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                } ?: Text(
                    text = stringResource(Res.string.workspace_slug_url_hint, state.slug),
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
                            contentDescription = stringResource(Res.string.cd_slug_available),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    state.slug.length >= 2 && !state.isSlugAvailable -> {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = stringResource(Res.string.cd_slug_not_available),
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
            label = { Text(stringResource(Res.string.workspace_description_label)) },
            placeholder = { Text(stringResource(Res.string.workspace_description_placeholder)) },
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
                label = { Text(stringResource(Res.string.workspace_type_label)) },
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
                Text(if (isEditMode) stringResource(Res.string.workspace_updating) else stringResource(Res.string.workspace_creating))
            } else {
                Text(if (isEditMode) stringResource(Res.string.workspace_update_button) else stringResource(Res.string.workspace_create_button))
            }
        }

        // Cancel Button
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            Text(stringResource(Res.string.cancel))
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
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.workspace_delete_button))
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
            onConfirm = { viewModel.archiveWorkspace() },
            onDismiss = viewModel::hideDeleteDialog
        )
    }
}

@Composable
private fun WorkspaceAvatarSection(
    avatarUrl: String?,
    workspaceId: String?,
    workspaceName: String,
    selectedImageData: ByteArray?,
    isUploading: Boolean,
    isEditMode: Boolean,
    onPickAvatar: () -> Unit,
    onUploadAvatar: () -> Unit,
    onDeleteAvatar: () -> Unit,
    onClearSelected: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.workspace_avatar_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Avatar preview with camera overlay
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Avatar image or placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable(enabled = !isUploading) { onPickAvatar() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    selectedImageData != null -> {
                        // Show selected image preview using Coil's AsyncImage with ByteArray
                        AsyncImage(
                            model = selectedImageData,
                            contentDescription = stringResource(Res.string.cd_selected_avatar),
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !avatarUrl.isNullOrBlank() -> {
                        // Show current avatar from URL
                        val imageUrl = if (workspaceId != null) {
                            "${ApiUrlBuilder.workspaceAvatarUrl(workspaceId)}?t=${Random.nextLong()}"
                        } else {
                            avatarUrl
                        }
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = stringResource(Res.string.cd_workspace_avatar),
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    workspaceName.isNotBlank() -> {
                        // Show initials
                        val initials = workspaceName.split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .joinToString("")
                            .ifEmpty { workspaceName.firstOrNull()?.uppercase()?.toString() ?: "W" }
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        // Show default icon
                        Icon(
                            Icons.Default.Business,
                            contentDescription = stringResource(Res.string.cd_default_avatar),
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Loading overlay
                if (isUploading) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            // Camera button overlay (bottom right)
            if (!isUploading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onPickAvatar() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = stringResource(Res.string.cd_change_avatar),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Clear selected image button (top right) - only show when there's a selected image
            if (selectedImageData != null && !isUploading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable { onClearSelected() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.cd_clear_avatar_selection),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Upload button - only show in edit mode when there's a selected image
            if (isEditMode && selectedImageData != null) {
                Button(
                    onClick = onUploadAvatar,
                    enabled = !isUploading,
                    modifier = Modifier.height(36.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(Res.string.workspace_avatar_upload), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Delete button - only show in edit mode when there's an existing avatar
            if (isEditMode && !avatarUrl.isNullOrBlank() && selectedImageData == null) {
                OutlinedButton(
                    onClick = onDeleteAvatar,
                    enabled = !isUploading,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.workspace_avatar_remove), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Help text for new workspace
        if (!isEditMode) {
            Text(
                text = stringResource(Res.string.workspace_avatar_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
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
                contentDescription = stringResource(Res.string.cd_warning),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(stringResource(Res.string.workspace_delete_dialog_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.workspace_delete_dialog_message, workspaceName),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(Res.string.workspace_delete_confirm_instruction),
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
                    label = { Text(stringResource(Res.string.workspace_delete_slug_label)) },
                    singleLine = true,
                    enabled = !isDeleting,
                    isError = confirmationSlug.isNotEmpty() && !isConfirmationValid,
                    supportingText = {
                        if (confirmationSlug.isNotEmpty() && !isConfirmationValid) {
                            Text(stringResource(Res.string.workspace_delete_slug_mismatch), color = MaterialTheme.colorScheme.error)
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
                Text(stringResource(Res.string.delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}