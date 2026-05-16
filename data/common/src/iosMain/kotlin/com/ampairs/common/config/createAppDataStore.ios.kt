package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ampairs.common.workspace.WorkspaceContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * Creates a workspace-aware DataStore instance for iOS platform
 *
 * Each workspace has its own preferences (sync times, workspace-specific settings)
 * Theme preference could be shared or workspace-specific based on requirements
 *
 * Uses DataStoreManager to ensure singleton behavior per workspace.
 */
@OptIn(ExperimentalForeignApi::class)
fun createAppDataStore(workspaceSlug: String? = null): DataStore<Preferences> {
    val slug = workspaceSlug ?: WorkspaceContext.getCurrentWorkspaceSlugOrDefault()

    return DataStoreManager.getOrCreateDataStore(slug) { workspaceKey ->
        createAppDataStore(
            producePath = {
                val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null,
                )
                val baseDir = requireNotNull(documentDirectory).path
                val prefsDir = "$baseDir/preferences"
                val workspacePrefsDir = "$prefsDir/workspace_$workspaceKey"

                // Create directory structure
                val fileManager = NSFileManager.defaultManager
                fileManager.createDirectoryAtPath(
                    path = workspacePrefsDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )

                "$workspacePrefsDir/$appDataStoreFileName"
            }
        )
    }
}