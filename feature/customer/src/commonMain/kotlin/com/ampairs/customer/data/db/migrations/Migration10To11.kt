package com.ampairs.customer.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v10 → v11: introduces the `customer_fts` FTS4 index over `customers` (full-text customer search).
 *
 * Creates the FTS virtual table, the four Room content-sync triggers that keep it in step with
 * `customers`, and back-fills the index from existing rows via the FTS `'rebuild'` command. No
 * `ALTER TABLE` is needed — the FTS columns (name/email/phone/gstNumber/address/street/city/state)
 * all already exist on `customers`.
 *
 * IMPORTANT (verify before merge): the DDL below mirrors Room's deterministic `@Fts4(contentEntity)`
 * output for [com.ampairs.customer.data.db.CustomerFts]. The table options and the four
 * `room_fts_content_sync_customer_fts_*` trigger names/bodies MUST match the generated v11 schema
 * (`schemas/.../11.json`) exactly, or Room's open-time schema validation will throw. After the first
 * successful build that emits 11.json, diff this block against it and reconcile any difference.
 */
val CUSTOMER_MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        // FTS4 external-content virtual table indexing the searchable customer columns.
        connection.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `customer_fts` USING FTS4(" +
                "`name` TEXT NOT NULL, `email` TEXT, `phone` TEXT, `gstNumber` TEXT, " +
                "`address` TEXT, `street` TEXT, `city` TEXT, `state` TEXT, " +
                "tokenize=unicode61, content=`customers`)"
        )

        // Content-sync triggers (Room maintains these automatically on a fresh install; a migration
        // must recreate them so the index stays current on every customers insert/update/delete).
        connection.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_customer_fts_BEFORE_UPDATE " +
                "BEFORE UPDATE ON `customers` BEGIN " +
                "DELETE FROM `customer_fts` WHERE `docid`=OLD.`rowid`; END"
        )
        connection.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_customer_fts_BEFORE_DELETE " +
                "BEFORE DELETE ON `customers` BEGIN " +
                "DELETE FROM `customer_fts` WHERE `docid`=OLD.`rowid`; END"
        )
        connection.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_customer_fts_AFTER_UPDATE " +
                "AFTER UPDATE ON `customers` BEGIN " +
                "INSERT INTO `customer_fts`(`docid`, `name`, `email`, `phone`, `gstNumber`, " +
                "`address`, `street`, `city`, `state`) VALUES (NEW.`rowid`, NEW.`name`, NEW.`email`, " +
                "NEW.`phone`, NEW.`gstNumber`, NEW.`address`, NEW.`street`, NEW.`city`, NEW.`state`); END"
        )
        connection.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_customer_fts_AFTER_INSERT " +
                "AFTER INSERT ON `customers` BEGIN " +
                "INSERT INTO `customer_fts`(`docid`, `name`, `email`, `phone`, `gstNumber`, " +
                "`address`, `street`, `city`, `state`) VALUES (NEW.`rowid`, NEW.`name`, NEW.`email`, " +
                "NEW.`phone`, NEW.`gstNumber`, NEW.`address`, NEW.`street`, NEW.`city`, NEW.`state`); END"
        )

        // Back-fill the index from existing customers in one pass.
        connection.execSQL("INSERT INTO `customer_fts`(`customer_fts`) VALUES('rebuild')")
    }
}
