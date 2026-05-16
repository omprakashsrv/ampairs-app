package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class InventoryApiModel(
    @SerialName("id") val id: String? = null,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_ref_id") val productRefId: String? = null,
    @SerialName("description") val description: String? = "",
    @SerialName("code") val code: String? = null,
    @SerialName("tax_code") val taxCode: String? = null,
    @SerialName("base_unit_id") val baseUnitId: String? = null,
    @SerialName("active") val active: Boolean? = true,
    @SerialName("mrp") val mrp: Double? = 0.0,
    @SerialName("dp") val dp: Double? = 0.0,
    @SerialName("stock") val stock: Double? = 0.0,
    @SerialName("selling_price") val sellingPrice: Double? = 0.0,
    @SerialName("buying_price") val buyingPrice: Double? = 0.0,
    @SerialName("last_updated") var lastUpdated: Long? = 0L,
    @SerialName("soft_deleted") val softDeleted: Boolean? = false,
    @SerialName("created_at") val createdAt: String? = "",
    @SerialName("updated_at") val updatedAt: String? = "",
)