package com.ampairs.sfa.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
