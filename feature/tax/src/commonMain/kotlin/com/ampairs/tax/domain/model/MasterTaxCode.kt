package com.ampairs.tax.domain.model

import com.ampairs.common.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Master Tax Code - Server-side registry of all available tax codes
 * Organized by country (India HSN/SAC, USA Categories, UK Categories, etc.)
 *
 * This is NOT synced to mobile - too large (100K+ codes)
 * Mobile searches via API and subscribes to specific codes only
 */
@Serializable
data class MasterTaxCode(
    @SerialName("id")
    val id: String,

    @SerialName("country_code")
    val countryCode: String,                        // ISO 3166-1 alpha-2 (IN, US, GB, etc.)

    @SerialName("code_type")
    val codeType: TaxCodeType,                      // HSN_CODE, SAC_CODE, TAX_CATEGORY

    @SerialName("code")
    val code: String,                               // "12345678", "998314", "GROCERY"

    @SerialName("description")
    val description: String,                        // Full description

    @SerialName("short_description")
    val shortDescription: String,                   // Short description

    @SerialName("chapter")
    val chapter: String? = null,                    // HSN Chapter (e.g., "12")

    @SerialName("heading")
    val heading: String? = null,                    // HSN Heading (e.g., "1234")

    @SerialName("sub_heading")
    val subHeading: String? = null,                 // HSN Sub-heading (e.g., "123456")

    @SerialName("category")
    val category: String? = null,                   // Category/Industry

    @SerialName("default_tax_rate")
    val defaultTaxRate: Double? = null,             // Suggested default rate

    @SerialName("default_tax_slab_id")
    val defaultTaxSlabId: String? = null,           // Default slab reference

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("metadata")
    val metadata: Map<String, String> = emptyMap(), // Additional attributes

    // Backend serializes these as ISO 8601 strings (e.g. "2026-05-25T17:01:04.083622Z")
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,

    @Serializable(with = InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,
)
