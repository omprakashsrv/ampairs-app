package com.ampairs.sfa.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "sfa_journey_plans",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_journey_plan_id_idx"),
        Index(value = ["rep_member_uid"], name = "sfa_journey_plan_rep_idx"),
    ],
)
data class JourneyPlanEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "rep_member_uid") val repMemberUid: String,
    @ColumnInfo(name = "beat_uid") val beatUid: String,
    @ColumnInfo(name = "weekday") val weekday: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)
