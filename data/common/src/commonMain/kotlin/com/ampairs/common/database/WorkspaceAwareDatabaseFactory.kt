package com.ampairs.common.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import kotlinx.coroutines.CoroutineDispatcher

class WorkspaceAwareDatabaseFactory(
    val databasePathProvider: DatabasePathProvider,
    val queryDispatcher: CoroutineDispatcher
)

/**
 * Create a fresh Room database for the given workspace, bypassing any DSM caching.
 * Use this in Metro @SingleIn(WorkspaceScope::class) providers — Metro is the cache.
 */
inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createDatabase(
    moduleName: String,
    workspaceSlug: String,
    migrations: List<Migration> = emptyList()
): T {
    val dbPath = databasePathProvider.getWorkspaceDatabasePath(workspaceSlug, moduleName)
    return createPlatformDatabase(dbPath, migrations)
}

expect inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createPlatformDatabase(
    dbPath: String,
    migrations: List<Migration> = emptyList()
): T
