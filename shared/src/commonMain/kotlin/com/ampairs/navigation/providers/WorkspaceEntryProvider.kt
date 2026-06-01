package com.ampairs.navigation.providers

import HomeScreen
import WorkspaceRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.sync.ui.SyncStatusScreen
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.ui.MemberDetailsScreen
import com.ampairs.workspace.ui.ModuleStoreScreen
import com.ampairs.workspace.ui.WorkspaceCreateScreen
import com.ampairs.workspace.ui.WorkspaceInvitationCreateScreen
import com.ampairs.workspace.ui.WorkspaceInvitationsScreen
import com.ampairs.workspace.ui.WorkspaceListScreen
import com.ampairs.workspace.ui.WorkspaceMembersScreen

/**
 * Entry provider for Workspace module routes in Navigation 3.
 * Returns NavEntry for workspace routes or null if route doesn't match.
 */
fun workspaceEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
    onNavigationServiceReady: ((DynamicModuleNavigationService?) -> Unit)?
): NavEntry<NavKey>? = when (key) {
    is WorkspaceRoute.Root -> NavEntry(key) {
        WorkspaceListScreen(
            onNavigateToCreateWorkspace = {
                backStack.add(WorkspaceRoute.Create)
            },
            onWorkspaceSelected = { workspaceId: String, workspaceSlug: String ->
                backStack.clear()
                backStack.add(WorkspaceRoute.Modules(workspaceId, workspaceSlug))
            },
            onWorkspaceEdit = { workspaceId: String ->
                backStack.add(WorkspaceRoute.Edit(workspaceId))
            },
            modifier = Modifier
        )
    }

    is WorkspaceRoute.Create -> NavEntry(key) {
        WorkspaceCreateScreen(
            onNavigateBack = {
                backStack.removeLastOrNull()
            },
            onWorkspaceCreated = { workspaceId ->
                backStack.removeLastOrNull()
            },
            modifier = Modifier
        )
    }

    is WorkspaceRoute.Edit -> NavEntry(key) {
        WorkspaceCreateScreen(
            onNavigateBack = {
                backStack.removeLastOrNull()
            },
            onWorkspaceCreated = { workspaceId ->
                backStack.removeLastOrNull()
            },
            workspaceId = key.workspaceId,
            modifier = Modifier
        )
    }

    is WorkspaceRoute.Members -> NavEntry(key) {
        WorkspaceMembersScreen(
            workspaceId = key.workspaceId,
            onNavigateBack = {
                backStack.removeLastOrNull()
            },
            onMemberClick = { memberId ->
                backStack.add(
                    WorkspaceRoute.MemberDetail(
                        key.workspaceId,
                        memberId
                    )
                )
            },
            onInviteClick = {
                backStack.add(WorkspaceRoute.CreateInvitation(key.workspaceId))
            }
        )
    }

    is WorkspaceRoute.Invitations -> NavEntry(key) {
        WorkspaceInvitationsScreen(
            workspaceId = key.workspaceId,
            onInviteClick = {
                backStack.add(WorkspaceRoute.CreateInvitation(key.workspaceId))
            }
        )
    }

    is WorkspaceRoute.MemberDetail -> NavEntry(key) {
        MemberDetailsScreen(
            workspaceId = key.workspaceId,
            memberId = key.memberId,
            onNavigateBack = {
                backStack.removeLastOrNull()
            },
            modifier = Modifier
        )
    }

    is WorkspaceRoute.CreateInvitation -> NavEntry(key) {
        WorkspaceInvitationCreateScreen(
            workspaceId = key.workspaceId,
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier
        )
    }

    is WorkspaceRoute.Modules -> NavEntry(key) {
        HomeScreen(
            workspaceId = key.workspaceId,
            backStack = backStack,
            onNavigationServiceReady = onNavigationServiceReady
        )
    }

    is WorkspaceRoute.ModuleStore -> NavEntry(key) {
        ModuleStoreScreen(
            workspaceId = key.workspaceId,
            paddingValues = PaddingValues(),
        )
    }

    is WorkspaceRoute.AcceptInvitation -> NavEntry(key) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Accept Invitation Screen - Coming Soon")
        }
    }

    is WorkspaceRoute.SyncStatus -> NavEntry(key) {
        SyncStatusScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
        )
    }

    else -> null
}
