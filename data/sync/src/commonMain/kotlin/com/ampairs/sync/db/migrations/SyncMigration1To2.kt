package com.ampairs.sync.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val SYNC_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE entity_sync_state ADD COLUMN lastSyncedAtIso TEXT DEFAULT NULL")
    }
}
