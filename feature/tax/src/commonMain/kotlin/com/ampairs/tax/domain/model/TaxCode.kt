package com.ampairs.tax.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TaxCodeType {
    @SerialName("HSN_CODE") HSN_CODE,
    @SerialName("SAC_CODE") SAC_CODE,
    @SerialName("TAX_CATEGORY") TAX_CATEGORY,
}

@Serializable
data class TaxCode(
    @SerialName("id")
    val id: String,
    @SerialName("master_tax_code_id")
    val masterTaxCodeId: String,
    @SerialName("code")
    val code: String,
    @SerialName("code_type")
    val codeType: TaxCodeType,
    @SerialName("description")
    val description: String,
    @SerialName("short_description")
    val shortDescription: String,
    @SerialName("custom_name")
    val customName: String? = null,
    @SerialName("custom_tax_rule_id")
    val customTaxRuleId: String? = null,
    @SerialName("usage_count")
    val usageCount: Int = 0,
    @SerialName("last_used_at")
    val lastUsedAt: Long? = null,
    @SerialName("is_favorite")
    val isFavorite: Boolean = false,
    val notes: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("added_at")
    val addedAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("sync_status")
    val syncStatus: String = "SYNCED",
)
