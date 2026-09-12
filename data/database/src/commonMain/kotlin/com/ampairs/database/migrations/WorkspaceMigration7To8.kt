package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v7 -> v8 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: links the
 * cb_maintenance ticket / PM schedule to the ticket-bucket taxonomy for reporting.
 *
 * - `cb_tickets.ticket_bucket_id` — the exact taxonomy leaf a ticket was classified under.
 * - `cb_pm_schedules.department` — category-level taxonomy link (PM is per-category).
 *
 * Both are server-authoritative and re-sync from the backend `/sync` feeds, so this is a plain
 * add-column. Column names mirror Room's generated schema for the updated entities.
 */
val WORKSPACE_MIGRATION_7_8 = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `cb_tickets` ADD COLUMN `ticket_bucket_id` TEXT")
        connection.execSQL("ALTER TABLE `cb_pm_schedules` ADD COLUMN `department` TEXT NOT NULL DEFAULT ''")
    }
}
