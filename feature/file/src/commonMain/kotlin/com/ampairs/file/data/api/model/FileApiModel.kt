package com.ampairs.file.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntityImageApiModel(
    @SerialName("uid") val uid: String = "",
    @SerialName("entity_type") val entityType: String = "",
    @SerialName("entity_uid") val entityUid: String = "",
    @SerialName("original_filename") val originalFilename: String = "",
    @SerialName("content_type") val contentType: String = "",
    @SerialName("file_size") val fileSize: Long = 0L,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    @SerialName("is_primary") val isPrimary: Boolean = false,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("active") val active: Boolean = true,
)

@Serializable
data class EntityImageListApiModel(
    @SerialName("images") val images: List<EntityImageApiModel> = emptyList(),
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("primary_image") val primaryImage: EntityImageApiModel? = null,
)
