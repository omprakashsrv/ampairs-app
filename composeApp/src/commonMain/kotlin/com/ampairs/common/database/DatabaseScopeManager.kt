package com.ampairs.common.database

import androidx.room.RoomDatabase
import com.ampairs.common.concurrency.Volatile
import com.ampairs.common.concurrency.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages the lifecycle of workspace-scoped database instances.
 * Handles database creation, caching, and cleanup when switching workspaces.
 */
class DatabaseScopeManager private constructor() {

    // Cache of database instances keyed by "workspaceSlug:moduleName"
    private val databaseCache = mutableMapOf<String, RoomDatabase>()

    companion object {
        @Volatile
        private var INSTANCE: DatabaseScopeManager? = null

        fun getInstance(): DatabaseScopeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseScopeManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Get or create a database instance for the given workspace and module.
     * @param workspaceSlug The workspace identifier
     * @param moduleName The module name (e.g., "customer", "product")
     * @param databaseFactory Function to create a new database instance if not cached
     * @return The database instance
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : RoomDatabase> getOrCreateDatabase(
        workspaceSlug: String,
        moduleName: String,
        databaseFactory: () -> T
    ): T {
        val key = "$workspaceSlug:$moduleName"

        return synchronized(databaseCache) {
            val existing = databaseCache[key]
            if (existing != null) {
                println("DatabaseScopeManager: ✅ Returning cached database for key: $key")
                existing as T
            } else {
                println("DatabaseScopeManager: 🆕 Creating NEW database for key: $key")
                val newDb = databaseFactory()
                databaseCache[key] = newDb
                println("DatabaseScopeManager: Cache now contains ${databaseCache.size} databases: ${databaseCache.keys}")
                newDb
            }
        }
    }

    /**
     * Close and remove all database instances for a specific workspace.
     * Should be called when switching workspaces.
     * @param workspaceSlug The workspace identifier to clear
     */
    fun clearWorkspaceDatabases(workspaceSlug: String) {
        synchronized(databaseCache) {
            println("DatabaseScopeManager: 🧹 Clearing databases for workspace: $workspaceSlug")
            println("DatabaseScopeManager: Cache before clear: ${databaseCache.keys}")

            val keysToRemove = databaseCache.keys.filter { it.startsWith("$workspaceSlug:") }
            println("DatabaseScopeManager: Keys to remove: $keysToRemove")

            keysToRemove.forEach { key ->
                val database = databaseCache.remove(key)
                database?.let { db ->
                    // Close database asynchronously to avoid blocking
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            db.close()
                            println("DatabaseScopeManager: ✅ Closed database: $key")
                        } catch (e: Exception) {
                            // Log error but don't fail - database cleanup is best-effort
                            println("DatabaseScopeManager: ❌ Failed to close database $key: ${e.message}")
                        }
                    }
                }
            }

            println("DatabaseScopeManager: Cache after clear: ${databaseCache.keys}")
        }
    }

    /**
     * Close and remove all database instances.
     * Should be called on app shutdown or logout.
     */
    fun clearAllDatabases() {
        synchronized(databaseCache) {
            databaseCache.values.forEach { database ->
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        database.close()
                    } catch (e: Exception) {
                        println("Warning: Failed to close database: ${e.message}")
                    }
                }
            }
            databaseCache.clear()
        }
    }

    /**
     * Get the current number of cached database instances (for debugging)
     */
    fun getCachedDatabaseCount(): Int = synchronized(databaseCache) {
        databaseCache.size
    }
}
