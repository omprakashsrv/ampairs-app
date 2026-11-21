package com.ampairs.sfa.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "sfa_beats",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_beat_id_idx"),
        Index(value = ["rep_member_uid"], name = "sfa_beat_rep_idx"),
    ],
)
data class BeatEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "rep_member_uid") val repMemberUid: String? = null,
    @ColumnInfo(name = "scheduled_days") val scheduledDays: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)
