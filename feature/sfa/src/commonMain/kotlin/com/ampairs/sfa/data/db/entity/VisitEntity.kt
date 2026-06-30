package com.ampairs.sfa.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.sfa.domain.model.Visit

@Entity(
    tableName = "sfa_visits",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_visit_id_idx"),
        Index(value = ["customer_uid"], name = "sfa_visit_customer_idx"),
        Index(value = ["rep_member_uid"], name = "sfa_visit_rep_idx"),
    ],
)
data class VisitEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "customer_uid") val customerUid: String,
    @ColumnInfo(name = "rep_member_uid") val repMemberUid: String,
    @ColumnInfo(name = "planned_visit_uid") val plannedVisitUid: String? = null,
    @ColumnInfo(name = "outcome") val outcome: String,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "distance_meters") val distanceMeters: Double? = null,
    @ColumnInfo(name = "geo_fence_status") val geoFenceStatus: String,
    @ColumnInfo(name = "ad_hoc") val adHoc: Boolean = false,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "order_uid") val orderUid: String? = null,
    @ColumnInfo(name = "visited_at") val visitedAt: String,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

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
