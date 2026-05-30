package com.ampairs.sync.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM entity_sync_state")
    fun observeAll(): Flow<List<SyncStateEntity>>

    @Query("SELECT * FROM entity_sync_state WHERE entityName = :entityName")
    fun observe(entityName: String): Flow<SyncStateEntity?>

    @Query("SELECT * FROM entity_sync_state")
    suspend fun getAll(): List<SyncStateEntity>

    @Query("SELECT * FROM entity_sync_state WHERE statusName IN ('PENDING_PUSH', 'PENDING_PULL', 'FAILED')")
    suspend fun getPending(): List<SyncStateEntity>

    @Upsert
    suspend fun upsert(state: SyncStateEntity)

    @Query("DELETE FROM entity_sync_state")
    suspend fun deleteAll()
}
