package com.ampairs.product.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds local persistence for custom attribute values (spec 011, US5). The schema-driven product
 * form stores CUSTOM field values in `Product.attributes`; this column carries them through the
 * Room round-trip (edit reload / sync push read from the entity).
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `productEntity` ADD COLUMN `attributes_json` TEXT")
    }
}
