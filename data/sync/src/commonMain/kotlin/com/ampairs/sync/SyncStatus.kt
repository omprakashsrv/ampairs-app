package com.ampairs.sync

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
        fun fromName(name: String, pendingCount: Int = 0): SyncStatus = when (name) {
            "IDLE" -> Idle
            "PENDING_PUSH" -> PendingPush(pendingCount)
            "PENDING_PULL" -> PendingPull
            "SYNCING" -> Syncing
            "FAILED" -> Failed("Unknown — restored from disk")
            else -> Idle
        }

        fun toPersistedName(status: SyncStatus): String = when (status) {
            is Idle -> "IDLE"
            is PendingPush -> "PENDING_PUSH"
            is PendingPull -> "PENDING_PULL"
            is Syncing -> "PENDING_PULL" // persist as PENDING so we retry after crash
            is Success -> "IDLE"
            is Failed -> "FAILED"
        }
    }
}
