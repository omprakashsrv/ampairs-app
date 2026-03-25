package com.ampairs.common.database

import android.content.Context

/**
 * Android-specific implementation of DatabasePathProvider
 */
class AndroidDatabasePathProvider(private val context: Context) : DatabasePathProvider {

    override fun getBaseDatabasePath(): String {
        // Use Android's database directory
        return context.getDatabasePath("dummy").parent ?: context.filesDir.absolutePath
    }

    override fun getWorkspaceDatabasePath(workspaceSlug: String, moduleName: String): String {
        // For Android, we need to use getDatabasePath for proper handling
        val workspaceDbName = "workspace_${workspaceSlug}_${moduleName}.db"
        return context.getDatabasePath(workspaceDbName).absolutePath
    }
}