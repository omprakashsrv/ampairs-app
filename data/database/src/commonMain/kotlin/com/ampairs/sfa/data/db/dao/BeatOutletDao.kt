package com.ampairs.sfa.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.sfa.data.db.entity.BeatOutletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BeatOutletDao {
    @Query("SELECT * FROM sfa_beat_outlets WHERE active = 1 ORDER BY visit_sequence ASC")
    fun getAllBeatOutlets(): Flow<List<BeatOutletEntity>>

    @Query("SELECT * FROM sfa_beat_outlets WHERE beat_uid = :beatUid AND active = 1 ORDER BY visit_sequence ASC")
    fun getOutletsForBeat(beatUid: String): Flow<List<BeatOutletEntity>>

    @Query("SELECT * FROM sfa_beat_outlets WHERE id = :id")
    suspend fun getBeatOutletById(id: String): BeatOutletEntity?

    @Query("SELECT * FROM sfa_beat_outlets WHERE synced = 0")
    suspend fun getUnsyncedBeatOutlets(): List<BeatOutletEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeatOutlet(row: BeatOutletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeatOutlets(rows: List<BeatOutletEntity>)

    @Query("DELETE FROM sfa_beat_outlets WHERE id = :id")
    suspend fun hardDeleteBeatOutlet(id: String)
}
