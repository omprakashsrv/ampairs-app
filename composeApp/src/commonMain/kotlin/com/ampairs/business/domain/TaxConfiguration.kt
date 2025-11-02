package com.ampairs.business.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tax configuration containing tax-related settings.
 */
@Serializable
data class TaxConfiguration(
    @SerialName("uid")
    val uid: String = "",
    @SerialName("tax_id")
    val taxId: String? = null,
    @SerialName("registration_number")
    val registrationNumber: String? = null,
    @SerialName("tax_settings")
    val taxSettings: Map<String, String>? = null,
    val country: String? = null,
    val state: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Request payload for updating tax configuration.
 */
@Serializable
data class TaxConfigurationUpdateRequest(
    @SerialName("tax_id")
    val taxId: String? = null,
    @SerialName("registration_number")
    val registrationNumber: String? = null,
    @SerialName("tax_settings")
    val taxSettings: Map<String, String>? = null
)
