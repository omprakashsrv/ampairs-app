package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A product as listed on the storefront. See contract §2 (ListedProduct object).
 */
@Serializable
data class ListedProduct(
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
    // Present only on the management product link (cart/order responses carry it explicitly).
    @SerialName("management_product_id") val managementProductId: String? = null,
)

/**
 * Generic Spring `PageResponse<T>` envelope used by `/products` and `/products/search`.
 * See contract §2.
 */
@Serializable
data class PageResponse<T>(
    @SerialName("content") val content: List<T> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("size") val size: Int = 0,
    @SerialName("total_elements") val totalElements: Long = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("first") val first: Boolean = true,
    @SerialName("last") val last: Boolean = true,
)
