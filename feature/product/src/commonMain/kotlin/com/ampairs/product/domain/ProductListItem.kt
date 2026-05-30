package com.ampairs.product.domain

data class ProductListItem(
    val id: String,
    val name: String,
    val code: String,
    val mrp: Double,
    val sellingPrice: Double,
    val categoryName: String?,
    val brandName: String?,
    val stockQuantity: Double?,
    val imageUrl: String?,
    val active: Boolean
)
