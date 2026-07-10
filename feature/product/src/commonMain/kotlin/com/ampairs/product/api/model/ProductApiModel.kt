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
    @SerialName("active") val active: Boolean = true,
    @SerialName("soft_deleted") val softDeleted: Boolean = false,
    @SerialName("status") val status: String? = null,
    @SerialName("tax_codes") val taxCodes: List<TaxCodeApiModel> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("last_updated") var lastUpdated: Long = 0L,
    @SerialName("unit_id") var unitId: String? = null,
    @SerialName("base_unit_id") var baseUnitId: String?,
    @SerialName("base_unit") val baseUnit: UnitApiModel?,
    @SerialName("unit_conversions") val unitConversions: List<UnitConversionApiModel>,
    @SerialName("images") val images: List<ImageApiModel>,
    @SerialName("inventory") val inventory: InventoryApiModel? = null,
    @SerialName("attributes") val attributes: Map<String, String>? = null,
    @SerialName("is_ecom_listed") val isEcomListed: Boolean = false,
)