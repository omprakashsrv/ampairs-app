package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.BeatOutlet

fun BeatOutletEntity.toBeatOutlet(): BeatOutlet = BeatOutlet(
    uid = id, beatUid = beatUid, customerUid = customerUid, visitSequence = visitSequence,
    visitDay = visitDay, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun BeatOutlet.toEntity(): BeatOutletEntity = BeatOutletEntity(
    id = uid, beatUid = beatUid, customerUid = customerUid, visitSequence = visitSequence,
    visitDay = visitDay, active = active, createdAt = createdAt, updatedAt = updatedAt,
)
