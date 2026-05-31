package com.ampairs.sync

import com.ampairs.sync.db.SyncPersistStatus

sealed class SyncStatus {
    /** No pending work. */
    data object Idle : SyncStatus()

    /** Local changes exist that have not been pushed to the server. */
    data class PendingPush(val count: Int = 0) : SyncStatus()

    /** Backend event received — fresh data needs to be pulled from server. */
    data object PendingPull : SyncStatus()

    /** Sync is actively in-flight. */
    data object Syncing : SyncStatus()

    /** Last sync completed successfully. */
    data class Success(val syncedAt: Long) : SyncStatus()

    /** Last sync failed — will retry on next trigger. */
    data class Failed(val reason: String) : SyncStatus()

    fun isPending() = this is PendingPush || this is PendingPull

    companion object {
        fun from(status: SyncPersistStatus, pendingCount: Int = 0): SyncStatus = when (status) {
            SyncPersistStatus.IDLE -> Idle
            SyncPersistStatus.PENDING_PUSH -> PendingPush(pendingCount)
            SyncPersistStatus.PENDING_PULL -> PendingPull
            SyncPersistStatus.SYNCING -> Syncing
            SyncPersistStatus.FAILED -> Failed("Unknown — restored from disk")
        }

        fun toPersistStatus(status: SyncStatus): SyncPersistStatus = when (status) {
            is Idle -> SyncPersistStatus.IDLE
            is PendingPush -> SyncPersistStatus.PENDING_PUSH
            is PendingPull -> SyncPersistStatus.PENDING_PULL
            is Syncing -> SyncPersistStatus.IDLE // should not be persisted directly
            is Success -> SyncPersistStatus.IDLE
            is Failed -> SyncPersistStatus.FAILED
        }
    }
}
