package com.ampairs.sfa.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.sfa.domain.model.BeatOutlet

@Entity(
    tableName = "sfa_beat_outlets",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_beat_outlet_id_idx"),
        Index(value = ["beat_uid"], name = "sfa_beat_outlet_beat_idx"),
    ],
)
data class BeatOutletEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "beat_uid") val beatUid: String,
    @ColumnInfo(name = "customer_uid") val customerUid: String,
    @ColumnInfo(name = "visit_sequence") val visitSequence: Int = 0,
    @ColumnInfo(name = "visit_day") val visitDay: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

fun BeatOutletEntity.toBeatOutlet(): BeatOutlet = BeatOutlet(
    uid = id, beatUid = beatUid, customerUid = customerUid, visitSequence = visitSequence,
    visitDay = visitDay, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

fun BeatOutlet.toEntity(): BeatOutletEntity = BeatOutletEntity(
    id = uid, beatUid = beatUid, customerUid = customerUid, visitSequence = visitSequence,
    visitDay = visitDay, active = active, createdAt = createdAt, updatedAt = updatedAt,
)
