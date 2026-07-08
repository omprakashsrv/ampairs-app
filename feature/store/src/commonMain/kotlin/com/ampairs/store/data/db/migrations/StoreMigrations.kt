package com.ampairs.store.data.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** v1 → v2: add the pull-only setting-definition cache table. */
val STORE_MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS store_setting_definitions (
                id TEXT NOT NULL PRIMARY KEY,
                module TEXT NOT NULL,
                setting_key TEXT NOT NULL,
                value_type TEXT NOT NULL,
                default_value TEXT NOT NULL,
                allowed_values TEXT NOT NULL,
                label TEXT NOT NULL,
                description TEXT
            )
            """.trimIndent()
        )
    }
}
