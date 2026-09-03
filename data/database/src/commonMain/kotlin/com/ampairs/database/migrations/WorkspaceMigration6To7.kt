package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v6 -> v7 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: adds the
 * `cb_ticket_buckets` table (customer-specific `cb_*` maintenance build). The ticket-classification
 * taxonomy is server-authoritative and re-syncs from the backend `/sync` feed, so this is a pure
 * create — no data mapping. Column shapes + index names mirror Room's generated schema for
 * `TicketBucketEntity`.
 */
val WORKSPACE_MIGRATION_6_7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `cb_ticket_buckets` (" +
                "`id` TEXT NOT NULL, `department` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                "`sub_category_1` TEXT NOT NULL, `sub_category_2` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL, `synced` INTEGER NOT NULL, `created_at` TEXT, " +
                "`updated_at` TEXT, `ref_id` TEXT, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `cb_ticket_bucket_id_idx` ON `cb_ticket_buckets` (`id`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `cb_ticket_bucket_dept_idx` ON `cb_ticket_buckets` (`department`)",
        )
    }
}
