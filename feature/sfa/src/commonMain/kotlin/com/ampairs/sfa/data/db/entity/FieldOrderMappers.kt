package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.FieldOrder

fun FieldOrderEntity.toFieldOrder(): FieldOrder = FieldOrder(
    uid = id, visitUid = visitUid, customerUid = customerUid, repMemberUid = repMemberUid,
    orderUid = orderUid, amount = amount, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun FieldOrder.toEntity(): FieldOrderEntity = FieldOrderEntity(
    id = uid, visitUid = visitUid, customerUid = customerUid, repMemberUid = repMemberUid,
    orderUid = orderUid, amount = amount, active = active, createdAt = createdAt, updatedAt = updatedAt,
)
