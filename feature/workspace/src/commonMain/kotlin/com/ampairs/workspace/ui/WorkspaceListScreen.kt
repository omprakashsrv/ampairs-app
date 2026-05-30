package com.ampairs.workspace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.workspace.domain.Workspace
import com.ampairs.workspace.domain.UserInvitation
import com.ampairs.workspace.viewmodel.WorkspaceListEvent
import com.ampairs.workspace.viewmodel.WorkspaceListViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.workspace.generated.resources.Res
import ampairsapp.feature.workspace.generated.resources.cd_back
import ampairsapp.feature.workspace.generated.resources.cd_create_workspace
import ampairsapp.feature.workspace.generated.resources.cd_edit_workspace
import ampairsapp.feature.workspace.generated.resources.cd_no_workspaces
import ampairsapp.feature.workspace.generated.resources.cd_offline
import ampairsapp.feature.workspace.generated.resources.cd_retry
import ampairsapp.feature.workspace.generated.resources.cd_search
import ampairsapp.feature.workspace.generated.resources.cd_workspace_avatar
import ampairsapp.feature.workspace.generated.resources.workspace_create
import ampairsapp.feature.workspace.generated.resources.workspace_dismiss
import ampairsapp.feature.workspace.generated.resources.workspace_empty_desc
import ampairsapp.feature.workspace.generated.resources.workspace_empty_title
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_accept
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_decline
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_expires_in
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_expires_soon
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_invited_by
import ampairsapp.feature.workspace.generated.resources.workspace_invitation_role_prefix
import ampairsapp.feature.workspace.generated.resources.workspace_invitations_loading
import ampairsapp.feature.workspace.generated.resources.workspace_invitations_title
import ampairsapp.feature.workspace.generated.resources.workspace_loading_workspaces
import ampairsapp.feature.workspace.generated.resources.workspace_members_count
import ampairsapp.feature.workspace.generated.resources.workspace_no_results_hint
import ampairsapp.feature.workspace.generated.resources.workspace_no_results_title
import ampairsapp.feature.workspace.generated.resources.workspace_offline_label
import ampairsapp.feature.workspace.generated.resources.workspace_refreshing
import ampairsapp.feature.workspace.generated.resources.workspace_search_hint
import ampairsapp.feature.workspace.generated.resources.workspace_showing_cached_data
import ampairsapp.feature.workspace.generated.resources.cd_refresh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceListScreen(
    onNavigateToCreateWorkspace: () -> Unit,
    onWorkspaceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onWorkspaceEdit: (String) -> Unit = {},
    viewModel: WorkspaceListViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadWorkspaces()
        viewModel.events.collect { event ->
            when (event) {
                is WorkspaceListEvent.NavigateToModules -> onWorkspaceSelected(event.workspaceId)
            }
        }
    }

    LaunchedEffect(searchQuery) {
        viewModel.searchWorkspaces(searchQuery)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // Invitations section - placed above workspace search/content
            if (state.invitations.isNotEmpty()) {
                InvitationsSection(
                    invitations = state.invitations,
                    isLoading = state.isInvitationsLoading,
                    error = state.invitationsError,
                    processingIds = state.processingInvitationIds,
                    onAccept = viewModel::acceptInvitation,
                    onReject = viewModel::rejectInvitation,
                    onClearError = viewModel::clearInvitationsError,
                    onRefresh = viewModel::refreshInvitations
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search Bar with offline/online indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(Res.string.workspace_search_hint)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.cd_search))
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Offline Mode Indicator (only show when offline)
//                if (state.isOfflineMode) {
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Surface(
//                        color = MaterialTheme.colorScheme.errorContainer,
//                        shape = RoundedCornerShape(8.dp)
//                    ) {
//                        Row(
//                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.WifiOff,
//                                contentDescription = "Offline",
//                                tint = MaterialTheme.colorScheme.onErrorContainer,
//                                modifier = Modifier.size(16.dp)
//                            )
//                            Spacer(modifier = Modifier.width(4.dp))
//                            Text(
//                                text = "Offline",
//                                style = MaterialTheme.typography.labelSmall,
//                                color = MaterialTheme.colorScheme.onErrorContainer
//                            )
//                        }
//                    }
//                }
            }

            // Error message with retry option
            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isOfflineMode) Icons.Default.CloudOff else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                                stringResource(Res.string.workspace_showing_cached_data, error)
                            else
                                error,
                            color = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                                MaterialTheme.colorScheme.onSecondaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row {
                            // Retry button for network errors
                            if (!state.isRefreshing) {
                                IconButton(
                                    onClick = { viewModel.refreshWorkspaces() }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = stringResource(Res.string.cd_retry),
                                        tint = if (state.isOfflineMode && state.workspaces.isNotEmpty())
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            TextButton(
                                onClick = { viewModel.clearError() }
                            ) {
                                Text(stringResource(Res.string.workspace_dismiss))
                            }
                        }
                    }
                }
            }

            // Content
            when {
                state.isLoading && state.workspaces.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(Res.string.workspace_loading_workspaces))
                        }
                    }
                }

                state.hasNoWorkspaces && searchQuery.isEmpty() -> {
                    // No workspaces state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = stringResource(Res.string.cd_no_workspaces),
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(Res.string.workspace_empty_title),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(Res.string.workspace_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onNavigateToCreateWorkspace
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(Res.string.workspace_create))
                            }
                        }
                    }
                }

                state.workspaces.isEmpty() && searchQuery.isNotEmpty() -> {
                    // No search results
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(Res.string.workspace_no_results_title),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                stringResource(Res.string.workspace_no_results_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                else -> {
                    // Workspace list with pull-to-refresh indicator
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Pull-to-refresh indicator
                        if (state.isRefreshing) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(Res.string.workspace_refreshing),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }

                        items(state.workspaces, key = { it.id }) { workspace ->
                            WorkspaceCard(
                                workspace = workspace,
                                isOfflineMode = state.isOfflineMode,
                                onClick = { viewModel.selectWorkSpace(workspace.id) },
                                onEdit = { onWorkspaceEdit(workspace.id) }
                            )
                        }

                        // Bottom padding for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToCreateWorkspace,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_create_workspace))
        }
    }
}

