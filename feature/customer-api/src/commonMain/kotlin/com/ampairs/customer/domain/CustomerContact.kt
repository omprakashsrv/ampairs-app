package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An app (ecom storefront) login linked to this CRM customer — shown on the customer detail
 * screen's "Linked accounts" section. `active = false` means the owner has restricted this
 * account's ordering access (see `EcomCustomerService.setContactActive` on the backend).
 */
@Serializable
data class CustomerContactResponse(
    @SerialName("contact_uid") val contactUid: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("customer_name") val customerName: String,
    val name: String,
    val phone: String? = null,
    val role: String,
    @SerialName("is_default") val isDefault: Boolean,
    val active: Boolean,
)

/** Request body for `POST /customer/v1/{customerId}/contacts` — links the app account with this phone. */
@Serializable
data class LinkContactRequest(
    val phone: String,
    val name: String? = null,
    val role: String = "OWNER",
    @SerialName("is_default") val isDefault: Boolean = false,
)

/** Request body for `PATCH /customer/v1/{customerId}/contacts/{contactUid}/status`. */
@Serializable
data class SetContactStatusRequest(
    val active: Boolean,
)
