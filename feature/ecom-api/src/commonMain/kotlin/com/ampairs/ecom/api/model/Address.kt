package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Saved delivery address. See contract §6.
 */
@Serializable
data class AddressResponse(
    @SerialName("uid") val uid: String = "",
    @SerialName("label") val label: String? = null,
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String? = null,
    @SerialName("city") val city: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("pin_code") val pinCode: String = "",
    @SerialName("country") val country: String = "IN",
    @SerialName("phone") val phone: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
)

/**
 * Create/update payload for `POST`/`PUT /ecom/account/addresses`. See contract §6.
 */
@Serializable
data class AddressRequest(
    @SerialName("label") val label: String? = null,
    @SerialName("address_line1") val addressLine1: String,
    @SerialName("address_line2") val addressLine2: String? = null,
    @SerialName("city") val city: String,
    @SerialName("state") val state: String,
    @SerialName("pin_code") val pinCode: String,
    @SerialName("country") val country: String = "IN",
    @SerialName("phone") val phone: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
)
