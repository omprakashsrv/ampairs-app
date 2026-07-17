package com.ampairs.sync.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.ampairs.sync.SyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM entity_sync_state")
    fun observeAll(): Flow<List<SyncStateEntity>>

    @Query("SELECT * FROM entity_sync_state WHERE entityName = :entity")
    fun observe(entity: SyncEntity): Flow<SyncStateEntity?>

    @Query("SELECT * FROM entity_sync_state")
    suspend fun getAll(): List<SyncStateEntity>

    @Query("SELECT * FROM entity_sync_state WHERE statusName IN ('PENDING_PUSH', 'PENDING_PULL', 'FAILED')")
    suspend fun getPending(): List<SyncStateEntity>

    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    /**
     * Persist status/progress fields only — never touches `lastSyncedAtIso`. Used for every
     * routine status transition (syncing/idle/pending/failed) so the pull checkpoint written by a
     * delegate's `pull()` is never clobbered by a status update that runs right after it.
     */
    @Query(
        """
        INSERT INTO entity_sync_state (entityName, statusName, lastSyncedAt, lastSyncedAtIso, pendingCount, errorMessage, updatedAt)
        VALUES (:entity, :status, :lastSyncedAt, NULL, :pendingCount, :errorMessage, :now)
        ON CONFLICT(entityName) DO UPDATE SET
            statusName = :status,
            lastSyncedAt = :lastSyncedAt,
            pendingCount = :pendingCount,
            errorMessage = :errorMessage,
            updatedAt = :now
        """
    )
    suspend fun upsertStatus(
        entity: SyncEntity,
        status: SyncPersistStatus,
        lastSyncedAt: Long?,
        pendingCount: Int,
        errorMessage: String?,
        now: Long,
    )

    @Query("SELECT lastSyncedAtIso FROM entity_sync_state WHERE entityName = :entity")
    suspend fun getLastSyncedAtIso(entity: SyncEntity): String?

    @Query("UPDATE entity_sync_state SET lastSyncedAtIso = :iso WHERE entityName = :entity")
    suspend fun setLastSyncedAtIso(entity: SyncEntity, iso: String)

    /**
     * Mark an entity as PENDING_PUSH so CentralSyncService's reactive observer fires an
     * automatic bulk push. Upsert that preserves `lastSyncedAtIso` (never wipes the pull
     * checkpoint). Repositories call this after any local write instead of touching the API.
     */
    @Query(
        """
        INSERT INTO entity_sync_state (entityName, statusName, lastSyncedAt, lastSyncedAtIso, pendingCount, errorMessage, updatedAt)
        VALUES (:entity, 'PENDING_PUSH', NULL, NULL, 0, NULL, :now)
        ON CONFLICT(entityName) DO UPDATE SET statusName = 'PENDING_PUSH', updatedAt = :now
        """
    )
    suspend fun markPendingPush(entity: SyncEntity, now: Long)

    @Query("DELETE FROM entity_sync_state")
    suspend fun deleteAll()
}
