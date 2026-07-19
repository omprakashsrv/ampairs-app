package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 -> v2 for the storefront apps' consolidated [com.ampairs.storefront.db.StorefrontAppDatabase]:
 * adds the `storefront_directory` offline cache so the multi-store picker survives offline. Pure
 * additive CREATE TABLE — existing auth rows are untouched. The SQL mirrors exactly what Room
 * generates for `StorefrontDirectoryEntity`; keep them in sync on any column change.
 */
val STOREFRONT_APP_MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `storefront_directory` (" +
                "`slug` TEXT NOT NULL, " +
                "`uid` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`description` TEXT, " +
                "`logo_url` TEXT, " +
                "`banner_url` TEXT, " +
                "`status` TEXT NOT NULL, " +
                "`access_mode` TEXT, " +
                "`brand_color_argb` INTEGER, " +
                "`position` INTEGER NOT NULL, " +
                "`cached_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`slug`))"
        )
    }
}
