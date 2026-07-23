package com.ampairs.ecom.data.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Local catalog row. Contract §8 `listed_product`.
 * `image_urls` is stored as a JSON array string (encoded/decoded in mappers).
 */
@Entity(
    tableName = "listed_product",
    indices = [
        Index(value = ["storefront_id", "is_visible"], name = "idx_product_storefront_visible"),
        Index(value = ["storefront_id", "category"], name = "idx_product_category"),
        Index(value = ["storefront_id", "brand"], name = "idx_product_brand"),
    ]
)
data class ListedProductEntity(
    @PrimaryKey val uid: String,
    val storefront_id: String,
    val management_product_id: String? = null,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val unit: String? = null,
    val price: Double,
    val mrp: Double? = null,
    val stock_status: String,
    val stock_quantity: Int = 0,
    val image_urls: String = "[]",
    val description: String? = null,
    val is_visible: Int = 1,
    val updated_at: String? = null,
)

/** Incremental-sync cursor per storefront. Contract §8 `sync_cursor`. */
@Entity(tableName = "sync_cursor")
data class SyncCursorEntity(
    @PrimaryKey val storefront_id: String,
    val next_since: String,
    val synced_at: Long,
)
