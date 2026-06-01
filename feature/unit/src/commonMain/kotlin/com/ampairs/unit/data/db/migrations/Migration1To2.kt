package com.ampairs.unit.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val UNIT_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE units ADD COLUMN ref_id TEXT DEFAULT NULL")
        connection.execSQL("CREATE INDEX IF NOT EXISTS unit_ref_idx ON units(ref_id)")
    }
}
