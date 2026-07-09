package com.ampairs.product.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_6_7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // Recreate brandEntity without image_id
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `brandEntity_new` (
                `seq_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `active` INTEGER NOT NULL DEFAULT 1,
                `soft_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced` INTEGER NOT NULL DEFAULT 0
            )
        """)
        connection.execSQL("INSERT INTO `brandEntity_new` (seq_id, id, name, active, soft_deleted, synced) SELECT seq_id, id, name, active, soft_deleted, synced FROM brandEntity")
        connection.execSQL("DROP TABLE `brandEntity`")
        connection.execSQL("ALTER TABLE `brandEntity_new` RENAME TO `brandEntity`")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `brand_idx` ON `brandEntity`(`id`)")

        // Recreate groupEntity without image_id
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `groupEntity_new` (
                `seq_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `ref_id` TEXT,
                `active` INTEGER NOT NULL DEFAULT 1,
                `soft_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced` INTEGER NOT NULL DEFAULT 0
            )
        """)
        connection.execSQL("INSERT INTO `groupEntity_new` (seq_id, id, name, ref_id, active, soft_deleted, synced) SELECT seq_id, id, name, ref_id, active, soft_deleted, synced FROM groupEntity")
        connection.execSQL("DROP TABLE `groupEntity`")
        connection.execSQL("ALTER TABLE `groupEntity_new` RENAME TO `groupEntity`")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `group_idx` ON `groupEntity`(`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `group_ref_idx` ON `groupEntity`(`ref_id`)")

        // Recreate categoryEntity without image_id
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `categoryEntity_new` (
                `seq_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `ref_id` TEXT,
                `active` INTEGER NOT NULL DEFAULT 1,
                `soft_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced` INTEGER NOT NULL DEFAULT 0
            )
        """)
        connection.execSQL("INSERT INTO `categoryEntity_new` (seq_id, id, name, ref_id, active, soft_deleted, synced) SELECT seq_id, id, name, ref_id, active, soft_deleted, synced FROM categoryEntity")
        connection.execSQL("DROP TABLE `categoryEntity`")
        connection.execSQL("ALTER TABLE `categoryEntity_new` RENAME TO `categoryEntity`")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `category_idx` ON `categoryEntity`(`id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `category_ref_idx` ON `categoryEntity`(`ref_id`)")

        // Recreate subCategoryEntity without image_id
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `subCategoryEntity_new` (
                `seq_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `active` INTEGER NOT NULL DEFAULT 1,
                `soft_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced` INTEGER NOT NULL DEFAULT 0
            )
        """)
        connection.execSQL("INSERT INTO `subCategoryEntity_new` (seq_id, id, name, active, soft_deleted, synced) SELECT seq_id, id, name, active, soft_deleted, synced FROM subCategoryEntity")
        connection.execSQL("DROP TABLE `subCategoryEntity`")
        connection.execSQL("ALTER TABLE `subCategoryEntity_new` RENAME TO `subCategoryEntity`")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `sub_category_idx` ON `subCategoryEntity`(`id`)")
    }
}
