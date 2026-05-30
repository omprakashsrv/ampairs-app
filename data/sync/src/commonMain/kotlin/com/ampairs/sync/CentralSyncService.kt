package com.ampairs.sync

import co.touchlab.kermit.Logger
import com.ampairs.common.di.AppScope
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateEntity
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private val log = Logger.withTag("CentralSyncService")

/**
 * Central coordinator for all offline-first background sync.
 *
 * Design principles:
 * - Sync state is persisted in Room so pending work survives process death.
 * - ViewModels never call APIs directly — they emit SyncEvents.
 * - Two sources of truth for "what needs syncing":
 *     1. Local write → marks entity PENDING_PUSH
 *     2. Backend WebSocket event → marks entity PENDING_PULL
 * - Sync is triggered on: app start, network reconnect, WebSocket reconnect.
 * - No periodic polling — state drives sync.
 */
@Inject
@SingleIn(AppScope::class)
class CentralSyncService(
    private val delegates: Map<SyncEntity, @JvmSuppressWildcards SyncDelegate>,
    private val dbFactory: SyncDatabaseFactory,
) {
    private val _syncStates = MutableStateFlow<Map<SyncEntity, EntitySyncState>>(emptyMap())
    val syncStates: StateFlow<Map<SyncEntity, EntitySyncState>> = _syncStates.asStateFlow()

    private val eventChannel = Channel<SyncEvent>(capacity = Channel.BUFFERED)

    private var scope: CoroutineScope? = null
    private var dao: SyncStateDao? = null
    private var pendingCountJobs = mutableListOf<Job>()

    // region — Lifecycle

    /**
     * Must be called after workspace selection. Opens the sync-state DB for the workspace,
     * restores persisted states, then kicks off pending syncs and event processing.
     */
    fun start(workspaceSlug: String) {
        stop() // clean up any prior session

        val newScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope = newScope

        val db = dbFactory.create(workspaceSlug)
        dao = db.syncStateDao()

        newScope.launch {
            initializeStates()
            observePendingCounts(newScope)
            processEvents()
        }

        log.i { "Started for workspace: $workspaceSlug" }
    }

    /**
     * Called by EventConnectionManager when the WebSocket connection is (re)established.
     * Re-processes any persisted PENDING_* states so they sync after network recovery.
     */
    fun onConnectionRestored() {
        scope?.launch {
            log.i { "WebSocket reconnected — processing pending states" }
            processPendingStates()
        }
    }

    /** Must be called on workspace exit / logout. */
    fun stop() {
        pendingCountJobs.forEach { it.cancel() }
        pendingCountJobs.clear()
        scope?.cancel()
        scope = null
        dao = null
        _syncStates.value = emptyMap()
        log.i { "Stopped" }
    }

    // endregion

    // region — Public API for ViewModels and EventConnectionManager

    /**
     * Emit a sync event. Non-suspending so ViewModels can call from any context.
     * Drops silently if the service is not started (scope is null).
     */
    fun emit(event: SyncEvent) {
        scope?.launch { eventChannel.send(event) }
    }

    /**
     * Mark an entity as needing a server push (called after a local write).
     * Persists the state immediately so it survives process death.
     */
    fun markPendingPush(entity: SyncEntity) {
        updateAndPersistStatus(entity, SyncStatus.PendingPush())
        emit(SyncEvent.TriggerPush(entity))
    }

    /**
     * Called by EventConnectionManager when a backend WebSocket event arrives.
     * Routes to the matching SyncDelegate after marking state as PENDING_PULL.
     */
    fun onBackendEvent(entityType: String, entityId: String, eventType: String) {
        val entity = SyncEntity.fromEntityType(entityType) ?: run {
            log.d { "Ignoring unknown entityType: $entityType" }
            return
        }
        updateAndPersistStatus(entity, SyncStatus.PendingPull)
        emit(SyncEvent.BackendEventReceived(entityType, entityId, eventType))
    }

    /** Observe the live sync state for a specific entity. */
    fun observeEntity(entity: SyncEntity): Flow<EntitySyncState?> =
        _syncStates.map { it[entity] }

    // endregion

    // region — Initialization & pending state processing

    private suspend fun initializeStates() {
        val currentDao = dao ?: return
        val persisted = currentDao.getAll().associateBy { it.entityName }

        val initialMap = delegates.keys.associateWith { entity ->
            val row = persisted[entity.name]
            EntitySyncState(
                entity = entity,
                status = if (row != null)
                    SyncStatus.fromName(row.statusName, row.pendingCount)
                else
                    SyncStatus.Idle,
                lastSyncedAt = row?.lastSyncedAt,
                errorMessage = row?.errorMessage,
            )
        }
        _syncStates.value = initialMap
        log.i { "States restored: ${initialMap.map { "${it.key.name}=${it.value.status::class.simpleName}" }}" }
    }

    // endregion

    // region — Pending state processing

    private suspend fun processPendingStates() {
        val currentDao = dao ?: return
        val pending = currentDao.getPending()
        pending.forEach { row ->
            val entity = SyncEntity.entries.firstOrNull { it.name == row.entityName } ?: return@forEach
            when (row.statusName) {
                "PENDING_PUSH" -> emit(SyncEvent.TriggerPush(entity))
                "PENDING_PULL", "FAILED" -> emit(SyncEvent.TriggerPull(entity))
            }
        }
    }

    // endregion

    // region — Pending count observation

    private fun observePendingCounts(scope: CoroutineScope) {
        delegates.forEach { (entity, delegate) ->
            val job = scope.launch {
                delegate.observePendingCount().collect { count ->
                    if (count > 0) {
                        updateAndPersistStatus(entity, SyncStatus.PendingPush(count))
                    } else {
                        val current = _syncStates.value[entity]?.status
                        if (current is SyncStatus.PendingPush) {
                            updateAndPersistStatus(entity, SyncStatus.Idle)
                        }
                    }
                }
            }
            pendingCountJobs.add(job)
        }
    }

    // endregion

    // region — Event processing loop

    private suspend fun processEvents() {
        for (event in eventChannel) {
            when (event) {
                is SyncEvent.TriggerPull -> scope?.launch { executePull(event.entity) }
                is SyncEvent.TriggerPush -> scope?.launch { executePush(event.entity) }
                is SyncEvent.TriggerFullSync -> {
                    val targets = if (event.entity != null) listOf(event.entity) else delegates.keys.toList()
                    targets.forEach { entity ->
                        scope?.launch { executePull(entity) }
                        scope?.launch { executePush(entity) }
                    }
                }
                is SyncEvent.BackendEventReceived -> {
                    val entity = SyncEntity.fromEntityType(event.entityType) ?: continue
                    scope?.launch { executeBackendEvent(entity, event.entityId, event.eventType) }
                }
                SyncEvent.WorkspaceSwitched -> { /* handled by stop()/start() */ }
            }
        }
    }

    // endregion

    // region — Sync execution

    private suspend fun executePull(entity: SyncEntity) {
        val delegate = delegates[entity] ?: return
        if (_syncStates.value[entity]?.status is SyncStatus.Syncing) return // already in-flight
        updateAndPersistStatus(entity, SyncStatus.Syncing)

        val result = runCatching { delegate.pullFromServer() }.fold(
            onSuccess = { it },
            onFailure = { SyncResult.Failure(it) },
        )
        applyResult(entity, result, wasPull = true)
    }

    private suspend fun executePush(entity: SyncEntity) {
        val delegate = delegates[entity] ?: return
        updateAndPersistStatus(entity, SyncStatus.Syncing)

        val result = runCatching { delegate.pushPendingToServer() }.fold(
            onSuccess = { it },
            onFailure = { SyncResult.Failure(it) },
        )
        applyResult(entity, result, wasPull = false)
    }

    private suspend fun executeBackendEvent(entity: SyncEntity, entityId: String, eventType: String) {
        val delegate = delegates[entity] ?: return
        val result = runCatching { delegate.handleBackendEvent(entityId, eventType) }.fold(
            onSuccess = { it },
            onFailure = { SyncResult.Failure(it) },
        )
        applyResult(entity, result, wasPull = true)
    }

    private fun applyResult(entity: SyncEntity, result: SyncResult, wasPull: Boolean) {
        val now = Clock.System.now().toEpochMilliseconds()
        when (result) {
            is SyncResult.Success -> {
                updateState(entity) {
                    it.copy(
                        status = SyncStatus.Success(now),
                        lastSyncedAt = now,
                        errorMessage = null,
                    )
                }
                persistStatus(entity, SyncStatus.Idle, now)
                log.i { "${entity.name} sync success (count=${result.count})" }
            }
            is SyncResult.Failure -> {
                val errorMsg = result.error.message ?: "Unknown error"
                updateState(entity) { it.copy(status = SyncStatus.Failed(errorMsg), errorMessage = errorMsg) }
                // Persist as pending so it retries on next reconnect
                val retryStatus = if (wasPull) "PENDING_PULL" else "PENDING_PUSH"
                persistStatusByName(entity, retryStatus, errorMessage = errorMsg)
                log.w { "${entity.name} sync failed: $errorMsg" }
            }
        }
    }

    // endregion

    // region — State helpers

    private fun updateState(entity: SyncEntity, transform: (EntitySyncState) -> EntitySyncState) {
        _syncStates.update { current ->
            val existing = current[entity] ?: EntitySyncState(entity)
            current + (entity to transform(existing))
        }
    }

    private fun updateAndPersistStatus(entity: SyncEntity, status: SyncStatus) {
        updateState(entity) { it.copy(status = status) }
        persistStatus(entity, status)
    }

    private fun persistStatus(entity: SyncEntity, status: SyncStatus, lastSyncedAt: Long? = null) {
        scope?.launch {
            val currentDao = dao ?: return@launch
            val current = _syncStates.value[entity]
            currentDao.upsert(
                SyncStateEntity(
                    entityName = entity.name,
                    statusName = SyncStatus.toPersistedName(status),
                    lastSyncedAt = lastSyncedAt ?: current?.lastSyncedAt,
                    pendingCount = (status as? SyncStatus.PendingPush)?.count ?: 0,
                    errorMessage = null,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                )
            )
        }
    }

    private fun persistStatusByName(entity: SyncEntity, statusName: String, errorMessage: String? = null) {
        scope?.launch {
            val currentDao = dao ?: return@launch
            val current = _syncStates.value[entity]
            currentDao.upsert(
                SyncStateEntity(
                    entityName = entity.name,
                    statusName = statusName,
                    lastSyncedAt = current?.lastSyncedAt,
                    pendingCount = 0,
                    errorMessage = errorMessage,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                )
            )
        }
    }

    // endregion
}
