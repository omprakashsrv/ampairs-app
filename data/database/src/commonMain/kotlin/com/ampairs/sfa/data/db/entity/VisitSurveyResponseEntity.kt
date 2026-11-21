package com.ampairs.sfa.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "sfa_visit_survey_responses",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_visit_survey_id_idx"),
        Index(value = ["visit_uid"], name = "sfa_visit_survey_visit_idx"),
    ],
)
data class VisitSurveyResponseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "visit_uid") val visitUid: String,
    @ColumnInfo(name = "rep_member_uid") val repMemberUid: String? = null,
    @ColumnInfo(name = "responses") val responses: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)
