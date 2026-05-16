package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Customer image domain model representing image metadata and file information.
 * Supports multiple image sizes and offline-first synchronization.
 */
@Serializable
data class CustomerImage(
    val uid: String = "",
    @SerialName("customer_uid")
    val customerId: String = "",
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
    val uploadStatus: String = "PENDING", // PENDING, UPLOADING, COMPLETED, FAILED
    @SerialName("local_path")
    val localPath: String? = null, // Local file path for offline support
    val tags: List<String>? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * List item representation for UI display
 */
data class CustomerImageListItem(
    val uid: String,
    val customerId: String,
    val fileName: String,
    val isPrimary: Boolean,
    val thumbnailUrl: String?,
    val uploadStatus: String,
    val localPath: String?,
    val sortOrder: Int,
    val synced: Boolean
)

/**
 * Upload request DTO for API
 */
@Serializable
data class CustomerImageUploadRequest(
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("file_name")
    val fileName: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("file_size")
    val fileSize: Long,
    val description: String? = null,
    @SerialName("is_primary")
    val isPrimary: Boolean = false,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    val tags: List<String>? = null,
    val metadata: Map<String, String>? = null
)

/**
 * Upload response DTO from API
 */
@Serializable
data class CustomerImageUploadResponse(
    val uid: String,
    @SerialName("upload_url")
    val uploadUrl: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String,
    @SerialName("expires_at")
    val expiresAt: String? = null
)

/**
 * Update request DTO for metadata changes
 */
@Serializable
data class CustomerImageUpdateRequest(
    val description: String? = null,
    @SerialName("is_primary")
    val isPrimary: Boolean? = null,
    @SerialName("sort_order")
    val sortOrder: Int? = null,
    val tags: List<String>? = null,
    val metadata: Map<String, String>? = null
)

/**
 * Thumbnail response DTO
 */
@Serializable
data class ThumbnailResponse(
    @SerialName("thumbnail_url")
    val thumbnailUrl: String,
    val size: String, // "150x150", "300x300", "500x500"
    @SerialName("expires_at")
    val expiresAt: String? = null
)

/**
 * Bulk operations request
 */
@Serializable
data class CustomerImageBulkRequest(
    @SerialName("image_ids")
    val imageIds: List<String>,
    val action: String, // "delete", "set_primary", "reorder"
    val data: Map<String, String>? = null
)

// Extension functions for conversions
fun CustomerImage.toListItem(): CustomerImageListItem = CustomerImageListItem(
    uid = uid,
    customerId = customerId,
    fileName = fileName,
    isPrimary = isPrimary,
    thumbnailUrl = thumbnailUrl,
    uploadStatus = uploadStatus,
    localPath = localPath,
    sortOrder = sortOrder,
    synced = true
)

fun CustomerImageUploadRequest.toCustomerImage(uid: String): CustomerImage = CustomerImage(
    uid = uid,
    customerId = customerId,
    fileName = fileName,
    contentType = contentType,
    fileSize = fileSize,
    description = description,
    isPrimary = isPrimary,
    sortOrder = sortOrder,
    tags = tags,
    metadata = metadata,
    uploadStatus = "PENDING"
)

/**
 * Image upload status enum values
 */
object CustomerImageStatus {
    const val PENDING = "PENDING"
    const val UPLOADING = "UPLOADING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
}

/**
 * Supported image content types
 */
object CustomerImageContentType {
    const val JPEG = "image/jpeg"
    const val PNG = "image/png"
    const val WEBP = "image/webp"

    val SUPPORTED_TYPES = listOf(JPEG, PNG, WEBP)

    fun isSupported(contentType: String): Boolean = contentType in SUPPORTED_TYPES
}