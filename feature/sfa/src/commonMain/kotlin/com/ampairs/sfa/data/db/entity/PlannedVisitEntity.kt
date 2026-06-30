package com.ampairs.sfa.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.sfa.domain.model.PlannedVisit

@Entity(
    tableName = "sfa_planned_visits",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_planned_visit_id_idx"),
        Index(value = ["rep_member_uid"], name = "sfa_planned_visit_rep_idx"),
        Index(value = ["planned_date"], name = "sfa_planned_visit_date_idx"),
    ],
)
data class PlannedVisitEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "journey_plan_uid") val journeyPlanUid: String? = null,
    @ColumnInfo(name = "beat_uid") val beatUid: String? = null,
    @ColumnInfo(name = "customer_uid") val customerUid: String,
    @ColumnInfo(name = "rep_member_uid") val repMemberUid: String,
    @ColumnInfo(name = "planned_date") val plannedDate: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "visit_sequence") val visitSequence: Int = 0,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

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
