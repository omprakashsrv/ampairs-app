package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Category tiles, subcategory chips and brand cards from `GET /store/{slug}/catalog-meta`.
 * See contract §2.
 */
@Serializable
data class CatalogMeta(
    @SerialName("categories") val categories: List<CategoryMeta> = emptyList(),
    @SerialName("brands") val brands: List<BrandMeta> = emptyList(),
)

@Serializable
data class CategoryMeta(
    @SerialName("name") val name: String = "",
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("subcategories") val subcategories: List<SubcategoryMeta> = emptyList(),
)

@Serializable
data class SubcategoryMeta(
    @SerialName("name") val name: String = "",
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class BrandMeta(
    @SerialName("name") val name: String = "",
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)
