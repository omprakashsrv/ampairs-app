package com.ampairs.customer.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.customer.domain.CustomerImage
import com.ampairs.customer.domain.CustomerImageListItem
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Room entity for customer image with sync support and workspace isolation.
 * Includes offline-first capabilities with sync metadata.
 */
@Entity(
    tableName = "customer_images",
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["is_primary"]),
        Index(value = ["upload_status"]),
        Index(value = ["synced"]),
        Index(value = ["customer_id", "is_primary"]),
        Index(value = ["customer_id", "sort_order"])
    ]
)
data class CustomerImageEntity(
    @PrimaryKey
    val uid: String,

    // Customer relationship
    @ColumnInfo(name = "customer_id")
    val customerId: String,

    // File metadata
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    @ColumnInfo(name = "content_type")
    val contentType: String,
    val description: String?,

    // Image properties
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    // URLs
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?,

    // Upload and sync status
    @ColumnInfo(name = "upload_status")
    val uploadStatus: String,
    @ColumnInfo(name = "local_path")
    val localPath: String?,

    // Additional metadata
    val tags: String?, // JSON string of tags array
    val metadata: String?, // JSON string of metadata map

    // Sync metadata for offline-first
    val synced: Boolean = false,
    @ColumnInfo(name = "sync_pending")
    val syncPending: Boolean = false,
    @ColumnInfo(name = "last_sync_attempt")
    val lastSyncAttempt: String?,

    // Timestamps
    @ColumnInfo(name = "created_at")
    val createdAt: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String?,
    @ColumnInfo(name = "local_created_at")
    val localCreatedAt: String,
    @ColumnInfo(name = "local_updated_at")
    val localUpdatedAt: String
)

// Extension functions for conversions
fun CustomerImageEntity.toCustomerImage(): CustomerImage = CustomerImage(
    uid = uid,
    customerId = customerId,
    fileName = fileName,
    fileSize = fileSize,
    contentType = contentType,
    description = description,
    isPrimary = isPrimary,
    sortOrder = sortOrder,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    uploadStatus = uploadStatus,
    localPath = localPath,
    tags = tags?.let {
        try {
            // Parse JSON string to list
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(it)
        } catch (e: Exception) {
            null
        }
    },
    metadata = metadata?.let {
        try {
            // Parse JSON string to map
            kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(it)
        } catch (e: Exception) {
            null
        }
    },
    createdAt = createdAt,
    updatedAt = updatedAt
)

@OptIn(ExperimentalTime::class)
fun CustomerImage.toEntity(
    synced: Boolean = false,
    localCreatedAt: String = Clock.System.now().toString(),
    localUpdatedAt: String = Clock.System.now().toString()
): CustomerImageEntity = CustomerImageEntity(
    uid = uid,
    customerId = customerId,
    fileName = fileName,
    fileSize = fileSize,
    contentType = contentType,
    description = description,
    isPrimary = isPrimary,
    sortOrder = sortOrder,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    uploadStatus = uploadStatus,
    localPath = localPath,
    tags = tags?.let { kotlinx.serialization.json.Json.encodeToString(it) },
    metadata = metadata?.let { kotlinx.serialization.json.Json.encodeToString(it) },
    synced = synced,
    syncPending = !synced,
    lastSyncAttempt = null,
    createdAt = createdAt,
    updatedAt = updatedAt,
    localCreatedAt = localCreatedAt,
    localUpdatedAt = localUpdatedAt
)

fun CustomerImageEntity.toListItem(): CustomerImageListItem = CustomerImageListItem(
    uid = uid,
    customerId = customerId,
    fileName = fileName,
    isPrimary = isPrimary,
    thumbnailUrl = thumbnailUrl,
    uploadStatus = uploadStatus,
    localPath = localPath,
    sortOrder = sortOrder,
    synced = synced
)