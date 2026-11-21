package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.JourneyPlan

fun JourneyPlanEntity.toJourneyPlan(): JourneyPlan = JourneyPlan(
    uid = id, repMemberUid = repMemberUid, beatUid = beatUid, weekday = weekday,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun JourneyPlan.toEntity(): JourneyPlanEntity = JourneyPlanEntity(
    id = uid, repMemberUid = repMemberUid, beatUid = beatUid, weekday = weekday,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)
