package com.ampairs.tax.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workspace Tax Code - Subset of master codes actively used by workspace
 * This IS synced to mobile for offline access
 *
 * Each workspace typically uses 50-500 codes from the master registry
 */
@Serializable
data class TaxCode(
    @SerialName("id")
    val id: String,

    @SerialName("master_tax_code_id")
    val masterTaxCodeId: String,                    // FK to master_tax_codes

    // Cached master data for offline access
    @SerialName("code")
    val code: String,                               // Cached: "12345678", "998314"

    @SerialName("code_type")
    val codeType: TaxCodeType,                      // Cached: HSN_CODE, SAC_CODE

    @SerialName("description")
    val description: String,                        // Cached description

    @SerialName("short_description")
    val shortDescription: String,                   // Cached short description

    // Workspace-specific configuration
    @SerialName("custom_tax_rule_id")
    val customTaxRuleId: String? = null,            // Override default rule

    @SerialName("usage_count")
    val usageCount: Int = 0,                        // Track usage frequency

    @SerialName("last_used_at")
    val lastUsedAt: Long? = null,

    @SerialName("is_favorite")
    val isFavorite: Boolean = false,

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("added_at")
    val addedAt: Long,

    @SerialName("updated_at")
    val updatedAt: Long,

    @SerialName("sync_status")
    val syncStatus: String = "SYNCED"              // SYNCED, PENDING, DELETE_PENDING
)
