package com.ampairs.product.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Variant Attribute Entity - Stores searchable variant attributes
 *
 * Enables efficient queries for available attribute names and values per product.
 * Example queries:
 * - Get all attribute names for a product: ["Size", "Color", "Storage"]
 * - Get all values for "Size": ["Small", "Medium", "Large", "XL"]
 * - Get all values for "Color": ["Red", "Blue", "Green"]
 *
 * Automatically populated from product variants for quick attribute lookups.
 */
@Entity(
    tableName = "variant_attributes",
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
        Index(value = ["attribute_name"]),
        Index(value = ["attribute_value"]),
        Index(value = ["product_id", "attribute_name", "attribute_value"], unique = true)
    ]
)
data class VariantAttributeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "attribute_name")
    val attributeName: String, // e.g., "Size", "Color", "Storage", "Material"

    @ColumnInfo(name = "attribute_value")
    val attributeValue: String, // e.g., "Large", "Blue", "128GB", "Cotton"

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null
)
