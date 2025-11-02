package com.ampairs.common.database

/**
 * Provides workspace-aware database paths for Room databases.
 * Each workspace gets its own isolated database files using the workspace slug.
 */
interface DatabasePathProvider {
    /**
     * Get the base database directory path for the platform
     */
    fun getBaseDatabasePath(): String

    /**
     * Get workspace-specific database path for a module
     * @param workspaceSlug The workspace slug for data isolation
     * @param moduleName The module name (e.g., "customer", "product")
     * @return Full path to the workspace-specific database file
     */
    fun getWorkspaceDatabasePath(workspaceSlug: String, moduleName: String): String {
        val basePath = getBaseDatabasePath()
        return "$basePath/workspace_$workspaceSlug/${moduleName}.db"
    }
}