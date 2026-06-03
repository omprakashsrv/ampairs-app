package com.ampairs.file.data.api

import com.ampairs.file.api.FileEntityType
import com.ampairs.file.data.api.model.EntityImageApiModel
import com.ampairs.file.data.api.model.EntityImageListApiModel

interface FileApiService {
    suspend fun upload(
        entityType: FileEntityType,
        entityUid: String,
        localUid: String,
        fileName: String,
        contentType: String,
        imageData: ByteArray,
        isPrimary: Boolean = false,
    ): Result<EntityImageApiModel>

    suspend fun getImages(
        entityType: FileEntityType,
        entityUid: String,
        lastSync: String?,
    ): Result<EntityImageListApiModel>

    suspend fun deleteImage(imageUid: String): Result<Unit>

    suspend fun setPrimaryImage(imageUid: String): Result<EntityImageApiModel>
}
