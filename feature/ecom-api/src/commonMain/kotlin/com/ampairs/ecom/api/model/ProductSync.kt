package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Delta item from `GET /store/{slug}/products/sync`. Includes unlisted products
 * (`is_visible = false`) so the local catalog can hide them. See contract §3.
 */
@Serializable
data class ProductSyncItem(
    @SerialName("uid") val uid: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("brand") val brand: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("subcategory") val subcategory: String? = null,
    @SerialName("unit") val unit: String? = null,
    @SerialName("price") val price: Double = 0.0,
    @SerialName("mrp") val mrp: Double? = null,
    @SerialName("stock_status") val stockStatus: String = StockStatus.UNKNOWN.name,
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("description") val description: String? = null,
    @SerialName("management_product_id") val managementProductId: String? = null,
    @SerialName("is_visible") val isVisible: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/**
 * Incremental sync page from `GET /store/{slug}/products/sync`. See contract §3.
 */
@Serializable
data class ProductSyncPage(
    @SerialName("items") val items: List<ProductSyncItem> = emptyList(),
    @SerialName("total_changes") val totalChanges: Int = 0,
    @SerialName("page") val page: Int = 0,
    @SerialName("size") val size: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("next_since") val nextSince: String? = null,
)
