package com.ampairs.business.data.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val BUSINESS_MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP INDEX IF EXISTS index_business_profile_workspace_id")
        connection.execSQL("ALTER TABLE business_profile DROP COLUMN workspace_id")
    }
}
