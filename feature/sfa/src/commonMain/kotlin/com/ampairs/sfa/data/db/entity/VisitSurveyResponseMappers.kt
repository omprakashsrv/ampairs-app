package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.VisitSurveyResponse

fun VisitSurveyResponseEntity.toVisitSurveyResponse(): VisitSurveyResponse = VisitSurveyResponse(
    uid = id, visitUid = visitUid, repMemberUid = repMemberUid, responses = responses,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun VisitSurveyResponse.toEntity(): VisitSurveyResponseEntity = VisitSurveyResponseEntity(
    id = uid, visitUid = visitUid, repMemberUid = repMemberUid, responses = responses,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)
