package com.ampairs.sync

import co.touchlab.kermit.Logger
import com.ampairs.common.di.AppScope
import com.ampairs.sync.db.SyncPersistStatus
import com.ampairs.sync.db.SyncStateDao
import com.ampairs.sync.db.SyncStateDatabase
import com.ampairs.sync.db.SyncStateEntity
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile
import kotlin.time.Clock

private val log = Logger.withTag("CentralSyncService")

/**
 * Central coordinator for all offline-first background sync.
 *
 * Design principles:
 * - Sync state is persisted in Room so pending work survives process death.
 * - ViewModels never call APIs directly — they emit SyncEvents.
 * - Push-only on launch/reconnect: only entities with PENDING_PUSH are synced automatically.
 * - Pulls are exclusively driven by backend WebSocket events via [onBackendEvent].
 * - No periodic polling — state drives sync.
 */
@Inject
@SingleIn(AppScope::class)
class CentralSyncService {
    // Resolved lazily on first sync event — avoids forcing database creation during workspace switch.
    @Volatile
    private var delegatesResolver: () -> Map<SyncEntity, SyncDelegate> = { emptyMap() }
    @Volatile
    private var resolvedDelegates: Map<SyncEntity, SyncDelegate>? = null

    fun setDelegates(resolver: () -> Map<SyncEntity, SyncDelegate>) {
        delegatesResolver = resolver
        resolvedDelegates = null
    }

    private val delegates: Map<SyncEntity, SyncDelegate>
        get() = resolvedDelegates ?: delegatesResolver().also { resolvedDelegates = it }
    private val _syncStates = MutableStateFlow<Map<SyncEntity, EntitySyncState>>(emptyMap())
    val syncStates: StateFlow<Map<SyncEntity, EntitySyncState>> = _syncStates.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<SyncLogEntry>>(emptyList())
    val syncLogs: StateFlow<List<SyncLogEntry>> = _syncLogs.asStateFlow()

    private val eventChannel = Channel<SyncEvent>(capacity = Channel.BUFFERED)

    // Per-entity mutexes so concurrent pushes/pulls serialize instead of the second being dropped.
    private val pushMutexes = SyncEntity.entries.associateWith { Mutex() }
    private val pullMutexes = SyncEntity.entries.associateWith { Mutex() }

    private var scope: CoroutineScope? = null
    private var dao: SyncStateDao? = null

    // region — Lifecycle

    /**
     * Must be called after workspace selection with the workspace's SyncStateDatabase.
     * Restores persisted states, then kicks off pending syncs and event processing.
     */
    fun start(db: SyncStateDatabase) {
        stop() // clean up any prior session

        val newScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope = newScope

        val syncDao = db.syncStateDao()
        dao = syncDao

        newScope.launch {
            initializeStates()
            processEvents()
        }

        // Reactively observe SyncStateEntity — only fire push triggers for entities that *newly*
        // enter PENDING_PUSH. Pulls are driven exclusively by WebSocket events.
        // Delta set keyed by entityName prevents re-triggering a push that's already in progress.
        newScope.launch {
            var previousPushSet = emptySet<SyncEntity>()
            syncDao.observeAll()
                .map { rows ->
                    rows.filter { it.statusName == SyncPersistStatus.PENDING_PUSH }
                        .map { it.entityName }
                        .toSet()
                }
                .collect { currentPushSet ->
                    val newlyPending = currentPushSet - previousPushSet
                    previousPushSet = currentPushSet
                    if (newlyPending.isNotEmpty()) {
                        log.d { "Observer fired (new pending push): ${newlyPending.map { it.name }}" }
                        newlyPending.forEach { entity -> emit(SyncEvent.TriggerPush(entity)) }
                    }
                }
        }

        log.i { "Started" }
    }

    /**
     * Called by EventSyncBridge when the WebSocket connection is (re)established.
     * Flushes any pending pushes that accumulated while offline. Pulls are not triggered
     * here — they come in via [onBackendEvent] once the server sends WebSocket events.
     */
    fun onConnectionRestored() {
        scope?.launch {
            log.i { "WebSocket reconnected — flushing pending pushes" }
            processPendingPushes()
        }
    }

    /** Must be called on workspace exit / logout. */
    fun stop() {
        scope?.cancel()
        scope = null
        dao = null
        _syncStates.value = emptyMap()
        _syncLogs.value = emptyList()
        log.i { "Stopped" }
    }

    // endregion

