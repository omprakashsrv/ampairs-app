package com.ampairs.product.domain

import kotlinx.serialization.Serializable

@Serializable
data class ProductSummary(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val mrp: Double = 0.0,
    val dp: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stockQuantity: Double? = null,
    val lowStockAlert: Double? = null,
    val primaryImageUrl: String? = null,
    val taxCode: String = "",
    val productType: ProductType? = null,
    val serviceType: ServiceType? = null,
    val hasVariants: Boolean = false,
    val categoryName: String? = null,
    val brandName: String? = null,
    val baseUnitId: String? = null,
) {
    // Cart quantity — mutable state not persisted in serialization
    var quantity: Double = 0.0
}
