package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for the (future) store-access API used by the LOGIN_FIRST gate.
 * See plan §11.1.
 */
@Serializable
data class StoreAccessRequest(
    @SerialName("storefront_slug") val storefrontSlug: String,
)

/**
 * Store-access status object. See plan §11.1.
 */
@Serializable
data class StoreAccessResponse(
    @SerialName("status") val status: String = StoreAccessStatus.NONE.name,
    @SerialName("requested_at") val requestedAt: String? = null,
)
