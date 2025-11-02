package com.ampairs.tally.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "syncAdapterEntity",
    indices = [Index(value = ["id"], unique = true)]
)
data class SyncAdapterEntity(
    @PrimaryKey(autoGenerate = true)
    val seq_id: Long = 0,
    val id: String,
    val name: String,
    val sync_frequency: String,
    val last_sync_id: String = "",
    val last_synced: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)