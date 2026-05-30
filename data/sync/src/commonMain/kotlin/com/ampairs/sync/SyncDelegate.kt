package com.ampairs.sync

import kotlinx.coroutines.flow.Flow

/**
 * Contract that every offline-capable feature module must implement.
 * Register via @Inject @ContributesIntoMap(AppScope::class) @SyncEntityKey(SyncEntity.X).
 */
interface SyncDelegate {
    val entity: SyncEntity

    /** Pull the latest data from the server into local Room DB. */
    suspend fun pullFromServer(): SyncResult

    /** Push locally unsynced rows (synced=false) to the server. */
    suspend fun pushPendingToServer(): SyncResult

    /**
     * Handle a backend WebSocket event for this entity.
     * Called when another device creates/updates/deletes a record.
     */
    suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult

    /** Emits the current count of locally unsynced rows. Used to drive PendingPush status. */
    fun observePendingCount(): Flow<Int>
}
