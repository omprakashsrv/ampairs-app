package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductUpdateApiModel(
    @SerialName("id") val id: String?,
    @SerialName("ref_id") val refId: String?,
    @SerialName("name") val name: String,
    @SerialName("group_id") val groupId: String?,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("base_unit_id") val baseUnitId: String?,
    @SerialName("mrp") val mrp: Double,
    @SerialName("dp") val dp: Double,
    @SerialName("selling_price") val sellingPrice: Double,
    @SerialName("tax_code") val taxCode: String,
    @SerialName("active") val active: Boolean,
    @SerialName("unit_conversions") val unitConversions: List<UnitConversionApiModel>?,
)