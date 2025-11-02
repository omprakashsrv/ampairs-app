package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ampairs.common.workspace.WorkspaceContext
import java.io.File

/**
 * Creates a workspace-aware DataStore instance for Desktop platform
 *
 * Each workspace has its own preferences (sync times, workspace-specific settings)
 * Theme preference could be shared or workspace-specific based on requirements
 *
 * Uses DataStoreManager to ensure singleton behavior per workspace.
 */
fun createAppDataStore(workspaceSlug: String? = null): DataStore<Preferences> {
    val slug = workspaceSlug ?: WorkspaceContext.getCurrentWorkspaceSlugOrDefault()

    return DataStoreManager.getOrCreateDataStore(slug) { workspaceKey ->
        createAppDataStore(
            producePath = {
                try {
                    // Use DataDirectoryManager for consistent data location
                    val prefsDir = com.ampairs.common.desktop.DataDirectoryManager.getPreferencesDir()
                    val workspacePrefsDir = File(prefsDir, "workspace_$workspaceKey")
                    workspacePrefsDir.mkdirs()

                    File(workspacePrefsDir, appDataStoreFileName).absolutePath
                } catch (_: IllegalStateException) {
                    // Fallback to user.home if data directory not set (initialization phase)
                    val fallbackDir = File(System.getProperty("user.home"), ".ampairs/preferences/workspace_$workspaceKey")
                    fallbackDir.mkdirs()
                    println("WARNING: Using fallback preferences directory for workspace $workspaceKey: ${fallbackDir.absolutePath}")
                    File(fallbackDir, appDataStoreFileName).absolutePath
                }
            }
        )
    }
}