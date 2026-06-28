package com.ampairs.product.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds the `product_standard_cost` table — effective-dated standard purchase costs
 * ([com.ampairs.product.db.entity.ProductStandardCostEntity]). New table, so this CREATEs it
 * (matching Room's generated shape: TEXT for String, REAL for Double, INTEGER for Boolean; String
 * primary key on `id`) plus the unique id index and the product_id lookup index. Index names match
 * the [androidx.room.Index] names declared on the entity so Room's schema validation passes.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `product_standard_cost` (" +
                "`id` TEXT NOT NULL, " +
                "`ref_id` TEXT, " +
                "`product_id` TEXT NOT NULL, " +
                "`variant_sku` TEXT, " +
                "`cost_price` REAL NOT NULL, " +
                "`effective_from` TEXT, " +
                "`effective_to` TEXT, " +
                "`active` INTEGER NOT NULL, " +
                "`synced` INTEGER NOT NULL, " +
                "`created_at` TEXT, " +
                "`updated_at` TEXT, " +
                "PRIMARY KEY(`id`))"
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `product_standard_cost_id_idx` " +
                "ON `product_standard_cost` (`id`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `product_standard_cost_product_idx` " +
                "ON `product_standard_cost` (`product_id`)"
        )
    }
}
