package com.ampairs.file.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.file.db.entity.FileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FileEntity>)

    @Query("SELECT * FROM file_items WHERE entityType = :entityType AND entityUid = :entityUid AND softDeleted = 0 ORDER BY displayOrder ASC, createdAt ASC")
    fun observeByEntity(entityType: String, entityUid: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM file_items WHERE entityType = :entityType AND entityUid = :entityUid AND isPrimary = 1 AND softDeleted = 0 LIMIT 1")
    fun observePrimaryByEntity(entityType: String, entityUid: String): Flow<FileEntity?>

    @Query("SELECT * FROM file_items WHERE entityType = :entityType AND entityUid = :entityUid AND softDeleted = 0 ORDER BY displayOrder ASC, createdAt ASC")
    suspend fun getByEntity(entityType: String, entityUid: String): List<FileEntity>

    @Query("SELECT * FROM file_items WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): FileEntity?

    @Query("SELECT * FROM file_items WHERE (uploadStatus = 'PENDING' OR uploadStatus = 'FAILED') AND softDeleted = 0")
    suspend fun getPendingUploads(): List<FileEntity>

    @Query("SELECT * FROM file_items WHERE softDeleted = 1 AND synced = 0")
    suspend fun getPendingDeletes(): List<FileEntity>

    @Query("UPDATE file_items SET uploadStatus = :status WHERE uid = :uid")
    suspend fun updateUploadStatus(uid: String, status: String)

    @Query("UPDATE file_items SET imageUrl = :imageUrl, thumbnailUrl = :thumbnailUrl, uploadStatus = 'COMPLETED', synced = 1 WHERE uid = :uid")
    suspend fun markUploaded(uid: String, imageUrl: String, thumbnailUrl: String)

    @Query("UPDATE file_items SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("UPDATE file_items SET softDeleted = 1, synced = 0 WHERE uid = :uid")
    suspend fun softDelete(uid: String)

    @Query("UPDATE file_items SET isPrimary = 0 WHERE entityType = :entityType AND entityUid = :entityUid")
    suspend fun clearPrimaryStatus(entityType: String, entityUid: String)

    @Query("UPDATE file_items SET isPrimary = 1 WHERE uid = :uid")
    suspend fun setPrimary(uid: String)

    @Query("DELETE FROM file_items WHERE entityType = :entityType AND entityUid = :entityUid")
    suspend fun deleteByEntity(entityType: String, entityUid: String)

    /** Permanent hard-delete by uid (used to remove server-removed images during pull). */
    @Query("DELETE FROM file_items WHERE uid = :uid")
    suspend fun hardDeleteByUid(uid: String)

    @Query("DELETE FROM file_items")
    suspend fun deleteAll()
}
