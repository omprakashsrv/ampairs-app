package com.ampairs.file.api

import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun observeFiles(entityType: FileEntityType, entityUid: String): Flow<List<FileItem>>
    fun observePrimaryFile(entityType: FileEntityType, entityUid: String): Flow<FileItem?>
    suspend fun saveLocally(
        entityType: FileEntityType,
        entityUid: String,
        result: FilePickerResult,
        isPrimary: Boolean = false,
    ): Result<FileItem>
    suspend fun deleteFile(fileUid: String): Result<Unit>
    suspend fun setPrimaryFile(entityType: FileEntityType, entityUid: String, fileUid: String): Result<Unit>
    suspend fun pullFromServer(entityType: FileEntityType, entityUid: String): Result<Int>

    /** Read a stored file's raw bytes from the local cache. Fails if it isn't cached on this device. */
    suspend fun readFileBytes(fileUid: String): Result<ByteArray>
}
