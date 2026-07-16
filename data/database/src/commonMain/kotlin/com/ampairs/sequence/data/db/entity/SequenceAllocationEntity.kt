package com.ampairs.sequence.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A server-granted exclusive number block for this device. The format snapshot
 * (prefix/suffix/padding/increment) is copied from the grant response so offline
 * formatting never needs the definition row.
 *
 * synced = false means local consumption progress has not been reported to the
 * server yet (pushed by SequenceSyncDelegate via POST /allocations/report).
 */
@Entity(
    tableName = "sequence_allocations",
    indices = [
        Index(value = ["uid"], unique = true, name = "sequence_allocation_uid_idx"),
        Index(value = ["entity_type", "status"], name = "sequence_allocation_entity_status_idx"),
    ]
)
data class SequenceAllocationEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "definition_uid")
    val definitionUid: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "device_id")
    val deviceId: String,

    @ColumnInfo(name = "range_start")
    val rangeStart: Long,

    @ColumnInfo(name = "range_end")
    val rangeEnd: Long,

    @ColumnInfo(name = "next_available")
    val nextAvailable: Long,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "prefix")
    val prefix: String? = null,

    @ColumnInfo(name = "suffix")
    val suffix: String? = null,

    @ColumnInfo(name = "padding_length")
    val paddingLength: Int = 0,

    @ColumnInfo(name = "increment_step")
    val incrementStep: Int = 1,

    @ColumnInfo(name = "synced")
    val synced: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,
)
