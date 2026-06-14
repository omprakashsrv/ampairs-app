package com.ampairs.tax.domain.model

import com.ampairs.common.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Tax Rule - Defines how multiple components combine to form complete tax
 * References workspace tax codes for offline access
 */
@Serializable
data class TaxRule(
    @SerialName("id")
    val id: String = "",

    @SerialName("country_code")
    val countryCode: String = "",

    // Tax code reference (workspace-specific)
    @SerialName("tax_code_id")
    val taxCodeId: String = "",                     // FK to workspace_tax_codes

    @SerialName("tax_code")
    val taxCode: String = "",                       // Cached: "12345678", "998314"

    @SerialName("tax_code_type")
    val taxCodeType: String = "",                   // "HSN_CODE", "SAC_CODE"

    @SerialName("tax_code_description")
    val taxCodeDescription: String? = null,         // Cached description

    // Jurisdiction
    @SerialName("jurisdiction")
    val jurisdiction: String = "",                  // "MH", "CA-Los Angeles"

    @SerialName("jurisdiction_level")
    val jurisdictionLevel: String = "",             // "STATE", "COUNTY", "CITY"

    // Component composition - map of scenario to composition
    @SerialName("component_composition")
    val componentComposition: Map<String, ComponentComposition> = emptyMap(),

    @SerialName("is_active")
    val isActive: Boolean = true,

    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant = Instant.fromEpochMilliseconds(0),

    @Serializable(with = InstantSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant = Instant.fromEpochMilliseconds(0),

    @SerialName("sync_status")
    val syncStatus: String = "SYNCED"
)

/**
 * Component Composition - How components combine for a scenario
 */
@Serializable
data class ComponentComposition(
    @SerialName("scenario")
    val scenario: String,                           // "intra_state", "inter_state", "standard"

    @SerialName("components")
    val components: List<ComponentReference>,

    @SerialName("total_rate")
    val totalRate: Double
)

/**
 * Component Reference - Individual component in composition
 */
@Serializable
data class ComponentReference(
    @SerialName("id")
    val id: String,                                 // Component ID

    @SerialName("name")
    val name: String,                               // Component name (CGST, SGST, IGST)

    @SerialName("rate")
    val rate: Double,

    @SerialName("order")
    val order: Int
)
