package com.ampairs.product.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Product Variant Entity - Represents a product variant with specific attributes
 *
 * Examples:
 * - Apparel: Size (S/M/L/XL), Color (Red/Blue/Green), Material (Cotton/Polyester)
 * - Electronics: Color (Black/White), Storage (64GB/128GB/256GB), Model (Basic/Pro)
 * - Furniture: Size (Single/Double/Queen), Color (Brown/White/Black), Material (Wood/Metal)
 *
 * Foreign key relationship with ProductEntity ensures cascade deletion.
 */
@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["sku"], unique = true),
        Index(value = ["active"])
    ]
)
data class ProductVariantEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "sku")
    val sku: String,

    @ColumnInfo(name = "variant_name")
    val variantName: String,

    // Variant Attributes (flexible for different product types)
    @ColumnInfo(name = "attribute_1_name")
    val attribute1Name: String? = null,  // e.g., "Size", "Storage", "Capacity"

    @ColumnInfo(name = "attribute_1_value")
    val attribute1Value: String? = null,  // e.g., "Large", "128GB", "500ml"

    @ColumnInfo(name = "attribute_2_name")
    val attribute2Name: String? = null,  // e.g., "Color", "Model", "Material"

    @ColumnInfo(name = "attribute_2_value")
    val attribute2Value: String? = null,  // e.g., "Blue", "Pro", "Plastic"

    @ColumnInfo(name = "attribute_3_name")
    val attribute3Name: String? = null,  // e.g., "Material", "RAM", "Finish"

    @ColumnInfo(name = "attribute_3_value")
    val attribute3Value: String? = null,  // e.g., "Cotton", "8GB", "Matte"

    // Variant-Specific Pricing (nullable = uses base product pricing if not set)
    @ColumnInfo(name = "mrp")
    val mrp: Double? = null,

    @ColumnInfo(name = "dealer_price")
    val dealerPrice: Double? = null,

    @ColumnInfo(name = "selling_price")
    val sellingPrice: Double? = null,

    // Stock Management (variant-specific stock)
    @ColumnInfo(name = "stock_quantity")
    val stockQuantity: Double = 0.0,

    @ColumnInfo(name = "low_stock_alert")
    val lowStockAlert: Double? = null,

    // Status
    @ColumnInfo(name = "active")
    val active: Int = 1,

    // Sync Metadata
    @ColumnInfo(name = "synced")
    val synced: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long? = null
)
