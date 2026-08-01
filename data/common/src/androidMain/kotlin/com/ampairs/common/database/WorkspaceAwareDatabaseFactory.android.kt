package com.ampairs.common.database

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration

actual inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createPlatformDatabase(
    dbPath: String,
    migrations: List<Migration>
): T {
    throw UnsupportedOperationException(
        "Android platform should use createAndroidDatabase() instead of createDatabase(). " +
        "This method requires a Context which is not available in the common database factory."
    )
}