    // region — Public API for ViewModels and EventSyncBridge

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
     * Called by EventSyncBridge when a backend WebSocket event arrives.
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
        val persisted = currentDao.getAll()
        val initialMap = persisted.associate { row ->
            row.entityName to EntitySyncState(
                entity = row.entityName,
                status = SyncStatus.from(row.statusName, row.pendingCount),
                lastSyncedAt = row.lastSyncedAt,
                errorMessage = row.errorMessage,
            )
        }
        _syncStates.value = initialMap
        log.i { "States restored: ${initialMap.map { "${it.key.name}=${it.value.status::class.simpleName}" }}" }
    }

    // endregion

    // region — Pending state processing

    private suspend fun processPendingPushes() {
        val currentDao = dao ?: return
        currentDao.getPending()
            .filter { it.statusName == SyncPersistStatus.PENDING_PUSH }
            .forEach { row -> emit(SyncEvent.TriggerPush(row.entityName)) }
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
        pullMutexes[entity]?.withLock {
            updateState(entity) { it.copy(status = SyncStatus.Syncing) }
            appendLog(SyncLogEntry(Clock.System.now().toEpochMilliseconds(), entity, SyncLogEntry.Direction.PULL, SyncLogEntry.Outcome.STARTED, "Pull started"))
            val result = runCatching { delegate.pullFromServer() }.fold(
                onSuccess = { it },
                onFailure = { SyncResult.Failure(it) },
            )
            applyResult(entity, result, wasPull = true)
        }
    }

    private suspend fun executePush(entity: SyncEntity) {
        val delegate = delegates[entity] ?: return
        // Run push dependencies sequentially before acquiring this entity's mutex.
        // Each dependency uses its own mutex, so a concurrently running catalog push will
        // simply be waited on rather than duplicated.
        delegate.pushDependencies.forEach { dep -> executePush(dep) }
        pushMutexes[entity]?.withLock {
            updateState(entity) { it.copy(status = SyncStatus.Syncing) }
            appendLog(SyncLogEntry(Clock.System.now().toEpochMilliseconds(), entity, SyncLogEntry.Direction.PUSH, SyncLogEntry.Outcome.STARTED, "Push started"))
            val result = runCatching { delegate.pushPendingToServer() }.fold(
                onSuccess = { it },
                onFailure = { SyncResult.Failure(it) },
            )
            applyResult(entity, result, wasPull = false)
        }
    }

    private suspend fun executeBackendEvent(entity: SyncEntity, entityId: String, eventType: String) {
        val delegate = delegates[entity] ?: return
        val result = runCatching { delegate.handleBackendEvent(entityId, eventType) }.fold(
            onSuccess = { it },
            onFailure = { SyncResult.Failure(it) },
        )
        applyResult(entity, result, wasPull = true)
    }

    private suspend fun applyResult(entity: SyncEntity, result: SyncResult, wasPull: Boolean) {
        val now = Clock.System.now().toEpochMilliseconds()
        val direction = if (wasPull) SyncLogEntry.Direction.PULL else SyncLogEntry.Direction.PUSH
        when (result) {
            is SyncResult.Success -> {
                updateState(entity) {
                    it.copy(
                        status = SyncStatus.Success(now, if (wasPull) SyncStatus.Direction.PULL else SyncStatus.Direction.PUSH),
                        lastSyncedAt = now,
                        errorMessage = null,
                    )
                }
                persistStatus(entity, SyncStatus.Idle, now)
                appendLog(SyncLogEntry(now, entity, direction, SyncLogEntry.Outcome.SUCCESS, "Synced ${result.count} record(s)"))
                log.i { "${entity.name} sync success (count=${result.count})" }
            }
            is SyncResult.Failure -> {
                val causeChain = generateSequence(result.error) { it.cause }
                    .mapNotNull { it.message?.trim() }
                    .distinct()
                    .toList()
                val errorMsg = causeChain.firstOrNull() ?: "Unknown error"
                val detail = causeChain.drop(1).joinToString(" ← ").ifBlank { null }
                updateState(entity) { it.copy(status = SyncStatus.Failed(errorMsg), errorMessage = errorMsg) }
                // Persist as pending so it retries on next reconnect
                val retryStatus = if (wasPull) SyncPersistStatus.PENDING_PULL else SyncPersistStatus.PENDING_PUSH
                persistPersistStatus(entity, retryStatus, errorMessage = errorMsg)
                appendLog(SyncLogEntry(now, entity, direction, SyncLogEntry.Outcome.FAILURE, errorMsg, detail = detail))
                log.w { "${entity.name} sync failed: $errorMsg${if (detail != null) " ← $detail" else ""}" }
            }
        }
    }

    // endregion

    // region — Log helpers

    private fun appendLog(entry: SyncLogEntry) {
        _syncLogs.update { current ->
            val nextId = (current.lastOrNull()?.id ?: 0L) + 1L
            val next = current + entry.copy(id = nextId)
            if (next.size > 50) next.drop(next.size - 50) else next
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
        scope?.launch { persistStatus(entity, status) }
    }

    private suspend fun persistStatus(entity: SyncEntity, status: SyncStatus, lastSyncedAt: Long? = null) {
        val currentDao = dao ?: return
        val current = _syncStates.value[entity]
        currentDao.upsert(
            SyncStateEntity(
                entityName = entity,
                statusName = SyncStatus.toPersistStatus(status),
                lastSyncedAt = lastSyncedAt ?: current?.lastSyncedAt,
                pendingCount = (status as? SyncStatus.PendingPush)?.count ?: 0,
                errorMessage = null,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
        )
    }

    private suspend fun persistPersistStatus(entity: SyncEntity, status: SyncPersistStatus, errorMessage: String? = null) {
        val currentDao = dao ?: return
        val current = _syncStates.value[entity]
        currentDao.upsert(
            SyncStateEntity(
                entityName = entity,
                statusName = status,
                lastSyncedAt = current?.lastSyncedAt,
                pendingCount = 0,
                errorMessage = errorMessage,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
        )
    }

    // endregion
}
