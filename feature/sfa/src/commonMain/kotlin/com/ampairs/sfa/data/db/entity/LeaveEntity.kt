package com.ampairs.sfa.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.sfa.domain.model.Leave

@Entity(
    tableName = "sfa_leaves",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_leave_id_idx"),
        Index(value = ["rep_member_uid"], name = "sfa_leave_rep_idx"),
    ],
)
data class LeaveEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "rep_member_uid") val repMemberUid: String,
    @ColumnInfo(name = "leave_date") val leaveDate: String,
    @ColumnInfo(name = "reason") val reason: String? = null,
    @ColumnInfo(name = "marked_by") val markedBy: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

fun LeaveEntity.toLeave(): Leave = Leave(
    uid = id, repMemberUid = repMemberUid, leaveDate = leaveDate, reason = reason,
    markedBy = markedBy, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun Leave.toEntity(): LeaveEntity = LeaveEntity(
    id = uid, repMemberUid = repMemberUid, leaveDate = leaveDate, reason = reason,
    markedBy = markedBy, active = active, createdAt = createdAt, updatedAt = updatedAt,
)
