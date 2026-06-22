package com.ampairs.ecom.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 -> v2: add optional geolocation columns to the saved address, captured when the buyer picks
 * the address on the map. Nullable REAL — existing rows stay null.
 */
val ECOM_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE customer_address ADD COLUMN latitude REAL")
        connection.execSQL("ALTER TABLE customer_address ADD COLUMN longitude REAL")
    }
}
