package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v3 -> v4 for the consolidated [com.ampairs.database.AmpairsWorkspaceDatabase]: adds
 * `invoiceEntity.ref_id` — the external-system reference for an invoice (the Tally voucher master id
 * captured after an app→Tally push, or the Tally GUID for a pulled-in voucher). It mirrors the
 * existing `ref_id` on customer/product/supplier and round-trips through the invoice `/sync`
 * contract. Nullable with no default, so existing rows migrate to NULL; the index mirrors Room's
 * generated schema for the new `invoice_ref_idx`.
 */
val WORKSPACE_MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `invoiceEntity` ADD COLUMN `ref_id` TEXT")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `invoice_ref_idx` ON `invoiceEntity` (`ref_id`)",
        )
    }
}
