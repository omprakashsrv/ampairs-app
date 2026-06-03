package com.ampairs.common.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Fresh Android database creation — no DSM caching.
 * Use in Metro @SingleIn(WorkspaceScope::class) providers where Metro is the cache.
 */
inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createAndroidDatabase(
    context: Context,
    queryDispatcher: CoroutineDispatcher,
    moduleName: String,
    workspaceSlug: String,
    migrations: List<Migration> = emptyList()
): T {
    val workspaceDbName = "workspace_${workspaceSlug}_${moduleName}.db"
    return Room.databaseBuilder<T>(
        context = context,
        name = context.getDatabasePath(workspaceDbName).absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .apply { if (migrations.isNotEmpty()) addMigrations(*migrations.toTypedArray()) }
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .enableMultiInstanceInvalidation()
        .build()
}
