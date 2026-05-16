package com.ampairs.customer.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MasterState(
    @SerialName("state_code")
    val stateCode: String = "",
    val name: String = "",
    @SerialName("short_name")
    val shortName: String = "",
    @SerialName("country_code")
    val countryCode: String = "",
    @SerialName("country_name")
    val countryName: String = "",
    val region: String? = null,
    val timezone: String? = null,
    @SerialName("local_name")
    val localName: String? = null,
    val capital: String? = null,
    val population: Long? = null,
    @SerialName("area_sq_km")
    val areaSqKm: Double? = null,
    @SerialName("gst_code")
    val gstCode: String? = null,
    @SerialName("postal_code_pattern")
    val postalCodePattern: String? = null,
    val active: Boolean = true,
    val metadata: String? = null
) {
    /**
     * Get formatted display name for UI
     */
    fun getDisplayName(): String {
        return "$name ($shortName)"
    }

    /**
     * Check if this is an Indian state
     */
    fun isIndianState(): Boolean {
        return countryCode == "IN"
    }
}