package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Storefront branding + status from `GET /store/{slug}`. See contract §1.
 */
@Serializable
data class Storefront(
    @SerialName("uid") val uid: String = "",
    @SerialName("slug") val slug: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("status") val status: String = StorefrontStatus.UNKNOWN.name,
    // Optional fields the backend may add for the access gate (plan §11). Nullable so the
    // client tolerates their absence and falls back to a local config flag.
    @SerialName("access_mode") val accessMode: String? = null,
    @SerialName("viewer_access") val viewerAccess: String? = null,
)
