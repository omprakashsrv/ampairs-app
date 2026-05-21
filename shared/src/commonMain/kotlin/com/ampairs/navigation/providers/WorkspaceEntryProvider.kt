package com.ampairs.navigation.providers

import Route
import SubscriptionRoute
import WorkspaceRoute
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.di.LocalAppGraph
import com.ampairs.workspace.navigation.DynamicModuleNavigationService
import com.ampairs.workspace.ui.MemberDetailsScreen
import com.ampairs.workspace.ui.ModuleStoreScreen
import com.ampairs.workspace.ui.WorkspaceCreateScreen
import com.ampairs.workspace.ui.WorkspaceInvitationCreateScreen
import com.ampairs.workspace.ui.WorkspaceInvitationsScreen
import com.ampairs.workspace.ui.WorkspaceListScreen
import com.ampairs.workspace.ui.WorkspaceMembersScreen
import com.ampairs.workspace.ui.WorkspaceModulesScreen

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
        val appPreferences = LocalAppGraph.current.appPreferences
        WorkspaceListScreen(
            appPreferences = appPreferences,
            onNavigateToCreateWorkspace = {
                backStack.add(WorkspaceRoute.Create)
            },
            onWorkspaceSelected = { workspaceId: String ->
                backStack.clear()
                backStack.add(WorkspaceRoute.Modules(workspaceId))
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
                backStack.add(SubscriptionRoute.Plans)
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
        val graph = LocalAppGraph.current
        val subscriptionViewModel = viewModel(key = key.workspaceId + "_subscription") {
            graph.subscriptionViewModelFactory.create()
        }
        WorkspaceModulesScreen(
            workspaceId = key.workspaceId,
            subscriptionViewModel = subscriptionViewModel,
            subscriptionOnboardingManager = graph.subscriptionOnboardingManager,
            onModuleSelected = { moduleCode ->
                // Handle module selection based on code
                when (moduleCode) {
                    "customer-management" -> backStack.add(Route.Customer)
                    "product-management" -> backStack.add(Route.Product)
                    "order-management" -> backStack.add(Route.Order)
                    "invoice-management" -> backStack.add(Route.Invoice)
                    "inventory-management" -> backStack.add(Route.Inventory)
                    "tax" -> backStack.add(Route.Tax)
                    "business" -> backStack.add(Route.Business)
                    "subscription" -> backStack.add(Route.Subscription)
                    else -> println("Unknown module: $moduleCode")
                }
            },
            onNavigateToModuleStore = {
                backStack.add(WorkspaceRoute.ModuleStore(key.workspaceId))
            },
            onNavigateToSubscription = {
                backStack.add(SubscriptionRoute.Plans)
            },
            onNavigationServiceReady = onNavigationServiceReady,
            paddingValues = PaddingValues()
        )
    }

    is WorkspaceRoute.ModuleStore -> NavEntry(key) {
        ModuleStoreScreen(
            workspaceId = key.workspaceId,
            paddingValues = PaddingValues(),
            onModuleNavigate = { route ->
                backStack.removeLastOrNull()
                backStack.add(route as NavKey)
            }
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

    else -> null
}
