package com.ampairs.sfa.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.sfa.data.db.entity.VisitSurveyResponseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitSurveyResponseDao {
    @Query("SELECT * FROM sfa_visit_survey_responses WHERE active = 1")
    fun getAllVisitSurveyResponses(): Flow<List<VisitSurveyResponseEntity>>

    @Query("SELECT * FROM sfa_visit_survey_responses WHERE id = :id")
    suspend fun getVisitSurveyResponseById(id: String): VisitSurveyResponseEntity?

    @Query("SELECT * FROM sfa_visit_survey_responses WHERE synced = 0")
    suspend fun getUnsyncedVisitSurveyResponses(): List<VisitSurveyResponseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitSurveyResponse(row: VisitSurveyResponseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitSurveyResponses(rows: List<VisitSurveyResponseEntity>)

    @Query("DELETE FROM sfa_visit_survey_responses WHERE id = :id")
    suspend fun hardDeleteVisitSurveyResponse(id: String)
}
