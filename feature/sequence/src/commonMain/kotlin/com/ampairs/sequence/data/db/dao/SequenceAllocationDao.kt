package com.ampairs.sequence.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.sequence.data.db.entity.SequenceAllocationEntity

@Dao
interface SequenceAllocationDao {

    /** Oldest usable block for an entity type on this device (capacity remaining). */
    @Query(
        """
        SELECT * FROM sequence_allocations
        WHERE entity_type = :entityType AND device_id = :deviceId
          AND status = 'ACTIVE' AND next_available <= range_end
        ORDER BY range_start ASC
        LIMIT 1
        """
    )
    suspend fun getUsableAllocation(entityType: String, deviceId: String): SequenceAllocationEntity?

    @Query("SELECT * FROM sequence_allocations WHERE uid = :uid")
    suspend fun getByUid(uid: String): SequenceAllocationEntity?

    /** Allocations whose consumption progress has not been reported to the server yet. */
    @Query("SELECT * FROM sequence_allocations WHERE synced = 0")
    suspend fun getUnsyncedAllocations(): List<SequenceAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: SequenceAllocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<SequenceAllocationEntity>)
}
