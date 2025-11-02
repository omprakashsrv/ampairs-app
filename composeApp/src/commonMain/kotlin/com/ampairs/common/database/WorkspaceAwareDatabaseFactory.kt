package com.ampairs.common.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.workspace.WorkspaceContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Factory for creating workspace-aware Room databases.
 * Each workspace gets its own isolated database files.
 */
class WorkspaceAwareDatabaseFactory(
    val databasePathProvider: DatabasePathProvider,
    val queryDispatcher: CoroutineDispatcher
) {

    /**
     * Create a workspace-aware Room database
     * @param moduleName The module name (e.g., "customer", "product")
     * @param workspaceSlug Optional workspace slug. If not provided, uses current workspace context
     * @return Room database builder configured for the workspace
     */
    inline fun <reified T : androidx.room.RoomDatabase> createDatabase(
        klass: kotlin.reflect.KClass<T>,
        moduleName: String,
        workspaceSlug: String? = null
    ): T {
        val slug = workspaceSlug ?: WorkspaceContext.getCurrentWorkspaceSlugOrDefault()
        val dbPath = databasePathProvider.getWorkspaceDatabasePath(slug, moduleName)

        return createDatabaseInternal<T>(dbPath)
    }

    inline fun <reified T : androidx.room.RoomDatabase> createDatabaseInternal(dbPath: String): T {
        return createPlatformDatabase<T>(dbPath)
    }
}

expect inline fun <reified T : androidx.room.RoomDatabase> WorkspaceAwareDatabaseFactory.createPlatformDatabase(dbPath: String): T