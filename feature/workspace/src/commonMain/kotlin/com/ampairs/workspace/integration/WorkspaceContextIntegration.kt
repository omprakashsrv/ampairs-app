package com.ampairs.workspace.integration

import com.ampairs.workspace.context.WorkspaceContext
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.workspace.domain.Workspace
import com.ampairs.workspace.domain.WorkspaceMember
import com.ampairs.workspace.WorkspaceDatabaseManager
import com.ampairs.workspace.navigation.GlobalNavigationManager

/**
 * Integration utilities for converting domain models to workspace context
 * and managing workspace context state transitions
 */
object WorkspaceContextIntegration {

    /**
     * Convert domain Workspace to WorkspaceContext for state management
     * This is called when user selects a workspace from the workspace list
     * Also sets the database context for workspace-aware databases
     */
    fun setWorkspaceFromDomain(workspace: Workspace) {
        val workspaceContext = WorkspaceContext(
            id = workspace.id,
            name = workspace.name,
            slug = workspace.slug,
            description = workspace.description,
            logoUrl = workspace.avatarUrl, // Use avatarUrl from domain model
            primaryColor = null, // Not available in current domain model
            enabledModules = emptySet(), // Module information comes from separate API
            userRole = null, // User role comes from workspace member data
            permissions = emptySet(), // Permissions come from workspace member data
            settings = mapOf(
                "timezone" to workspace.timezone,
                "language" to workspace.language,
                "subscriptionPlan" to workspace.subscriptionPlan,
                "maxMembers" to workspace.maxMembers,
                "workspaceType" to workspace.workspaceType
            )
        )

        // Set both business context and database context
        WorkspaceContextManager.getInstance().setWorkspaceContext(workspaceContext)

        // Set database context for workspace-aware databases
        val databaseManager = WorkspaceDatabaseManager()
        databaseManager.setCurrentWorkspace(workspace.slug)

        println("WorkspaceContextIntegration: Set workspace context for '${workspace.name}' with slug '${workspace.slug}'")
    }

    /**
     * Enrich workspace context with member information
     * This is called when workspace member data is available to add role and permissions
     */
    fun enrichWorkspaceContextWithMember(member: WorkspaceMember) {
        val currentContext = WorkspaceContextManager.getInstance().currentWorkspace.value
        if (currentContext != null && currentContext.id == member.workspaceId) {
            val enrichedContext = currentContext.copy(
                userRole = member.role,
                permissions = member.permissions.toSet()
            )
            WorkspaceContextManager.getInstance().setWorkspaceContext(enrichedContext)
        }
    }

    /**
     * Clear workspace context when user navigates back to workspace selection
     * or logs out
     */
    fun clearWorkspaceContext() {
        WorkspaceContextManager.getInstance().clearWorkspaceContext()

        // Clear navigation service and installed modules
        GlobalNavigationManager.getInstance().onWorkspaceCleared()

        // Also clear database context
        val databaseManager = WorkspaceDatabaseManager()
        databaseManager.clearWorkspace()

        println("WorkspaceContextIntegration: Cleared all workspace contexts")
    }

    /**
     * Get current workspace ID for API calls
     * Returns null if no workspace is selected
     */
    fun getCurrentWorkspaceId(): String? {
        return WorkspaceContextManager.getInstance().getCurrentWorkspaceId()
    }

    /**
     * Check if workspace context is available
     */
    fun isWorkspaceSelected(): Boolean {
        return WorkspaceContextManager.getInstance().currentWorkspace.value != null
    }

    /**
     * Generate URL-friendly slug from workspace name
     */
    private fun generateSlugFromName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "") // Remove special characters
            .replace(Regex("\\s+"), "-") // Replace spaces with hyphens
            .replace(Regex("-+"), "-") // Remove duplicate hyphens
            .trim('-') // Remove leading/trailing hyphens
    }

    /**
     * Example usage in WorkspaceListScreen onWorkspaceSelected callback:
     *
     * ```kotlin
     * onWorkspaceSelected = { workspaceId ->
     *     // Find the workspace from the list
     *     val selectedWorkspace = workspaceList.find { it.id == workspaceId }
     *     if (selectedWorkspace != null) {
     *         // Set workspace context in app state
     *         WorkspaceContextIntegration.setWorkspaceFromDomain(selectedWorkspace)
     *
     *         // Navigate to modules screen with workspace ID for backward compatibility
     *         navController.navigate(WorkspaceRoute.Modules(workspaceId))
     *     }
     * }
     * ```
     *
     * Example usage in other screens that need workspace context:
     *
     * ```kotlin
     * @Composable
     * fun CustomerListScreen() {
     *     val workspaceId = WorkspaceContextIntegration.getCurrentWorkspaceId()
     *         ?: return // Handle no workspace selected
     *
     *     // Use workspaceId for API calls
     *     val viewModel: CustomerListViewModel = koinInject { parametersOf(workspaceId) }
     *     // ... rest of the screen
     * }
     * ```
     */
}