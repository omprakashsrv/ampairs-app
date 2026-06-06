package com.ampairs.invoice.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Invoice schema v1 -> v2 (spec 010): unit-of-measure + base quantity + variant on line items,
 * document-level tax/discount mode, and client-assigned GST number series + sequence. Back-fills
 * base_quantity = quantity; legacy invoices default to the 'DEFAULT' series and tax-exclusive /
 * post-tax-reduction (preserving prior behavior).
 */
val INVOICE_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN price_mode TEXT NOT NULL DEFAULT 'TAX_EXCLUSIVE'")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN overall_discount_mode TEXT NOT NULL DEFAULT 'POST_TAX_REDUCTION'")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN series TEXT NOT NULL DEFAULT 'DEFAULT'")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN sequence_number INTEGER NOT NULL DEFAULT 0")

        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN unit_id TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN base_quantity REAL NOT NULL DEFAULT 0.0")
        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN variant_sku TEXT DEFAULT NULL")
        connection.execSQL("UPDATE invoiceItemEntity SET base_quantity = quantity")
    }
}
