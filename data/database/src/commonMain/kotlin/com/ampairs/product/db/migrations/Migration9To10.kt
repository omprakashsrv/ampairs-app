package com.ampairs.product.db.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds indexes on the columns used by the product list filter (brand, category, sub-category, group)
 * so multi-select filtering stays fast as the product table grows. Index names match the [Index]
 * names declared on [com.ampairs.product.db.entity.ProductEntity] so Room's schema validation passes.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS `product_brand_idx` ON `productEntity` (`brand_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `product_category_idx` ON `productEntity` (`category_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `product_sub_category_idx` ON `productEntity` (`sub_category_id`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `product_group_idx` ON `productEntity` (`group_id`)")
    }
}
