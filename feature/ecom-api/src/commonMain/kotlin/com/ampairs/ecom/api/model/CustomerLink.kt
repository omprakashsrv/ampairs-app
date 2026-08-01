package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /ecom/account/link-candidate` response — a CRM account found to match the buyer's own phone
 * in this workspace, offered for confirmation before linking. The server returns a null `data` (not
 * this type) when nothing matched — see [com.ampairs.ecom.data.api.EcomApi.getLinkCandidate].
 *
 * [phone]/[gstNumber]/[address] mirror the workspace owner's own CRM entry (not the buyer's login) —
 * shown in a confirmation sheet so the buyer can verify it's really their account before linking.
 */
@Serializable
data class LinkCandidateResponse(
    @SerialName("customer_id") val customerId: String,
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String,
    @SerialName("gst_number") val gstNumber: String? = null,
    @SerialName("address") val address: String? = null,
)

/** Request body for `POST /ecom/account/link` — commits a candidate from [LinkCandidateResponse]. */
@Serializable
data class ConfirmLinkRequest(
    @SerialName("customer_id") val customerId: String,
)

/** `POST /ecom/account/link` response — the CRM account the buyer is now linked to. */
@Serializable
data class DistributorAccount(
    @SerialName("customer_id") val customerId: String,
    @SerialName("name") val name: String,
    @SerialName("is_default") val isDefault: Boolean,
    @SerialName("role") val role: String,
)
