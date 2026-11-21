package com.ampairs.sfa.data.db.entity

import com.ampairs.sfa.domain.model.Visit

fun VisitEntity.toVisit(): Visit = Visit(
    uid = id, customerUid = customerUid, repMemberUid = repMemberUid, plannedVisitUid = plannedVisitUid,
    outcome = outcome, latitude = latitude, longitude = longitude, distanceMeters = distanceMeters,
    geoFenceStatus = geoFenceStatus, adHoc = adHoc, notes = notes, orderUid = orderUid,
    visitedAt = visitedAt, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun Visit.toEntity(): VisitEntity = VisitEntity(
    id = uid, customerUid = customerUid, repMemberUid = repMemberUid, plannedVisitUid = plannedVisitUid,
    outcome = outcome, latitude = latitude, longitude = longitude, distanceMeters = distanceMeters,
    geoFenceStatus = geoFenceStatus, adHoc = adHoc, notes = notes, orderUid = orderUid,
    visitedAt = visitedAt, active = active, createdAt = createdAt, updatedAt = updatedAt,
)
