package com.ampairs.sfa.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.sfa.data.db.entity.JourneyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyPlanDao {
    @Query("SELECT * FROM sfa_journey_plans WHERE active = 1")
    fun getAllJourneyPlans(): Flow<List<JourneyPlanEntity>>

    @Query("SELECT * FROM sfa_journey_plans WHERE id = :id")
    suspend fun getJourneyPlanById(id: String): JourneyPlanEntity?

    @Query("SELECT * FROM sfa_journey_plans WHERE synced = 0")
    suspend fun getUnsyncedJourneyPlans(): List<JourneyPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourneyPlan(row: JourneyPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourneyPlans(rows: List<JourneyPlanEntity>)

    @Query("DELETE FROM sfa_journey_plans WHERE id = :id")
    suspend fun hardDeleteJourneyPlan(id: String)
}
