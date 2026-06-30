package com.ampairs.sfa.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.sfa.data.db.entity.BeatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BeatDao {
    @Query("SELECT * FROM sfa_beats WHERE active = 1 ORDER BY name ASC")
    fun getAllBeats(): Flow<List<BeatEntity>>

    @Query("SELECT * FROM sfa_beats WHERE id = :id")
    suspend fun getBeatById(id: String): BeatEntity?

    @Query("SELECT * FROM sfa_beats WHERE synced = 0")
    suspend fun getUnsyncedBeats(): List<BeatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeat(beat: BeatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeats(beats: List<BeatEntity>)

    @Query("DELETE FROM sfa_beats WHERE id = :id")
    suspend fun hardDeleteBeat(id: String)
}
