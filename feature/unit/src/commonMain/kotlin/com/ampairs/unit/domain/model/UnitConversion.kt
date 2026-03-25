package com.ampairs.unit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unit conversion model for product-specific unit conversions
 *
 * Formula: derived_quantity = base_quantity * multiplier
 *
 * Example: 1 BOX of Product A = 12 PCS
 *   - productId: "PRD123..."
 *   - baseUnitId: "BOX"
 *   - derivedUnitId: "PCS"
 *   - multiplier: 12.0
 *
 * Conversion: convertQuantity(2.0, "BOX", "PCS") = 2.0 * 12.0 = 24 PCS
 */
@Serializable
data class UnitConversion(
    val uid: String = "",
    @SerialName("product_id")
    val productId: String,
    @SerialName("base_unit_id")
    val baseUnitId: String,
    @SerialName("derived_unit_id")
    val derivedUnitId: String,
    val multiplier: Double,
    val active: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
