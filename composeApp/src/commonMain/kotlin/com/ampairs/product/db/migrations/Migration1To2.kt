package com.ampairs.product.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Database Migration v1 → v2
 *
 * Changes:
 * 1. Add product_type, service_type, has_variants columns to productEntity table
 * 2. Create product_variants table with foreign key to productEntity
 * 3. Create variant_attributes table for searchable attributes
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // 1. Add new columns to productEntity table
        connection.execSQL(
            """
            ALTER TABLE productEntity
            ADD COLUMN product_type TEXT DEFAULT NULL
            """.trimIndent()
        )

        connection.execSQL(
            """
            ALTER TABLE productEntity
            ADD COLUMN service_type TEXT DEFAULT NULL
            """.trimIndent()
        )

        connection.execSQL(
            """
            ALTER TABLE productEntity
            ADD COLUMN has_variants INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )

        // 2. Create product_variants table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS product_variants (
                id TEXT NOT NULL PRIMARY KEY,
                product_id TEXT NOT NULL,
                sku TEXT NOT NULL UNIQUE,
                variant_name TEXT NOT NULL,
                size TEXT,
                color TEXT,
                material TEXT,
                mrp REAL,
                dealer_price REAL,
                selling_price REAL,
                stock INTEGER NOT NULL DEFAULT 0,
                active INTEGER NOT NULL DEFAULT 1,
                synced INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(product_id) REFERENCES productEntity(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Create indices for product_variants
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_product_variants_product_id
            ON product_variants(product_id)
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_product_variants_sku
            ON product_variants(sku)
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_product_variants_active
            ON product_variants(active)
            """.trimIndent()
        )

        // 3. Create variant_attributes table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS variant_attributes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                product_id TEXT NOT NULL,
                attribute_type TEXT NOT NULL,
                attribute_value TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(product_id) REFERENCES productEntity(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Create indices for variant_attributes
        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_variant_attributes_product_id
            ON variant_attributes(product_id)
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_variant_attributes_attribute_type
            ON variant_attributes(attribute_type)
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_variant_attributes_attribute_value
            ON variant_attributes(attribute_value)
            """.trimIndent()
        )
    }
}
