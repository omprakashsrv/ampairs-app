package com.ampairs.business.data.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v3 → v4: tax identity/config moved to the tax module. Drop the business profile's tax columns.
 */
val BUSINESS_MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE business_profile DROP COLUMN tax_id")
        connection.execSQL("ALTER TABLE business_profile DROP COLUMN registration_number")
        connection.execSQL("ALTER TABLE business_profile DROP COLUMN tax_settings_json")
    }
}
