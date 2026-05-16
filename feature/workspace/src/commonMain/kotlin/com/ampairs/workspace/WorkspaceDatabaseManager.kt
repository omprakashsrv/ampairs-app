package com.ampairs.workspace

import com.ampairs.common.workspace.WorkspaceContext
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages workspace switching and database context.
 * Call setCurrentWorkspace() before accessing any module databases.
 */
class WorkspaceDatabaseManager {

    /**
     * Set the current workspace and update database context.
     * This should be called when:
     * 1. User logs in and selects a workspace
     * 2. User switches between workspaces
     * 3. Before accessing any module databases
     *
     * @param workspaceSlug The workspace slug from Workspace.slug
     */
    fun setCurrentWorkspace(workspaceSlug: String) {
        WorkspaceContext.setCurrentWorkspace(workspaceSlug)
    }

    /**
     * Clear the current workspace context.
     * Call this when user logs out.
     */
    fun clearWorkspace() {
        WorkspaceContext.setCurrentWorkspace(null)
    }

    /**
     * Get the current workspace slug
     */
    fun getCurrentWorkspaceSlug(): String? {
        return WorkspaceContext.currentWorkspaceSlug.value
    }

    /**
     * Observable current workspace slug
     */
    val currentWorkspaceSlug: StateFlow<String?> = WorkspaceContext.currentWorkspaceSlug

    /**
     * Check if a workspace is currently set
     */
    fun hasCurrentWorkspace(): Boolean {
        return WorkspaceContext.hasCurrentWorkspace()
    }

    /**
     * Get workspace-specific database path for debugging
     * @param moduleName The module name (e.g., "customer", "product")
     */
    fun getWorkspaceDatabasePath(moduleName: String): String? {
        val slug = getCurrentWorkspaceSlug() ?: return null
        // This is for debugging only - actual paths are handled by the factory
        return "workspace_${slug}/${moduleName}.db"
    }
}