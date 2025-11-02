package com.ampairs.common.database

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific implementation of DatabasePathProvider
 */
@OptIn(ExperimentalForeignApi::class)
class IosDatabasePathProvider : DatabasePathProvider {

    override fun getBaseDatabasePath(): String {
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String
        return documentsPath
    }

    override fun getWorkspaceDatabasePath(workspaceSlug: String, moduleName: String): String {
        val documentsPath = getBaseDatabasePath()
        val workspaceDir = "$documentsPath/workspace_$workspaceSlug"

        // Create workspace directory if it doesn't exist
        val fileManager = NSFileManager.defaultManager
        fileManager.createDirectoryAtPath(
            workspaceDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )

        return "$workspaceDir/${moduleName}.db"
    }
}