package com.ampairs.product.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_7_8 = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `imageEntity`")
        connection.execSQL("DROP TABLE IF EXISTS `productImageEntity`")
        connection.execSQL("DROP TABLE IF EXISTS `product_upload_images`")
    }
}
