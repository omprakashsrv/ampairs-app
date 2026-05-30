package com.ampairs.sync.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entity_sync_state")
data class SyncStateEntity(
    @PrimaryKey val entityName: String,   // SyncEntity.name — e.g. "CUSTOMER"
    val statusName: String,               // SyncStatus persisted name — e.g. "PENDING_PUSH"
    val lastSyncedAt: Long? = null,
    val pendingCount: Int = 0,
    val errorMessage: String? = null,
    val updatedAt: Long,
)
