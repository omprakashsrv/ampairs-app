package com.ampairs.sync

data class EntitySyncState(
    val entity: SyncEntity,
    val status: SyncStatus = SyncStatus.Idle,
    val lastSyncedAt: Long? = null,
    val errorMessage: String? = null,
)
