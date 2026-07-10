package com.ampairs.invoice.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Invoice schema v1 -> v2 (spec 010): unit-of-measure + base quantity + variant on line items,
 * document-level tax/discount mode, and client-assigned GST number series + sequence. Back-fills
 * base_quantity = quantity; legacy invoices default to the 'DEFAULT' series and tax-exclusive /
 * post-tax-reduction (preserving prior behavior).
 */
val INVOICE_MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
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

/**
 * Invoice schema v2 -> v3: collapse the from/to customer party pair to a single buyer (customer_*),
 * since the seller is the implicit current workspace. Back-fills the buyer from the legacy
 * to_customer_* columns, then drops the six from/to columns (bundled SQLite supports DROP COLUMN).
 */
val INVOICE_MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN customer_id TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN customer_name TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN customer_gst TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN customer_phone TEXT DEFAULT NULL")
        connection.execSQL(
            "UPDATE invoiceEntity SET customer_id = to_customer_id, customer_name = to_customer_name, customer_gst = to_customer_gst"
        )
        connection.execSQL("ALTER TABLE invoiceEntity DROP COLUMN from_customer_id")
        connection.execSQL("ALTER TABLE invoiceEntity DROP COLUMN from_customer_name")
        connection.execSQL("ALTER TABLE invoiceEntity DROP COLUMN from_customer_gst")
        connection.execSQL("ALTER TABLE invoiceEntity DROP COLUMN to_customer_id")
        connection.execSQL("ALTER TABLE invoiceEntity DROP COLUMN to_customer_name")
        connection.execSQL("ALTER TABLE invoiceEntity DROP COLUMN to_customer_gst")
    }
}

/** Invoice schema v3 -> v4: snapshot the seller (issuing business) identity onto the document. */
val INVOICE_MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN seller_name TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN seller_address TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN seller_gst TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN place_of_supply TEXT DEFAULT NULL")
    }
}

/** Invoice schema v4 -> v5: seller's place of supply (origin state) for the IGST-vs-CGST/SGST decision. */
val INVOICE_MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE invoiceEntity ADD COLUMN seller_place_of_supply TEXT DEFAULT NULL")
    }
}

/** Invoice schema v5 -> v6 (spec 009): client-resolved pricing snapshot on each line, pushed verbatim on /sync. */
val INVOICE_MIGRATION_5_6 = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN resolved_unit_price_minor INTEGER DEFAULT NULL")
        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN currency TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN price_source TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN matched_price_list_uid TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN applied_tier_min_qty REAL DEFAULT NULL")
        connection.execSQL("ALTER TABLE invoiceItemEntity ADD COLUMN below_moq INTEGER NOT NULL DEFAULT 0")
    }
}
