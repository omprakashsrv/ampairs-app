package com.ampairs.product.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductApiModel(
    @SerialName("id") val id: String,
    @SerialName("ref_id") val refId: String? = "",
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
    @SerialName("group_id") var groupId: String?,
    @SerialName("brand_id") var brandId: String?,
    @SerialName("category_id") var categoryId: String?,
    @SerialName("sub_category_id") var subCategoryId: String?,
    @SerialName("mrp") val mrp: Double,
    @SerialName("dp") val dp: Double,
    @SerialName("selling_price") val sellingPrice: Double,
    @SerialName("tax_code") val taxCode: String,
    @SerialName("active") val active: Boolean,
    @SerialName("soft_deleted") val softDeleted: Boolean,
    @SerialName("tax_codes") val taxCodes: List<TaxCodeApiModel>,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?,
    @SerialName("last_updated") var lastUpdated: Long,
    @SerialName("base_unit_id") var baseUnitId: String?,
    @SerialName("base_unit") val baseUnit: UnitApiModel?,
    @SerialName("unit_conversions") val unitConversions: List<UnitConversionApiModel>,
    @SerialName("images") val images: List<ImageApiModel>,
    @SerialName("inventory") val inventory: InventoryApiModel? = null,
)