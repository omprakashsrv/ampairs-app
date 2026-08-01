package com.ampairs.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Downgrade v2 -> v1 recovery for [com.ampairs.storefront.db.StorefrontAppDatabase].
 *
 * A short-lived build put the storefront-directory cache table (`storefront_directory`) into the
 * auth database at version 2. That cache has since moved to its own
 * [com.ampairs.storefront.db.StorefrontDirectoryDatabase] and the auth DB is back at version 1. Any
 * device that ran the v2 build has `storefront_app.db` on disk at version 2; opening it with v1 code
 * would otherwise crash with "A migration from 2 to 1 was required but not found".
 *
 * This drops the stray cache table (nothing else changed between v1 and v2), bringing the schema
 * back to exactly v1 while leaving the durable auth tables — user / token / session — untouched, so
 * affected users are NOT logged out.
 */
val STOREFRONT_APP_DOWNGRADE_2_1 = object : Migration(2, 1) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `storefront_directory`")
    }
}
