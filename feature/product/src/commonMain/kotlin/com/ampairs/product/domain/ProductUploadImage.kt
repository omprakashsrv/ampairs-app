package com.ampairs.product.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain model for a product upload image with full metadata.
 */
@Serializable
data class ProductUploadImage(
    val uid: String = "",
    @SerialName("product_uid")
    val productId: String = "",
    @SerialName("file_name")
    val fileName: String = "",
    @SerialName("file_size")
    val fileSize: Long = 0,
    @SerialName("content_type")
    val contentType: String = "",
    val description: String? = null,
    @SerialName("is_primary")
    val isPrimary: Boolean = false,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("upload_status")
    val uploadStatus: String = ProductImageStatus.PENDING,
    @SerialName("local_path")
    val localPath: String? = null,
)

/**
 * Lightweight list item for UI display in grids and lists.
 */
data class ProductUploadImageListItem(
    val uid: String,
    val productId: String,
    val fileName: String,
    val isPrimary: Boolean,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val uploadStatus: String,
    val localPath: String?,
    val sortOrder: Int,
    val synced: Boolean,
)

/**
 * Upload response DTO from API.
 */
@Serializable
data class ProductUploadImageUploadResponse(
    val uid: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String,
)
