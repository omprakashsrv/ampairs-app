package com.ampairs.sfa.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "sfa_attendance",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_attendance_id_idx"),
        Index(value = ["rep_member_uid"], name = "sfa_attendance_rep_idx"),
    ],
)
data class AttendanceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "rep_member_uid") val repMemberUid: String,
    @ColumnInfo(name = "check_in_at") val checkInAt: String? = null,
    @ColumnInfo(name = "check_in_latitude") val checkInLatitude: Double? = null,
    @ColumnInfo(name = "check_in_longitude") val checkInLongitude: Double? = null,
    @ColumnInfo(name = "check_out_at") val checkOutAt: String? = null,
    @ColumnInfo(name = "check_out_latitude") val checkOutLatitude: Double? = null,
    @ColumnInfo(name = "check_out_longitude") val checkOutLongitude: Double? = null,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)
