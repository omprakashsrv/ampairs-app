package com.ampairs.sfa.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.sfa.domain.model.Beat

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

fun BeatEntity.toBeat(): Beat = Beat(
    uid = id, name = name, description = description, repMemberUid = repMemberUid,
    scheduledDays = scheduledDays, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun Beat.toEntity(): BeatEntity = BeatEntity(
    id = uid, name = name, description = description, repMemberUid = repMemberUid,
    scheduledDays = scheduledDays, active = active, createdAt = createdAt, updatedAt = updatedAt,
)
