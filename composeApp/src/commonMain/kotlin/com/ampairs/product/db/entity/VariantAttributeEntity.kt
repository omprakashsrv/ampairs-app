package com.ampairs.product.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Variant Attribute Entity - Stores searchable variant attributes
 *
 * Enables efficient queries for available sizes, colors, materials per product.
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
        Index(value = ["attribute_type"]),
        Index(value = ["attribute_value"])
    ]
)
data class VariantAttributeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "attribute_type")
    val attributeType: String, // "SIZE", "COLOR", "MATERIAL"

    @ColumnInfo(name = "attribute_value")
    val attributeValue: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
