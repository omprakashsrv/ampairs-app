package com.ampairs.workspace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.workspace.domain.WorkspaceInvitation
import com.ampairs.workspace.viewmodel.WorkspaceInvitationsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.workspace.generated.resources.Res
import ampairsapp.feature.workspace.generated.resources.workspace_invitations_header
import ampairsapp.feature.workspace.generated.resources.cd_toggle_filters
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_send_invite
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_filters
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_status_label
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_role_label
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_total_sent
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_pending_label
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_accepted_label
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_expired_label
import ampairsapp.feature.workspace.generated.resources.cd_invitation_actions
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_resend
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_cancel_action
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_sent_by
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_created_at
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_expires_at_label
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_resent_count
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_delivery_sent
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_delivery_delivered
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_delivery_opened
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_delivery_clicked
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_empty_title
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_empty_desc
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_send_first
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_error_title
import ampairsapp.feature.workspace.generated.resources.try_again

/**
 * Comprehensive invitation management screen for workspace administration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceInvitationsScreen(
    workspaceId: String,
    onInviteClick: () -> Unit,
    viewModel: WorkspaceInvitationsViewModel = assistedMetroViewModel<WorkspaceInvitationsViewModel, WorkspaceInvitationsViewModel.Factory> { create(workspaceId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf("ALL") }
    var selectedRole by remember { mutableStateOf("ALL") }

    LaunchedEffect(workspaceId) {
        // Force refresh on first load to ensure we get fresh data from the API
        viewModel.loadInvitations(refresh = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with actions - Always visible at top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.workspace_invitations_header),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(
                        if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        contentDescription = stringResource(Res.string.cd_toggle_filters)
                    )
                }

                Button(
                    onClick = onInviteClick,
                    modifier = Modifier.padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.workspace_invitation_send_invite),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filters section
        if (showFilters) {
            InvitationFiltersSection(
                selectedStatus = selectedStatus,
                selectedRole = selectedRole,
                onStatusChanged = {
                    selectedStatus = it; viewModel.filterInvitations(
                    it,
                    selectedRole
                )
                },
                onRoleChanged = {
                    selectedRole = it; viewModel.filterInvitations(
                    selectedStatus,
                    it
                )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Invitations summary card
        if (state.invitations.isNotEmpty()) {
            InvitationsSummaryCard(
                totalInvitations = state.invitations.size,
                pendingInvitations = state.invitations.count { it.status == "PENDING" },
                acceptedInvitations = state.invitations.count { it.status == "ACCEPTED" },
                expiredInvitations = state.invitations.count { it.status == "EXPIRED" }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Invitations list
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
                    onRetry = { viewModel.loadInvitations() }
                )
            }

            state.invitations.isEmpty() -> {
                EmptyInvitationsState(onInviteClick = onInviteClick)
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.invitations) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            onResend = { viewModel.resendInvitation(invitation.id) },
                            onCancel = { viewModel.cancelInvitation(invitation.id) },
                            canManageInvitations = state.canManageInvitations
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitationFiltersSection(
    selectedStatus: String,
    selectedRole: String,
    onStatusChanged: (String) -> Unit,
    onRoleChanged: (String) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.workspace_invitation_filters),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status filter
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.workspace_invitation_status_label),
                        style = MaterialTheme.typography.labelMedium
                    )

                    var statusExpanded by remember { mutableStateOf(false) }
                    val statuses =
                        listOf("ALL", "PENDING", "ACCEPTED", "EXPIRED", "CANCELLED", "DECLINED")

                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedStatus,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    onClick = {
                                        onStatusChanged(status)
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Role filter
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.workspace_invitation_role_label),
                        style = MaterialTheme.typography.labelMedium
                    )

                    var roleExpanded by remember { mutableStateOf(false) }
                    val roles = listOf("ALL", "ADMIN", "MANAGER", "MEMBER", "VIEWER")

                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRole,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            roles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        onRoleChanged(role)
                                        roleExpanded = false
                                    }
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
private fun InvitationsSummaryCard(
    totalInvitations: Int,
    pendingInvitations: Int,
    acceptedInvitations: Int,
    expiredInvitations: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                icon = Icons.AutoMirrored.Filled.Send,
                value = totalInvitations.toString(),
                label = stringResource(Res.string.workspace_invitation_total_sent)
            )

            SummaryItem(
                icon = Icons.Default.Schedule,
                value = pendingInvitations.toString(),
                label = stringResource(Res.string.workspace_invitation_pending_label),
                iconTint = MaterialTheme.colorScheme.tertiary
            )

            SummaryItem(
                icon = Icons.Default.CheckCircle,
                value = acceptedInvitations.toString(),
                label = stringResource(Res.string.workspace_invitation_accepted_label),
                iconTint = MaterialTheme.colorScheme.primary
            )

            SummaryItem(
                icon = Icons.Default.Error,
                value = expiredInvitations.toString(),
                label = stringResource(Res.string.workspace_invitation_expired_label),
                iconTint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InvitationCard(
    invitation: WorkspaceInvitation,
    onResend: () -> Unit,
    onCancel: () -> Unit,
    canManageInvitations: Boolean,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recipient info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = invitation.recipientName
                            ?: "+${invitation.countryCode} ${invitation.phone}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (invitation.recipientName != null) {
                        Text(
                            text = "+${invitation.countryCode} ${invitation.phone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InvitationStatusChip(status = invitation.status)

                        Spacer(modifier = Modifier.width(8.dp))

                        InvitationRoleChip(role = invitation.invitedRole)
                    }
                }

                // Actions menu
                if (canManageInvitations) {
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.cd_invitation_actions))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (invitation.status == "PENDING") {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.workspace_invitation_resend)) },
                                    onClick = {
                                        onResend()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.workspace_invitation_cancel_action)) },
                                    onClick = {
                                        onCancel()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Cancel, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Invitation details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.workspace_invitation_sent_by, invitation.sentByName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(Res.string.workspace_invitation_created_at, invitation.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(Res.string.workspace_invitation_expires_at_label, invitation.expiresAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (invitation.resendCount > 0) {
                        Text(
                            text = stringResource(Res.string.workspace_invitation_resent_count, invitation.resendCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Delivery status indicators
            if (invitation.status == "PENDING" || invitation.status == "ACCEPTED") {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DeliveryStatusIndicator(
                        label = stringResource(Res.string.workspace_invitation_delivery_sent),
                        isCompleted = invitation.emailSent,
                        icon = Icons.AutoMirrored.Default.Send
                    )

                    DeliveryStatusIndicator(
                        label = stringResource(Res.string.workspace_invitation_delivery_delivered),
                        isCompleted = invitation.emailDelivered,
                        icon = Icons.Default.Email
                    )

                    DeliveryStatusIndicator(
                        label = stringResource(Res.string.workspace_invitation_delivery_opened),
                        isCompleted = invitation.emailOpened,
                        icon = Icons.Default.Visibility
                    )

                    DeliveryStatusIndicator(
                        label = stringResource(Res.string.workspace_invitation_delivery_clicked),
                        isCompleted = invitation.linkClicked,
                        icon = Icons.Default.TouchApp
                    )
                }
            }

            // Custom message if present
            invitation.invitationMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "\"$message\"",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitationStatusChip(status: String) {
    val statusColors = when (status) {
        "PENDING" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "ACCEPTED" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "EXPIRED" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        "CANCELLED" -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.onSurface
        "DECLINED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
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
private fun InvitationRoleChip(role: String) {
    val roleColors = when (role) {
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
private fun DeliveryStatusIndicator(
    label: String,
    isCompleted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun EmptyInvitationsState(onInviteClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Default.Send,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.workspace_invitation_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.workspace_invitation_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onInviteClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.workspace_invitation_send_first),
                style = MaterialTheme.typography.labelLarge
            )
        }
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
            text = stringResource(Res.string.workspace_invitation_error_title),
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
            Text(stringResource(Res.string.try_again))
        }
    }
}