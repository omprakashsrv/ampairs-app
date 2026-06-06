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
