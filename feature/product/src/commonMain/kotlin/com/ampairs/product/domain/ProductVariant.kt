package com.ampairs.product.domain

import com.ampairs.product.db.entity.ProductVariantEntity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Product Variant Domain Model
 *
 * Represents a specific variant of a product with custom attributes and pricing.
 */
data class ProductVariant(
    val id: String,
    val productId: String,
    val sku: String,
    val variantName: String,
    val attribute1Name: String? = null,
    val attribute1Value: String? = null,
    val attribute2Name: String? = null,
    val attribute2Value: String? = null,
    val attribute3Name: String? = null,
    val attribute3Value: String? = null,
    val mrp: Double? = null,
    val dealerPrice: Double? = null,
    val sellingPrice: Double? = null,
    val stockQuantity: Double = 0.0,
    val lowStockAlert: Double? = null,
    val active: Boolean = true,
    val synced: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val lastUpdated: Long? = null
) {
    /**
     * Display name combining all attribute values
     * Example: "Large, Blue, Cotton" or "128GB, Black"
     */
    val displayName: String
        get() = buildList {
            attribute1Value?.let { add(it) }
            attribute2Value?.let { add(it) }
            attribute3Value?.let { add(it) }
        }.joinToString(", ").ifEmpty { variantName }

    /**
     * Full display name with variant name
     * Example: "Shirt - Large, Blue, Cotton"
     */
    val fullDisplayName: String
        get() = if (displayName != variantName) {
            "$variantName - $displayName"
        } else {
            variantName
        }

    /**
     * Check if stock is below alert threshold
     */
    val isLowStock: Boolean
        get() = lowStockAlert != null && stockQuantity <= lowStockAlert

    /**
     * Check if variant is out of stock
     */
    val isOutOfStock: Boolean
        get() = stockQuantity <= 0.0

    /**
     * Get all attributes as a map for easy iteration
     */
    val attributes: Map<String, String>
        get() = buildMap {
            if (attribute1Name != null && attribute1Value != null) {
                put(attribute1Name, attribute1Value)
            }
            if (attribute2Name != null && attribute2Value != null) {
                put(attribute2Name, attribute2Value)
            }
            if (attribute3Name != null && attribute3Value != null) {
                put(attribute3Name, attribute3Value)
            }
        }

    /**
     * Get effective MRP (variant override or base product price)
     */
    fun getEffectiveMrp(baseProductMrp: Double): Double = mrp ?: baseProductMrp

    /**
     * Get effective dealer price (variant override or base product price)
     */
    fun getEffectiveDealerPrice(baseProductDp: Double): Double = dealerPrice ?: baseProductDp

    /**
     * Get effective selling price (variant override or base product price)
     */
    fun getEffectiveSellingPrice(baseProductSellingPrice: Double): Double =
        sellingPrice ?: baseProductSellingPrice
}

/**
 * Convert ProductVariantEntity to domain model
 */
fun ProductVariantEntity.toDomain(): ProductVariant {
    return ProductVariant(
        id = this.id,
        productId = this.productId,
        sku = this.sku,
        variantName = this.variantName,
        attribute1Name = this.attribute1Name,
        attribute1Value = this.attribute1Value,
        attribute2Name = this.attribute2Name,
        attribute2Value = this.attribute2Value,
        attribute3Name = this.attribute3Name,
        attribute3Value = this.attribute3Value,
        mrp = this.mrp,
        dealerPrice = this.dealerPrice,
        sellingPrice = this.sellingPrice,
        stockQuantity = this.stockQuantity,
        lowStockAlert = this.lowStockAlert,
        active = this.active == 1,
        synced = this.synced == 1,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastUpdated = this.lastUpdated
    )
}

/**
 * Convert ProductVariant to entity
 */
@OptIn(ExperimentalTime::class)
fun ProductVariant.toEntity(): ProductVariantEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return ProductVariantEntity(
        id = this.id,
        productId = this.productId,
        sku = this.sku,
        variantName = this.variantName,
        attribute1Name = this.attribute1Name,
        attribute1Value = this.attribute1Value,
        attribute2Name = this.attribute2Name,
        attribute2Value = this.attribute2Value,
        attribute3Name = this.attribute3Name,
        attribute3Value = this.attribute3Value,
        mrp = this.mrp,
        dealerPrice = this.dealerPrice,
        sellingPrice = this.sellingPrice,
        stockQuantity = this.stockQuantity,
        lowStockAlert = this.lowStockAlert,
        active = if (this.active) 1 else 0,
        synced = if (this.synced) 1 else 0,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastUpdated = this.lastUpdated ?: now
    )
}

/**
 * Convert list of entities to domain models
 */
fun List<ProductVariantEntity>.toDomainList(): List<ProductVariant> {
    return this.map { it.toDomain() }
}
