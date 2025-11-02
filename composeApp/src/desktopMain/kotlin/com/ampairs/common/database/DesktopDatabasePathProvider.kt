package com.ampairs.common.database

import getDatabaseDir
import java.io.File

/**
 * Desktop-specific implementation of DatabasePathProvider
 */
class DesktopDatabasePathProvider : DatabasePathProvider {

    override fun getBaseDatabasePath(): String {
        return getDatabaseDir().absolutePath
    }

    override fun getWorkspaceDatabasePath(workspaceSlug: String, moduleName: String): String {
        val workspaceDir = File(getDatabaseDir(), "workspace_$workspaceSlug")
        workspaceDir.mkdirs() // Ensure the workspace directory exists
        return File(workspaceDir, "${moduleName}.db").absolutePath
    }
}