package com.ampairs.ecom.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Merchant-owned storefront, as returned by the storefront management API
 * (`GET/POST/PUT /v1/ecom/management/storefront`). This is the workspace-scoped configuration
 * view — richer than the public [Storefront] bootstrap model (it carries status + access mode +
 * lifecycle timestamps). `status` / `accessMode` are kept as raw strings so the client tolerates
 * unknown enum values.
 */
@Serializable
data class ManagedStorefront(
    @SerialName("uid") val uid: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("slug") val slug: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("status") val status: String = StorefrontStatus.UNKNOWN.name,
    @SerialName("access_mode") val accessMode: String = StorefrontAccessMode.PUBLIC.name,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("unpublished_at") val unpublishedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/**
 * Create the workspace's storefront. `slug` is permanent and must match `^[a-z0-9-]+$` (3–50 chars).
 */
@Serializable
data class StorefrontCreateRequest(
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String,
    @SerialName("description") val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
)

/**
 * Partial update of the workspace's storefront — only non-null fields are applied server-side.
 * `slug` is intentionally not updatable.
 */
@Serializable
data class StorefrontUpdateRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("access_mode") val accessMode: String? = null,
)
