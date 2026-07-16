package com.ampairs.unit.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for unit conversion storage
 *
 * Product-specific conversions (e.g., 1 BOX of Product A = 12 PCS)
 *
 * Table: unit_conversions
 * Indices: id (unique), product_id, composite lookup index for efficient queries
 */
@Entity(
    tableName = "unit_conversions",
    indices = [
        Index(value = ["id"], unique = true, name = "unit_conversion_id_idx"),
        Index(value = ["product_id"], name = "unit_conversion_product_idx"),
        Index(
            value = ["base_unit_id", "derived_unit_id", "product_id"],
            name = "unit_conversion_lookup_idx"
        )
    ]
)
data class UnitConversionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "base_unit_id")
    val baseUnitId: String,

    @ColumnInfo(name = "derived_unit_id")
    val derivedUnitId: String,

    @ColumnInfo(name = "multiplier")
    val multiplier: Double,

    @ColumnInfo(name = "active")
    val active: Boolean = true,

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null
)
