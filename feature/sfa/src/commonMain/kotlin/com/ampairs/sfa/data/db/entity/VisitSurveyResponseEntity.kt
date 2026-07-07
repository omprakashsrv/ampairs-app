package com.ampairs.sfa.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.sfa.domain.model.VisitSurveyResponse

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

fun VisitSurveyResponseEntity.toVisitSurveyResponse(): VisitSurveyResponse = VisitSurveyResponse(
    uid = id, visitUid = visitUid, repMemberUid = repMemberUid, responses = responses,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun VisitSurveyResponse.toEntity(): VisitSurveyResponseEntity = VisitSurveyResponseEntity(
    id = uid, visitUid = visitUid, repMemberUid = repMemberUid, responses = responses,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)
