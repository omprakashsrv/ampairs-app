package com.ampairs.tally.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ampairs.tally.db.entity.SyncAdapterEntity

@Dao
interface SyncAdapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncAdapter: SyncAdapterEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(syncAdapters: List<SyncAdapterEntity>)
    
    @Update
    suspend fun update(syncAdapter: SyncAdapterEntity)
    
    @Query("SELECT * FROM syncAdapterEntity")
    suspend fun selectAll(): List<SyncAdapterEntity>
    
    @Query("SELECT * FROM syncAdapterEntity WHERE id = :id")
    suspend fun selectById(id: String): SyncAdapterEntity?
    
    @Query("SELECT * FROM syncAdapterEntity WHERE name = :name")
    suspend fun selectByName(name: String): SyncAdapterEntity?
    
    @Query("SELECT * FROM syncAdapterEntity WHERE sync_frequency = :frequency")
    suspend fun selectBySyncFrequency(frequency: String): List<SyncAdapterEntity>
    
    @Query("SELECT * FROM syncAdapterEntity WHERE last_synced = ''")
    suspend fun selectNeverSynced(): List<SyncAdapterEntity>
    
    @Query("SELECT * FROM syncAdapterEntity WHERE last_synced != '' ORDER BY last_synced DESC")
    suspend fun selectBySyncTime(): List<SyncAdapterEntity>
    
    @Query("UPDATE syncAdapterEntity SET last_sync_id = :lastSyncId, last_synced = :lastSynced, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, lastSyncId: String, lastSynced: String, updatedAt: String)
    
    @Query("DELETE FROM syncAdapterEntity WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM syncAdapterEntity")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM syncAdapterEntity")
    suspend fun count(): Int
}