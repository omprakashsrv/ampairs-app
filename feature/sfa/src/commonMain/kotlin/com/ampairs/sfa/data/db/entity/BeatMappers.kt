package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.Beat

fun BeatEntity.toBeat(): Beat = Beat(
    uid = id, name = name, description = description, repMemberUid = repMemberUid,
    scheduledDays = scheduledDays, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun Beat.toEntity(): BeatEntity = BeatEntity(
    id = uid, name = name, description = description, repMemberUid = repMemberUid,
    scheduledDays = scheduledDays, active = active, createdAt = createdAt, updatedAt = updatedAt,
)
