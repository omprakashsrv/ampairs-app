package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class UnitConversionApiModel(
    @SerialName("id") var id: String,
    @SerialName("product_id") var productId: String,
    @SerialName("base_unit") var baseUnit: UnitApiModel,
    @SerialName("derived_unit") var derivedUnit: UnitApiModel,
    @SerialName("multiplier") var multiplier: Double,
    @SerialName("active") val active: Boolean,
    @SerialName("soft_deleted") val softDeleted: Boolean,
)
