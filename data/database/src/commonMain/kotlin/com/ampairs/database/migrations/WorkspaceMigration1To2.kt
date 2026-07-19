package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 -> v2 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: adds
 * `ecom_order.order_number`, the human-friendly, gap-free order number the buyer sees/quotes to
 * identify or track their order (backend: ecom migration V1.0.119). Existing local rows default
 * to "" until the next pull refreshes them from the server.
 */
val WORKSPACE_MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `ecom_order` ADD COLUMN `order_number` TEXT NOT NULL DEFAULT ''")
    }
}
