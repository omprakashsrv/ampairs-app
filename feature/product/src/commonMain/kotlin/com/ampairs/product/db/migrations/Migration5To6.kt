package com.ampairs.product.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `product_upload_images` (
                `uid` TEXT NOT NULL,
                `product_id` TEXT NOT NULL,
                `file_name` TEXT NOT NULL,
                `file_size` INTEGER NOT NULL,
                `content_type` TEXT NOT NULL,
                `description` TEXT,
                `is_primary` INTEGER NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `image_url` TEXT,
                `thumbnail_url` TEXT,
                `upload_status` TEXT NOT NULL,
                `local_path` TEXT,
                `synced` INTEGER NOT NULL,
                `local_created_at` TEXT NOT NULL,
                `local_updated_at` TEXT NOT NULL,
                PRIMARY KEY(`uid`)
            )
        """)
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_product_upload_images_product_id` ON `product_upload_images`(`product_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_product_upload_images_upload_status` ON `product_upload_images`(`upload_status`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_product_upload_images_product_id_is_primary` ON `product_upload_images`(`product_id`, `is_primary`)")
    }
}
