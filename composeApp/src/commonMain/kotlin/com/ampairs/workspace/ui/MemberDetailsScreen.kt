package com.ampairs.workspace.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.workspace.api.model.UserRoleResponse
import com.ampairs.workspace.api.model.WorkspaceRole
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.viewmodel.MemberDetailsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Comprehensive member details screen with view and edit capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailsScreen(
    workspaceId: String,
    memberId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MemberDetailsViewModel = koinViewModel { parametersOf(workspaceId, memberId) }
    val state by viewModel.state.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var showStatusConfirmation by remember { mutableStateOf(false) }
    var pendingStatusChange by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(memberId) {
        viewModel.loadMemberDetails()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with navigation and actions
        MemberDetailsHeader(
            isEditing = isEditing,
            canEdit = state.canEdit,
            onToggleEdit = { isEditing = !isEditing },
            onSave = {
                viewModel.saveMemberChanges()
                isEditing = false
            },
            onCancel = {
                viewModel.discardChanges()
                isEditing = false
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on state
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                ErrorState(
                    error = state.error!!,
                    onRetry = { viewModel.refresh() }
                )
            }

            state.member != null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Offline mode indicator
                    if (state.isOfflineMode) {
                        MemberDetailsOfflineIndicator(
                            onRefresh = { viewModel.refresh() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val memberToDisplay =
                        if (isEditing) state.displayMember ?: state.member!! else state.member!!
                    MemberDetailsContent(
                        member = memberToDisplay,
                        userRole = state.userRole,
                        availableRoles = state.availableRoles,
                        availablePermissions = state.availablePermissions,
                        isEditing = isEditing,
                        onRoleChange = viewModel::updateRole,
                        onStatusChange = { newStatus ->
                            val currentMember = state.member!!
                            // Show confirmation for deactivation or reactivation
                            if (currentMember.status != newStatus && (newStatus == "INACTIVE" || currentMember.status == "INACTIVE")) {
                                pendingStatusChange = newStatus
                                showStatusConfirmation = true
                            } else {
                                viewModel.updateStatus(newStatus)
                            }
                        },
                        onPermissionsChange = viewModel::updatePermissions,
                        onRemoveMember = {
                            viewModel.removeMember()
                            onNavigateBack()
                        },
                        canChangeRole = state.canChangeRole,
                        canChangeStatus = state.canChangeStatus,
                        canChangePermissions = state.canChangePermissions,
                        canRemoveMember = state.canRemoveMember,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Status change confirmation dialog
        if (showStatusConfirmation) {
            val currentMember = state.member!!
            val statusAction = if (pendingStatusChange == "INACTIVE") "deactivate" else "reactivate"
            val statusResult =
                if (pendingStatusChange == "INACTIVE") "deactivated" else "reactivated"

            AlertDialog(
                onDismissRequest = {
                    showStatusConfirmation = false
                    pendingStatusChange = null
                },
                title = { Text("${statusAction.replaceFirstChar { it.uppercaseChar() }} Member") },
                text = {
                    Text("Are you sure you want to $statusAction ${currentMember.name}? This member will be $statusResult and ${if (pendingStatusChange == "INACTIVE") "lose access to" else "regain access to"} the workspace.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingStatusChange?.let { newStatus ->
                                viewModel.updateStatus(newStatus)
                            }
                            showStatusConfirmation = false
                            pendingStatusChange = null
                        },
                        colors = if (pendingStatusChange == "INACTIVE") {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        } else ButtonDefaults.buttonColors()
                    ) {
                        Text(statusAction.replaceFirstChar { it.uppercaseChar() })
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showStatusConfirmation = false
                            pendingStatusChange = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun MemberDetailsHeader(
    isEditing: Boolean,
    canEdit: Boolean,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Member Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        AnimatedVisibility(visible = canEdit) {
            Row {
                if (isEditing) {
                    // Save and Cancel buttons when editing
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                } else {
                    // Edit button when viewing
                    Button(onClick = onToggleEdit) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberDetailsContent(
    member: WorkspaceMember,
    userRole: UserRoleResponse?,
    availableRoles: List<WorkspaceRole>,
    availablePermissions: Map<String, Map<String, Boolean>>,
    isEditing: Boolean,
    onRoleChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onPermissionsChange: (List<String>) -> Unit,
    onRemoveMember: () -> Unit,
    canChangeRole: Boolean,
    canChangeStatus: Boolean,
    canChangePermissions: Boolean,
    canRemoveMember: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Member Profile Card
        MemberProfileCard(
            member = member,
            isEditing = isEditing
        )

        // Role and Status Card
        MemberRoleStatusCard(
            member = member,
            availableRoles = availableRoles,
            isEditing = isEditing,
            canChangeRole = canChangeRole,
            canChangeStatus = canChangeStatus,
            onRoleChange = onRoleChange,
            onStatusChange = onStatusChange
        )

        // Permissions Card
        MemberPermissionsCard(
            member = member,
            availablePermissions = availablePermissions,
            isEditing = isEditing,
            canEdit = canChangePermissions,
            onPermissionsChange = onPermissionsChange
        )

        // Activity Card
        MemberActivityCard(member = member)

        // Danger Zone (only if can remove member)
        if (canRemoveMember && member.role != "OWNER") {
            MemberDangerZoneCard(
                member = member,
                onRemoveMember = onRemoveMember
            )
        }
    }
}

@Composable
private fun MemberProfileCard(
    member: WorkspaceMember,
    isEditing: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Member Avatar",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    member.email?.let { email ->
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    member.phone?.let { phone ->
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRoleStatusCard(
    member: WorkspaceMember,
    availableRoles: List<WorkspaceRole>,
    isEditing: Boolean,
    canChangeRole: Boolean,
    canChangeStatus: Boolean,
    onRoleChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Role & Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Role Section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Role",
                        style = MaterialTheme.typography.labelMedium
                    )

                    if (isEditing && canChangeRole) {
                        RoleDropdown(
                            currentRole = member.role,
                            availableRoles = availableRoles,
                            onRoleChange = onRoleChange
                        )
                    } else {
                        MemberRoleChip(role = member.role)
                    }
                }

                // Status Section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium
                    )

                    if (isEditing && canChangeStatus) {
                        StatusDropdown(
                            currentStatus = member.status,
                            onStatusChange = onStatusChange
                        )
                    } else {
                        MemberStatusChip(status = member.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberPermissionsCard(
    member: WorkspaceMember,
    availablePermissions: Map<String, Map<String, Boolean>>,
    isEditing: Boolean,
    canEdit: Boolean,
    onPermissionsChange: (List<String>) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing && canEdit) {
                PermissionEditor(
                    currentPermissions = member.permissions,
                    availablePermissions = availablePermissions,
                    onPermissionsChange = onPermissionsChange
                )
            } else {
                PermissionViewer(permissions = member.permissions)
            }
        }
    }
}

@Composable
private fun MemberActivityCard(
    member: WorkspaceMember,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityItem(
                    label = "Joined",
                    value = member.joinedAt
                )

                member.lastActivity?.let { lastActivity ->
                    ActivityItem(
                        label = "Last Activity",
                        value = lastActivity
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberDangerZoneCard(
    member: WorkspaceMember,
    onRemoveMember: () -> Unit,
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showRemoveDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.PersonRemove, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove Member")
            }
        }
    }

    // Remove member confirmation dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Member") },
            text = {
                Text("Are you sure you want to remove ${member.name} from this workspace? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMember()
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper Composables

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    currentRole: String,
    availableRoles: List<com.ampairs.workspace.api.model.WorkspaceRole>,
    onRoleChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // Use backend roles if available, fallback to default roles
    val roles = if (availableRoles.isNotEmpty()) {
        availableRoles.map { it.name }.filter { it != "OWNER" } // Exclude OWNER role from dropdown
    } else {
        listOf("ADMIN", "MANAGER", "MEMBER", "VIEWER") // Fallback for when roles haven't loaded yet
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentRole,
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            roles.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role) },
                    onClick = {
                        onRoleChange(role)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    currentStatus: String,
    onStatusChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = listOf("ACTIVE", "INACTIVE")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentStatus,
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            statuses.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onStatusChange(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MemberRoleChip(role: String) {
    val roleColors = when (role) {
        "OWNER" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        "ADMIN" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "MANAGER" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        "MEMBER" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "VIEWER" -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AssistChip(
        onClick = { },
        label = { Text(role) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = roleColors.first,
            labelColor = roleColors.second
        )
    )
}

@Composable
private fun MemberStatusChip(status: String) {
    val statusColors = when (status) {
        "ACTIVE" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "INACTIVE" -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
        "PENDING" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AssistChip(
        onClick = { },
        label = { Text(status) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = statusColors.first,
            labelColor = statusColors.second
        )
    )
}

@Composable
private fun PermissionEditor(
    currentPermissions: List<String>,
    availablePermissions: Map<String, Map<String, Boolean>>,
    onPermissionsChange: (List<String>) -> Unit,
) {
    // Convert current permissions list to a set for easier checking
    val currentPermissionKeys = remember(currentPermissions) {
        currentPermissions.toSet()
    }

    // Track selected permissions
    var selectedPermissions by remember(currentPermissionKeys) {
        mutableStateOf(currentPermissionKeys.toMutableSet())
    }

    // Update callback when selectedPermissions changes
    LaunchedEffect(selectedPermissions) {
        onPermissionsChange(selectedPermissions.toList())
    }

    if (availablePermissions.isEmpty()) {
        Text(
            text = "Loading permissions...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            availablePermissions.forEach { (module, actions) ->
                // Module section
                Column {
                    Text(
                        text = module.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Actions for this module
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        actions.forEach { (action, _) ->
                            // Convert module:action format to enum format (e.g., "member:view" -> "MEMBER_VIEW")
                            val enumFormat = "${module.uppercase()}_${action.uppercase()}"
                            val isSelected =
                                selectedPermissions.contains(enumFormat) // Enum format from backend

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { isChecked ->
                                        selectedPermissions =
                                            selectedPermissions.toMutableSet().apply {
                                                if (isChecked) {
                                                    add(enumFormat) // Add in backend enum format
                                                } else {
                                                    // Remove all possible formats of this permission
                                                    remove(enumFormat)
                                                }
                                            }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = action.replaceFirstChar { it.uppercaseChar() },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionViewer(
    permissions: List<String>,
) {
    if (permissions.isEmpty()) {
        Text(
            text = "No specific permissions assigned",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        // Group permissions by module for better organization
        val groupedPermissions = permissions.groupBy { permission ->
            // Extract module from permission name (e.g., "MEMBER_VIEW" -> "member")
            val parts = permission.split("_")
            if (parts.size > 1) parts[0].lowercase() else "general"
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedPermissions.forEach { (module, modulePermissions) ->
                Column {
                    Text(
                        text = module.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        modulePermissions.forEach { permission ->
                            // Extract action from permission name (e.g., "MEMBER_VIEW" -> "View")
                            val action = permission.split("_").drop(1).joinToString(" ") { part ->
                                part.lowercase().replaceFirstChar { it.uppercaseChar() }
                            }
                            Text(
                                text = "• $action",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Failed to load member details",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

@Composable
private fun MemberDetailsOfflineIndicator(
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Offline Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Showing cached member details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            TextButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh")
            }
        }
    }
}