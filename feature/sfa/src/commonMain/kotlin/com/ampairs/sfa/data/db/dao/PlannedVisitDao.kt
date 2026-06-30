package com.ampairs.sfa.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.sfa.data.db.entity.PlannedVisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedVisitDao {
    @Query("SELECT * FROM sfa_planned_visits WHERE active = 1 ORDER BY planned_date ASC, visit_sequence ASC")
    fun getAllPlannedVisits(): Flow<List<PlannedVisitEntity>>

    @Query("SELECT * FROM sfa_planned_visits WHERE id = :id")
    suspend fun getPlannedVisitById(id: String): PlannedVisitEntity?

    @Query("SELECT * FROM sfa_planned_visits WHERE synced = 0")
    suspend fun getUnsyncedPlannedVisits(): List<PlannedVisitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedVisit(row: PlannedVisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedVisits(rows: List<PlannedVisitEntity>)

    @Query("DELETE FROM sfa_planned_visits WHERE id = :id")
    suspend fun hardDeletePlannedVisit(id: String)
}
