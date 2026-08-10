package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v4 -> v5 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: adds
 * `payment_voucher.ref_id` — the external-system reference for a payment voucher (the Tally voucher
 * master id captured after an app→Tally push, or the Tally GUID for a voucher pulled in from Tally).
 * Mirrors the `invoiceEntity.ref_id` column added in v3->v4 (see [WORKSPACE_MIGRATION_3_4]). Nullable
 * with no default, so existing rows migrate to NULL; the index mirrors Room's generated schema for
 * the new `payment_voucher_ref_idx`.
 */
val WORKSPACE_MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `payment_voucher` ADD COLUMN `ref_id` TEXT")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `payment_voucher_ref_idx` ON `payment_voucher` (`ref_id`)",
        )
    }
}
