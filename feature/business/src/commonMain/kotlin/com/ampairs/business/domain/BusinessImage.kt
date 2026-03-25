package com.ampairs.business.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Business image types for gallery categorization.
 */
@Serializable
enum class BusinessImageType {
    GALLERY,
    STOREFRONT,
    INTERIOR,
    PRODUCT_SHOWCASE,
    TEAM,
    BANNER,
    CERTIFICATE,
    OTHER
}

/**
 * Domain model for business gallery images.
 */
@Serializable
data class BusinessImage(
    val uid: String,
    @SerialName("business_id")
    val businessId: String,
    @SerialName("image_type")
    val imageType: String = "GALLERY",
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerialName("alt_text")
    val altText: String? = null,
    @SerialName("display_order")
    val displayOrder: Int = 0,
    @SerialName("is_primary")
    val isPrimary: Boolean = false,
    val active: Boolean = true,
    @SerialName("original_filename")
    val originalFilename: String? = null,
    @SerialName("file_size")
    val fileSize: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("uploaded_by")
    val uploadedBy: String? = null,
    @SerialName("uploaded_at")
    val uploadedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Request payload for updating business image metadata.
 */
@Serializable
data class UpdateBusinessImageRequest(
    val title: String? = null,
    val description: String? = null,
    @SerialName("alt_text")
    val altText: String? = null,
    @SerialName("image_type")
    val imageType: String? = null
)

/**
 * Request payload for reordering images.
 */
@Serializable
data class ReorderImagesRequest(
    @SerialName("image_uids")
    val imageUids: List<String>
)
