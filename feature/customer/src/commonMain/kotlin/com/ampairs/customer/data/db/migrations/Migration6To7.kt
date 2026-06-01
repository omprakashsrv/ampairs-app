package com.ampairs.customer.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val CUSTOMER_MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE customers ADD COLUMN ref_id TEXT DEFAULT NULL")
        connection.execSQL("CREATE INDEX IF NOT EXISTS customer_ref_idx ON customers(ref_id)")

        connection.execSQL("ALTER TABLE customer_groups ADD COLUMN ref_id TEXT DEFAULT NULL")
        connection.execSQL("CREATE INDEX IF NOT EXISTS customer_group_ref_idx ON customer_groups(ref_id)")
    }
}
