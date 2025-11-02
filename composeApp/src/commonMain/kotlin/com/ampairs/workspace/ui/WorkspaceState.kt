package com.ampairs.workspace.ui

import com.ampairs.workspace.domain.Workspace
import com.ampairs.workspace.domain.UserInvitation

data class WorkspaceListState(
    val workspaces: List<Workspace> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val hasNoWorkspaces: Boolean = false,
    val userFullName: String = "User",
    val isUserLoading: Boolean = false,
    val isOfflineMode: Boolean = false, // Indicates if we're in offline mode due to network issues

    // User invitations
    val invitations: List<UserInvitation> = emptyList(),
    val isInvitationsLoading: Boolean = false,
    val invitationsError: String? = null,
    val processingInvitationIds: Set<String> = emptySet() // Track invitations being processed
)

data class WorkspaceCreateState(
    val workspaceId: String? = null, // For edit mode
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val workspaceType: String = "BUSINESS",
    val avatarUrl: String? = null,
    val timezone: String = "UTC",
    val language: String = "en",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSlugChecking: Boolean = false,
    val isSlugAvailable: Boolean = true,
    val isSlugModified: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val createdWorkspaceId: String? = null,
    val isLoadingWorkspace: Boolean = false, // For loading workspace data in edit mode
)

data class WorkspaceDetailState(
    val workspace: Workspace? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
)