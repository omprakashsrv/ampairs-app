package com.ampairs.sfa.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.sfa.data.db.entity.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM sfa_visits WHERE active = 1 ORDER BY visited_at DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM sfa_visits WHERE id = :id")
    suspend fun getVisitById(id: String): VisitEntity?

    @Query("SELECT * FROM sfa_visits WHERE synced = 0")
    suspend fun getUnsyncedVisits(): List<VisitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Query("DELETE FROM sfa_visits WHERE id = :id")
    suspend fun hardDeleteVisit(id: String)
}
