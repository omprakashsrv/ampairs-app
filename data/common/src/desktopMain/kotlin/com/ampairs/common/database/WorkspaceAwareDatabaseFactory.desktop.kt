package com.ampairs.common.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createPlatformDatabase(
    dbPath: String,
    migrations: List<Migration>
): T {
    return Room.databaseBuilder<T>(name = dbPath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryDispatcher)
        .apply { if (migrations.isNotEmpty()) addMigrations(*migrations.toTypedArray()) }
        .build()
}
