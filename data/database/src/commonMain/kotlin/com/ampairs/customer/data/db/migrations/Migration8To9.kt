package com.ampairs.customer.data.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds local persistence for custom attribute values (spec 011, US1). The schema-driven customer
 * form stores CUSTOM field values in `Customer.attributes`; without this column they were dropped
 * on the Room round-trip (edit reload / sync push read from the entity).
 */
val CUSTOMER_MIGRATION_8_9 = object : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `customers` ADD COLUMN `attributes_json` TEXT")
    }
}