@Composable
private fun WorkspaceCard(
    workspace: Workspace,
    isOfflineMode: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isOfflineMode)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with enhanced visual indicators
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!workspace.avatarUrl.isNullOrEmpty()) {
                    // Load avatar image from server
                    val avatarUrl = ApiUrlBuilder.workspaceAvatarThumbnailUrl(workspace.id)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(avatarUrl)
                            .size(Size(96, 96))
                            .build(),
                        contentDescription = stringResource(Res.string.cd_workspace_avatar),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Show initials
                    Text(
                        text = workspace.name.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content with improved layout
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = workspace.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                workspace.description?.let { description ->
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Workspace type with enhanced styling
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = workspace.workspaceType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    workspace.memberCount?.let { count ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pluralStringResource(Res.plurals.workspace_members_count, count, count),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Offline sync indicator
                    if (isOfflineMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = stringResource(Res.string.cd_offline),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = stringResource(Res.string.workspace_offline_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Edit button
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.cd_edit_workspace),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun InvitationsSection(
    invitations: List<UserInvitation>,
    isLoading: Boolean,
    error: String?,
    processingIds: Set<String>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onClearError: () -> Unit,
    onRefresh: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Mail,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.workspace_invitations_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                if (!isLoading) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.cd_refresh),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearError) {
                        Text(stringResource(Res.string.workspace_dismiss), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.workspace_invitations_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                invitations.forEach { invitation ->
                    InvitationCard(
                        invitation = invitation,
                        isProcessing = processingIds.contains(invitation.id),
                        onAccept = { onAccept(invitation.id) },
                        onReject = { onReject(invitation.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: UserInvitation,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Workspace icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = invitation.workspaceName.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invitation.workspaceName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = stringResource(Res.string.workspace_invitation_role_prefix, invitation.role),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    invitation.inviterName?.let { inviter ->
                        Text(
                            text = stringResource(Res.string.workspace_invitation_invited_by, inviter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    invitation.daysUntilExpiry?.let { days ->
                        Text(
                            text = if (days > 0)
                                stringResource(Res.string.workspace_invitation_expires_in, days)
                            else
                                stringResource(Res.string.workspace_invitation_expires_soon),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (days <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }

                    invitation.message?.let { message ->
                        if (message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"$message\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isProcessing,
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.workspace_invitation_decline), style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAccept,
                    enabled = !isProcessing,
                    modifier = Modifier.height(32.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.workspace_invitation_accept), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}