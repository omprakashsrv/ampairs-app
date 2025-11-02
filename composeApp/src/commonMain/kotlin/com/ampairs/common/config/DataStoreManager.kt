package com.ampairs.common.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Manages DataStore instances per workspace to ensure singleton behavior per workspace.
 * Similar to DatabaseScopeManager, this prevents multiple DataStore instances
 * from being created for the same file.
 */
object DataStoreManager {
    private val dataStores = mutableMapOf<String, DataStore<Preferences>>()

    /**
     * Get or create a DataStore instance for the given workspace.
     *
     * @param workspaceSlug The workspace identifier
     * @param producer Function that creates a new DataStore instance
     * @return Cached or newly created DataStore instance
     */
    fun getOrCreateDataStore(
        workspaceSlug: String,
        producer: (String) -> DataStore<Preferences>
    ): DataStore<Preferences> {
        return synchronized(this) {
            dataStores.getOrPut(workspaceSlug) {
                producer(workspaceSlug)
            }
        }
    }

    /**
     * Clear all DataStore instances for a specific workspace.
     * Should be called when switching workspaces.
     */
    fun clearDataStoresForWorkspace(workspaceSlug: String) {
        synchronized(this) {
            dataStores.remove(workspaceSlug)
        }
    }

    /**
     * Clear all cached DataStore instances.
     * Should be called on logout or workspace list refresh.
     */
    fun clearAll() {
        synchronized(this) {
            dataStores.clear()
        }
    }
}
