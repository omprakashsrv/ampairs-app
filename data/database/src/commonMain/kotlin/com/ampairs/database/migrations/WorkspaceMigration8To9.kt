package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v8 -> v9 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: links a
 * cb_maintenance PM schedule to the EXACT ticket-bucket taxonomy leaf (same granularity a ticket
 * carries), so reports can join PM ↔ ticket on the identical classification.
 *
 * - `cb_pm_schedules.ticket_bucket_id` — the uid of the ticket_bucket leaf the schedule maps to.
 *
 * Server-authoritative and re-syncs from the backend `/sync` feed, so this is a plain add-column.
 */
val WORKSPACE_MIGRATION_8_9 = object : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `cb_pm_schedules` ADD COLUMN `ticket_bucket_id` TEXT")
    }
}
