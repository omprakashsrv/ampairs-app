package com.ampairs.file.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "file_items",
    indices = [
        Index(value = ["entityType", "entityUid"]),
        Index(value = ["uploadStatus"]),
        Index(value = ["entityType", "entityUid", "isPrimary"]),
        Index(value = ["synced"]),
    ]
)
data class FileEntity(
    @PrimaryKey val uid: String,
    val entityType: String,
    val entityUid: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long = 0L,
    val isPrimary: Int = 0,
    val displayOrder: Int = 0,
    val imageUrl: String = "",
    val thumbnailUrl: String = "",
    val uploadStatus: String = "PENDING",
    val localPath: String? = null,
    val synced: Int = 0,
    val softDeleted: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
)
