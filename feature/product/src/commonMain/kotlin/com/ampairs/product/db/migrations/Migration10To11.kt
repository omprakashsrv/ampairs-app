package com.ampairs.product.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v10 → v11: introduces the `product_fts` FTS4 index over `productEntity` (full-text product search
 * by name / code / description).
 *
 * Creates the FTS virtual table, the four Room content-sync triggers that keep it in step with
 * `productEntity`, and back-fills the index from existing rows via the FTS `'rebuild'` command. No
 * `ALTER TABLE` is needed — the indexed columns already exist on `productEntity`.
 *
 * IMPORTANT (verify before merge): the DDL below mirrors Room's deterministic `@Fts4(contentEntity)`
 * output for [com.ampairs.product.db.entity.ProductFts]. The table options and the four
 * `room_fts_content_sync_product_fts_*` trigger names/bodies MUST match the generated v11 schema
 * (`schemas/.../11.json`) exactly, or Room's open-time schema validation will throw. After the first
 * successful build that emits 11.json, diff this block against it and reconcile any difference.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `product_fts` USING FTS4(" +
                "`name` TEXT NOT NULL, `code` TEXT NOT NULL, `description` TEXT, " +
                "tokenize=unicode61, content=`productEntity`)"
        )

        connection.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_product_fts_BEFORE_UPDATE " +
                "BEFORE UPDATE ON `productEntity` BEGIN " +
                "DELETE FROM `product_fts` WHERE `docid`=OLD.`rowid`; END"
        )
        connection.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_product_fts_BEFORE_DELETE " +
                "BEFORE DELETE ON `productEntity` BEGIN " +
                "DELETE FROM `product_fts` WHERE `docid`=OLD.`rowid`; END"
        )
        connection.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_product_fts_AFTER_UPDATE " +
                "AFTER UPDATE ON `productEntity` BEGIN " +
                "INSERT INTO `product_fts`(`docid`, `name`, `code`, `description`) " +
                "VALUES (NEW.`rowid`, NEW.`name`, NEW.`code`, NEW.`description`); END"
        )
        connection.execSQL(
            "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_product_fts_AFTER_INSERT " +
                "AFTER INSERT ON `productEntity` BEGIN " +
                "INSERT INTO `product_fts`(`docid`, `name`, `code`, `description`) " +
                "VALUES (NEW.`rowid`, NEW.`name`, NEW.`code`, NEW.`description`); END"
        )

        connection.execSQL("INSERT INTO `product_fts`(`product_fts`) VALUES('rebuild')")
    }
}
