package com.ampairs.product.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE productEntity ADD COLUMN ref_id TEXT DEFAULT NULL")
        connection.execSQL("CREATE INDEX IF NOT EXISTS product_ref_idx ON productEntity(ref_id)")

        connection.execSQL("ALTER TABLE groupEntity ADD COLUMN ref_id TEXT DEFAULT NULL")
        connection.execSQL("CREATE INDEX IF NOT EXISTS group_ref_idx ON groupEntity(ref_id)")

        connection.execSQL("ALTER TABLE categoryEntity ADD COLUMN ref_id TEXT DEFAULT NULL")
        connection.execSQL("CREATE INDEX IF NOT EXISTS category_ref_idx ON categoryEntity(ref_id)")
    }
}
