package com.ampairs.common.database

import androidx.room.RoomDatabase

// Note: Android should use createAndroidDatabase instead of this method
// This is only provided for completeness but should not be used
actual inline fun <reified T : RoomDatabase> WorkspaceAwareDatabaseFactory.createPlatformDatabase(dbPath: String): T {
    throw UnsupportedOperationException(
        "Android platform should use createAndroidDatabase() instead of createDatabase(). " +
        "This method requires a Context which is not available in the common database factory."
    )
}