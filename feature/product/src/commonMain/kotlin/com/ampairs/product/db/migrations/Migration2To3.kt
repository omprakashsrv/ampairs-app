package com.ampairs.product.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Database Migration v2 → v3
 *
 * Changes:
 * 1. Add description, stock_quantity, low_stock_alert columns to productEntity table
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // Add new columns to productEntity table
        connection.execSQL(
            """
            ALTER TABLE productEntity
            ADD COLUMN description TEXT DEFAULT NULL
            """.trimIndent()
        )

        connection.execSQL(
            """
            ALTER TABLE productEntity
            ADD COLUMN stock_quantity REAL DEFAULT NULL
            """.trimIndent()
        )

        connection.execSQL(
            """
            ALTER TABLE productEntity
            ADD COLUMN low_stock_alert REAL DEFAULT NULL
            """.trimIndent()
        )
    }
}
