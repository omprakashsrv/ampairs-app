package com.ampairs.tax.domain.model

import com.ampairs.common.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TaxCode(
    @SerialName("id") val id: String,
    @SerialName("master_tax_code_id") val masterTaxCodeId: String,
    @SerialName("code") val code: String,
    @SerialName("code_type") val codeType: TaxCodeType,
    @SerialName("description") val description: String,
    @SerialName("short_description") val shortDescription: String,
    @SerialName("custom_name") val customName: String? = null,
    @SerialName("custom_tax_rule_id") val customTaxRuleId: String? = null,
    @SerialName("usage_count") val usageCount: Int = 0,
    @Serializable(with = InstantSerializer::class)
    @SerialName("last_used_at") val lastUsedAt: Instant? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("notes") val notes: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    // Backend serializes these as ISO 8601 strings (e.g. "2026-05-25T17:01:04.083622Z")
    @Serializable(with = InstantSerializer::class)
    @SerialName("added_at") val addedAt: Instant,
    @Serializable(with = InstantSerializer::class)
    @SerialName("updated_at") val updatedAt: Instant,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
)
