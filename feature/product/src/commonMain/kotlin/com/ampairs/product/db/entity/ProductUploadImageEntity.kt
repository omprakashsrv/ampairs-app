package com.ampairs.product.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.product.domain.ProductUploadImage
import com.ampairs.product.domain.ProductUploadImageListItem
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(
    tableName = "product_upload_images",
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["upload_status"]),
        Index(value = ["product_id", "is_primary"])
    ]
)
data class ProductUploadImageEntity(
    @PrimaryKey
    val uid: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "content_type")
    val contentType: String,

    val description: String?,

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?,

    @ColumnInfo(name = "upload_status")
    val uploadStatus: String,

    @ColumnInfo(name = "local_path")
    val localPath: String?,

    val synced: Boolean = false,

    @ColumnInfo(name = "local_created_at")
    val localCreatedAt: String,

    @ColumnInfo(name = "local_updated_at")
    val localUpdatedAt: String,
)

fun ProductUploadImageEntity.toProductUploadImage(): ProductUploadImage = ProductUploadImage(
    uid = uid,
    productId = productId,
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
)

fun ProductUploadImageEntity.toListItem(): ProductUploadImageListItem = ProductUploadImageListItem(
    uid = uid,
    productId = productId,
    fileName = fileName,
    isPrimary = isPrimary,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    uploadStatus = uploadStatus,
    localPath = localPath,
    sortOrder = sortOrder,
    synced = synced,
)

@OptIn(ExperimentalTime::class)
fun ProductUploadImage.toEntity(
    synced: Boolean = false,
    localCreatedAt: String = Clock.System.now().toString(),
    localUpdatedAt: String = Clock.System.now().toString(),
): ProductUploadImageEntity = ProductUploadImageEntity(
    uid = uid,
    productId = productId,
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
    synced = synced,
    localCreatedAt = localCreatedAt,
    localUpdatedAt = localUpdatedAt,
)
