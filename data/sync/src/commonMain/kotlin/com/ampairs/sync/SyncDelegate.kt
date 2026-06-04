package com.ampairs.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Contract that every offline-capable feature module must implement.
 * Register via @Inject @ContributesIntoMap(AppScope::class) @SyncEntityKey(SyncEntity.X).
 */
interface SyncDelegate {
    val entity: SyncEntity

    /**
     * Entities whose push must complete before this entity's push starts.
     * CentralSyncService serializes them in order before acquiring this entity's mutex.
     */
    val pushDependencies: List<SyncEntity> get() = emptyList()

    /**
     * Entities that must be pulled (present locally) before this entity is pulled — its referenced
     * parents (e.g. customer depends on customer_group + customer_type). During bootstrap the
     * CentralSyncService pulls dependencies first, in topological waves; a dependent is held if a
     * dependency's pull fails. Defaults to [pushDependencies] since push wants the same ordering.
     */
    val dependsOn: List<SyncEntity> get() = pushDependencies

    /** Pull the latest data from the server into local Room DB. */
    suspend fun pullFromServer(): SyncResult

    /** Push locally unsynced rows (synced=false) to the server. */
    suspend fun pushPendingToServer(): SyncResult

    /**
     * Handle a backend WebSocket event for this entity.
     * Called when another device creates/updates/deletes a record.
     */
    suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult

    /**
     * Pending push count — driven externally via CentralSyncService.markPendingPush().
     * Override only if reactive DAO observation is preferred for this entity.
     */
    fun observePendingCount(): Flow<Int> = flowOf(0)
}
