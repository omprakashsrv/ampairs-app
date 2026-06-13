package com.ampairs.order.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Order schema v1 -> v2 (spec 010): unit-of-measure + base quantity + variant on line items, and
 * document-level tax/discount mode. Back-fills base_quantity = quantity for existing lines; legacy
 * documents default to tax-exclusive / post-tax-reduction (preserving prior behavior).
 */
val ORDER_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE orderEntity ADD COLUMN price_mode TEXT NOT NULL DEFAULT 'TAX_EXCLUSIVE'")
        connection.execSQL("ALTER TABLE orderEntity ADD COLUMN overall_discount_mode TEXT NOT NULL DEFAULT 'POST_TAX_REDUCTION'")

        connection.execSQL("ALTER TABLE orderItemEntity ADD COLUMN unit_id TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE orderItemEntity ADD COLUMN base_quantity REAL NOT NULL DEFAULT 0.0")
        connection.execSQL("ALTER TABLE orderItemEntity ADD COLUMN variant_sku TEXT DEFAULT NULL")
        connection.execSQL("UPDATE orderItemEntity SET base_quantity = quantity")
    }
}

/**
 * Order schema v2 -> v3: collapse the from/to customer party pair to a single buyer (customer_*),
 * since the seller is the implicit current workspace. Back-fills the buyer from the legacy
 * to_customer_* columns, then drops the six from/to columns (bundled SQLite supports DROP COLUMN).
 */
val ORDER_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE orderEntity ADD COLUMN customer_id TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE orderEntity ADD COLUMN customer_name TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE orderEntity ADD COLUMN customer_gst TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE orderEntity ADD COLUMN customer_phone TEXT DEFAULT NULL")
        connection.execSQL(
            "UPDATE orderEntity SET customer_id = to_customer_id, customer_name = to_customer_name, customer_gst = to_customer_gst"
        )
        connection.execSQL("ALTER TABLE orderEntity DROP COLUMN from_customer_id")
        connection.execSQL("ALTER TABLE orderEntity DROP COLUMN from_customer_name")
        connection.execSQL("ALTER TABLE orderEntity DROP COLUMN from_customer_gst")
        connection.execSQL("ALTER TABLE orderEntity DROP COLUMN to_customer_id")
        connection.execSQL("ALTER TABLE orderEntity DROP COLUMN to_customer_name")
        connection.execSQL("ALTER TABLE orderEntity DROP COLUMN to_customer_gst")
    }
}
