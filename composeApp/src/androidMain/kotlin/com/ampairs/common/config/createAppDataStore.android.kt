package com.ampairs.common.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ampairs.common.workspace.WorkspaceContext
import java.io.File

/**
 * Creates a workspace-aware DataStore instance for Android platform
 *
 * Each workspace has its own preferences (sync times, workspace-specific settings)
 * Theme preference could be shared or workspace-specific based on requirements
 *
 * Uses DataStoreManager to ensure singleton behavior per workspace.
 */
fun createAppDataStore(context: Context, workspaceSlug: String? = null): DataStore<Preferences> {
    val slug = workspaceSlug ?: WorkspaceContext.getCurrentWorkspaceSlugOrDefault()

    return DataStoreManager.getOrCreateDataStore(slug) { workspaceKey ->
        createAppDataStore(
            producePath = {
                val prefsDir = File(context.filesDir, "preferences")
                val workspacePrefsDir = File(prefsDir, "workspace_$workspaceKey")
                workspacePrefsDir.mkdirs()

                File(workspacePrefsDir, appDataStoreFileName).absolutePath
            }
        )
    }
}