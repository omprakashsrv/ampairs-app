package com.ampairs.pricing.data.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 -> v2: add effective-dating columns to price-list items (effective-dated pricing / Tally's
 * "Applicable From"). Nullable TEXT (ISO-8601, UTC) — existing rows stay null, i.e. effective from
 * the beginning and open-ended, which preserves current resolution behavior.
 */
val PRICING_MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE price_list_items ADD COLUMN effective_from TEXT")
        connection.execSQL("ALTER TABLE price_list_items ADD COLUMN effective_to TEXT")
    }
}
