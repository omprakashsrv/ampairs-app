package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.Leave

fun LeaveEntity.toLeave(): Leave = Leave(
    uid = id, repMemberUid = repMemberUid, leaveDate = leaveDate, reason = reason,
    markedBy = markedBy, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun Leave.toEntity(): LeaveEntity = LeaveEntity(
    id = uid, repMemberUid = repMemberUid, leaveDate = leaveDate, reason = reason,
    markedBy = markedBy, active = active, createdAt = createdAt, updatedAt = updatedAt,
)
