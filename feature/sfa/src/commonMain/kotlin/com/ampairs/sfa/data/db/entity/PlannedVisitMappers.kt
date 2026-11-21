package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.PlannedVisit

fun PlannedVisitEntity.toPlannedVisit(): PlannedVisit = PlannedVisit(
    uid = id, journeyPlanUid = journeyPlanUid, beatUid = beatUid, customerUid = customerUid,
    repMemberUid = repMemberUid, plannedDate = plannedDate, status = status, visitSequence = visitSequence,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun PlannedVisit.toEntity(): PlannedVisitEntity = PlannedVisitEntity(
    id = uid, journeyPlanUid = journeyPlanUid, beatUid = beatUid, customerUid = customerUid,
    repMemberUid = repMemberUid, plannedDate = plannedDate, status = status, visitSequence = visitSequence,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)
