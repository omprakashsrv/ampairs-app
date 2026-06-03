package com.ampairs.file.api

data class FileItem(
    val uid: String,
    val entityType: FileEntityType,
    val entityUid: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long = 0L,
    val isPrimary: Boolean = false,
    val displayOrder: Int = 0,
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val uploadStatus: FileUploadStatus = FileUploadStatus.PENDING,
    val localPath: String? = null,
    val synced: Boolean = false,
)
