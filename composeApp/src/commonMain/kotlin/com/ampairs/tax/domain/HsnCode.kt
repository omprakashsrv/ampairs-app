package com.ampairs.tax.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HsnCode(
    @SerialName("id")
    val id: String = "",

    @SerialName("hsn_code")
    val hsnCode: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("chapter")
    val chapter: String = "",

    @SerialName("heading")
    val heading: String = "",

    @SerialName("parent_hsn_id")
    val parentHsnId: String? = null,

    @SerialName("category")
    val category: HsnCategory = HsnCategory.GENERAL,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    val createdAt: Long = 0,

    @SerialName("updated_at")
    val updatedAt: Long = 0
) {
    val isValidHsnCode: Boolean
        get() = hsnCode.matches(Regex("^\\d{4,8}$"))

    val formattedCode: String
        get() = when (hsnCode.length) {
            4 -> hsnCode
            6 -> "${hsnCode.take(4)} ${hsnCode.drop(4)}"
            8 -> "${hsnCode.take(4)} ${hsnCode.drop(4).take(2)} ${hsnCode.drop(6)}"
            else -> hsnCode
        }
}

@Serializable
enum class HsnCategory {
    @SerialName("general")
    GENERAL,

    @SerialName("agriculture")
    AGRICULTURE,

    @SerialName("textiles")
    TEXTILES,

    @SerialName("chemicals")
    CHEMICALS,

    @SerialName("machinery")
    MACHINERY,

    @SerialName("electronics")
    ELECTRONICS,

    @SerialName("vehicles")
    VEHICLES,

    @SerialName("precious_metals")
    PRECIOUS_METALS,

    @SerialName("food_beverages")
    FOOD_BEVERAGES,

    @SerialName("tobacco")
    TOBACCO,

    @SerialName("construction")
    CONSTRUCTION,

    @SerialName("healthcare")
    HEALTHCARE
}

data class HsnSearchFilter(
    val query: String = "",
    val category: HsnCategory? = null,
    val chapter: String? = null,
    val activeOnly: Boolean = true
)

data class HsnListItem(
    val id: String,
    val hsnCode: String,
    val description: String,
    val category: HsnCategory,
    val currentGstRate: Double?,
    val isActive: Boolean
)