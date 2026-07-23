package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET/PATCH /v1/ecom/management/customers` — a buyer↔CRM-customer link, for the owner's
 * workspace-wide "ecom users" screen (every buyer across every customer, not scoped to one).
 */
@Serializable
data class EcomContactResponse(
    @SerialName("contact_uid") val contactUid: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("customer_name") val customerName: String,
    val name: String,
    val phone: String? = null,
    val role: String,
    @SerialName("is_default") val isDefault: Boolean,
    val active: Boolean,
)

/** Request body for `PATCH /v1/ecom/management/customers/{contactUid}/status`. */
@Serializable
data class SetEcomContactStatusRequest(
    val active: Boolean,
)
