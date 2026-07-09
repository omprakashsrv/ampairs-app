package com.ampairs.sync.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.ampairs.sync.SyncEntity

@Entity(tableName = "entity_sync_state")
data class SyncStateEntity(
    @PrimaryKey val entityName: SyncEntity,
    val statusName: SyncPersistStatus,
    val lastSyncedAt: Long? = null,
    val lastSyncedAtIso: String? = null,
    val pendingCount: Int = 0,
    val errorMessage: String? = null,
    val updatedAt: Long,
)
