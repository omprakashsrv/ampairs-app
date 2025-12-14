package com.ampairs.product.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Product Variant Entity - Represents a product variant with specific attributes
 *
 * Supports size/color/material combinations with variant-specific pricing and stock.
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

    // Variant Attributes
    @ColumnInfo(name = "size")
    val size: String? = null,

    @ColumnInfo(name = "color")
    val color: String? = null,

    @ColumnInfo(name = "material")
    val material: String? = null,

    // Variant-Specific Pricing (overrides base product pricing)
    @ColumnInfo(name = "mrp")
    val mrp: Double? = null,

    @ColumnInfo(name = "dealer_price")
    val dealerPrice: Double? = null,

    @ColumnInfo(name = "selling_price")
    val sellingPrice: Double? = null,

    // Stock Management
    @ColumnInfo(name = "stock")
    val stock: Int = 0,

    // Status
    @ColumnInfo(name = "active")
    val active: Boolean = true,

    // Sync Metadata
    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
