package com.ampairs.common.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ampairs.common.workspace.WorkspaceContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Android-specific extension for WorkspaceAwareDatabaseFactory
 */
fun WorkspaceAwareDatabaseFactory.createDatabaseForAndroid(
    context: Context,
    queryDispatcher: CoroutineDispatcher,
    moduleName: String,
    workspaceSlug: String? = null
): androidx.room.RoomDatabase {
    val slug = workspaceSlug ?: WorkspaceContext.getCurrentWorkspaceSlugOrDefault()
    val workspaceDbName = "workspace_${slug}_${moduleName}.db"

    return Room.databaseBuilder<androidx.room.RoomDatabase>(
        context = context,
        name = context.getDatabasePath(workspaceDbName).absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .fallbackToDestructiveMigration(true)
        .build()
}

/**
 * Type-safe Android database creation with scope management.
 * Uses DatabaseScopeManager to cache and properly close databases when switching workspaces.
 */
inline fun <reified T : androidx.room.RoomDatabase> WorkspaceAwareDatabaseFactory.createAndroidDatabase(
    klass: kotlin.reflect.KClass<T>,
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
            .fallbackToDestructiveMigration(true)
            .build()
    }
}