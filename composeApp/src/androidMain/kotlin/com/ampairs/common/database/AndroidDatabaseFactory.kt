package com.ampairs.common.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.workspace.WorkspaceContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Type-safe Android database creation with scope management.
 * Uses DatabaseScopeManager to cache and properly close databases when switching workspaces.
 */
inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createAndroidDatabase(
    context: Context,
    queryDispatcher: CoroutineDispatcher,
    moduleName: String,
    workspaceSlug: String? = null
): T {
    val slug = workspaceSlug ?: WorkspaceContext.getCurrentWorkspaceSlugOrDefault()
    println("AndroidDatabaseFactory: Creating database for module=$moduleName, workspace=$slug")

    val scopeManager = DatabaseScopeManager.getInstance()

    return scopeManager.getOrCreateDatabase(slug, moduleName) {
        val workspaceDbName = "workspace_${slug}_${moduleName}.db"
        println("AndroidDatabaseFactory: Building Room database: $workspaceDbName")

        Room.databaseBuilder<T>(
            context = context,
            name = context.getDatabasePath(workspaceDbName).absolutePath
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(queryDispatcher)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true) // Only destroy on version downgrades
            .enableMultiInstanceInvalidation() // Support multi-process scenarios
            .build()
    }
}