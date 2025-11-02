package com.ampairs.workspace.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.viewmodel.WorkspaceMembersViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Comprehensive member management screen for workspace administration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceMembersScreen(
    workspaceId: String,
    onNavigateBack: () -> Unit,
    onMemberClick: (String) -> Unit,
    onInviteClick: () -> Unit,
) {
    val viewModel: WorkspaceMembersViewModel = koinViewModel { parametersOf(workspaceId) }
    val state by viewModel.state.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("ALL") }
    var selectedStatus by remember { mutableStateOf("ALL") }

    LaunchedEffect(workspaceId) {
        viewModel.loadMembers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Team Members",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row {
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(
                        if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        contentDescription = "Toggle Filters"
                    )
                }

                Button(
                    onClick = onInviteClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invite Member")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filters section
        if (showFilters) {
            MemberFiltersSection(
                selectedRole = selectedRole,
                selectedStatus = selectedStatus,
                onRoleChanged = { selectedRole = it; viewModel.filterMembers(it, selectedStatus) },
                onStatusChanged = { selectedStatus = it; viewModel.filterMembers(selectedRole, it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Members summary card
        if (state.members.isNotEmpty()) {
            MembersSummaryCard(
                totalMembers = state.members.size,
                activeMembers = state.members.count { it.status == "ACTIVE" },
                recentlyActive = state.members.count {
                    it.lastActivity != null &&
                            // Simple check for recent activity (could be enhanced with proper date parsing)
                            true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            // Offline mode indicator
            if (state.isOfflineMode) {
                OfflineModeIndicator(
                    onRefresh = { viewModel.loadMembers(forceRefresh = true) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Members list
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
                    onRetry = { viewModel.loadMembers() }
                )
            }

            state.members.isEmpty() -> {
                EmptyMembersState(onInviteClick = onInviteClick)
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.members) { member ->
                        MemberCard(
                            member = member,
                            onMemberClick = { onMemberClick(member.id) },
                            canManageMembers = state.currentUserRole?.permissions
                                ?.get("member_management")
                                ?.get("can_manage_members") == true
                        )
                    }
                    
                    // Pagination footer
                    item {
                        PaginationFooter(
                            currentPage = state.currentPage,
                            totalPages = state.totalPages,
                            totalMembers = state.totalMembers,
                            hasNextPage = state.hasNextPage,
                            isLoading = state.isLoading,
                            onLoadMore = { viewModel.loadMoreMembers() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberFiltersSection(
    selectedRole: String,
    selectedStatus: String,
    onRoleChanged: (String) -> Unit,
    onStatusChanged: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Role filter
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Role",
                        style = MaterialTheme.typography.labelMedium
                    )

                    var roleExpanded by remember { mutableStateOf(false) }
                    val roles = listOf("ALL", "OWNER", "ADMIN", "MANAGER", "MEMBER", "VIEWER")

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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

                // Status filter
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium
                    )

                    var statusExpanded by remember { mutableStateOf(false) }
                    val statuses = listOf("ALL", "ACTIVE", "INACTIVE", "PENDING")

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
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
            }
        }
    }
}

@Composable
private fun MembersSummaryCard(
    totalMembers: Int,
    activeMembers: Int,
    recentlyActive: Int,
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
                icon = Icons.Default.Group,
                value = totalMembers.toString(),
                label = "Total Members"
            )

            SummaryItem(
                icon = Icons.Default.Person,
                value = activeMembers.toString(),
                label = "Active Members"
            )

            SummaryItem(
                icon = Icons.Default.AccessTime,
                value = recentlyActive.toString(),
                label = "Recently Active"
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MemberCard(
    member: WorkspaceMember,
    onMemberClick: () -> Unit,
    canManageMembers: Boolean,
) {
    Card(
        onClick = onMemberClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Member Avatar",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = member.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    MemberRoleChip(role = member.role)
                }

                if (canManageMembers && member.role != "OWNER") {
                    IconButton(
                        onClick = { /* Show member actions menu */ }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Member Actions")
                    }
                }
            }

            member.lastActivity?.let { lastActivity ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last active: $lastActivity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun EmptyMembersState(onInviteClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Group,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No team members yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Invite your first team member to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onInviteClick) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Invite Member")
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
            text = "Failed to load members",
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
private fun PaginationFooter(
    currentPage: Int,
    totalPages: Int,
    totalMembers: Int,
    hasNextPage: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page info
        Text(
            text = "Page ${currentPage + 1} of $totalPages • $totalMembers total members",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (hasNextPage) {
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Button(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Load More Members")
                }
            }
        } else if (currentPage > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "All members loaded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OfflineModeIndicator(
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
                        text = "Showing cached data",
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