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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
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
 * - Pushes: entities entering PENDING_PUSH after a local write are synced automatically; on
 *   startup/reconnect persisted pending states are replayed (push → pull per entity).
 * - Pulls are driven two ways: (1) live backend WebSocket events via [onBackendEvent], and
 *   (2) checkpoint reconciliation via [reconcileCheckpoints] on connect / reconnect / hourly,
 *   which pulls only entities the server has advanced past (in dependency order).
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

    // The server checkpoint (ISO-8601 max updatedAt) each entity was last pulled up to this session.
    // Lets repeated bootstraps (hourly / reconnect) skip entities the server hasn't advanced past.
    // Both sides come from the checkpoint endpoint, so the string comparison is exact. Reset in stop().
    private val lastPulledCheckpoints = mutableMapOf<SyncEntity, String>()
    private val reconcileMutex = Mutex()

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

        // Replay persisted pending states — push first so local unsynced data reaches the
        // server before we overwrite anything with a pull (e.g. after process death).
        newScope.launch { processPendingStates() }

        // Reactively observe SyncStateEntity — only fire push triggers for entities that *newly*
        // enter PENDING_PUSH. Pulls are driven by WebSocket events, checkpoint reconciliation, and
        // the one-shot processPendingStates() above for process-death recovery.
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
     * Flushes pending pushes first so local unsynced data reaches the server before
     * any pull overwrites it. Also replays persisted PENDING_PULL states (process-death recovery).
     * Checkpoint reconciliation ([reconcileCheckpoints]) is triggered separately by EventSyncBridge.
     */
    fun onConnectionRestored() {
        scope?.launch {
            log.i { "WebSocket reconnected — flushing pending states (push → pull per entity)" }
            processPendingStates()
        }
    }

    /** Must be called on workspace exit / logout. */
    fun stop() {
        scope?.cancel()
        scope = null
        dao = null
        _syncStates.value = emptyMap()
        _syncLogs.value = emptyList()
        lastPulledCheckpoints.clear()
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
     * Mark an entity as needing a server pull and trigger it.
     * Used when a backend signal indicates this entity changed.
     */
    fun markPendingPull(entity: SyncEntity) {
        updateAndPersistStatus(entity, SyncStatus.PendingPull)
        emit(SyncEvent.TriggerPull(entity))
    }

    /**
     * Bootstrap reconciliation. Called on workspace connect, on reconnect, and on the hourly timer
     * with the server's `max(updatedAt)` per entity (ISO-8601 string, or null when the entity has
     * no rows). For each entity whose server checkpoint is ahead of what was last pulled this
     * session, schedules a pull. Pulls run in dependency order (parents before dependents).
     */
    fun reconcileCheckpoints(serverCheckpoints: Map<SyncEntity, String?>) {
        val currentScope = scope ?: return
        currentScope.launch {
            // Serialize reconciles (connect + reconnect + hourly can overlap) so the skip decisions
            // and the lastPulledCheckpoints map stay consistent.
            reconcileMutex.withLock {
                val laggards = mutableSetOf<SyncEntity>()
                for (entity in delegates.keys) {
                    val server = serverCheckpoints[entity]
                    if (server.isNullOrBlank()) continue // no server data → never pull
                    val pulled = lastPulledCheckpoints[entity]
                    // Skip when the server hasn't advanced past what we already pulled this session.
                    // Both values come from the checkpoint endpoint, so the ISO-8601 compare is exact.
                    if (pulled == null || server > pulled) {
                        laggards.add(entity)
                    }
                }

                if (laggards.isEmpty()) {
                    log.i { "Bootstrap reconcile: all entities in sync" }
                    return@withLock
                }
                log.i { "Bootstrap reconcile: pulling ${laggards.map { it.name }}" }
                laggards.forEach { entity ->
                    updateState(entity) { it.copy(status = SyncStatus.PendingPull) }
                    persistPersistStatus(entity, SyncPersistStatus.PENDING_PULL)
                }
                pullEntities(laggards, serverCheckpoints)
            }
        }
    }

    /**
     * Called by EventSyncBridge when a backend WebSocket event arrives.
     * Routes to the matching SyncDelegate after marking state as PENDING_PULL.
     *
     * [lastUpdatedAt] is the slim signal's change watermark (ISO-8601, diagnostic). We always pull
     * on a live signal — the incremental pull returns only new rows, and the safe skip-when-in-sync
     * comparison lives in the bootstrap reconcile (which compares same-source watermarks). The live
     * signal's time is the server's emit time, not the entity's updatedAt, so it is not used to skip.
     */
    fun onBackendEvent(
        entityType: String,
        entityId: String,
        eventType: String,
        lastUpdatedAt: String? = null,
    ) {
        val entity = SyncEntity.fromEntityType(entityType) ?: run {
            log.d { "Ignoring unknown entityType: $entityType" }
            return
        }
        log.d { "Backend event ${entity.name} (watermark=$lastUpdatedAt) — scheduling pull" }
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

    /**
     * Replays persisted pending states on startup or reconnect.
     * For each entity that has pending work, push runs before pull so that local unsynced
     * data reaches the server before a pull could overwrite it. Different entities run
     * concurrently; push → pull ordering is enforced within each entity's coroutine.
     */
    private suspend fun processPendingStates() {
        val currentDao = dao ?: return
        val pending = currentDao.getPending()
        if (pending.isEmpty()) return

        val pushEntities = pending
            .filter { it.statusName == SyncPersistStatus.PENDING_PUSH }
            .map { it.entityName }.toSet()
        val pullEntities = pending
            .filter { it.statusName == SyncPersistStatus.PENDING_PULL }
            .map { it.entityName }.toSet()

        log.d { "Replaying pending states — push: ${pushEntities.map { it.name }}, pull: ${pullEntities.map { it.name }}" }

        (pushEntities + pullEntities).forEach { entity ->
            scope?.launch {
                if (entity in pushEntities) executePush(entity)
                if (entity in pullEntities) executePull(entity)
            }
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
                        // Push local changes first, then pull — so local edits reach the server
                        // before the pull, and the pull never races ahead of an unpushed change.
                        scope?.launch {
                            executePush(entity)
                            executePull(entity)
                        }
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

    private suspend fun executePull(entity: SyncEntity): Boolean {
        val delegate = delegates[entity] ?: return false
        return pullMutexes[entity]?.withLock {
            updateState(entity) { it.copy(status = SyncStatus.Syncing) }
            appendLog(SyncLogEntry(Clock.System.now().toEpochMilliseconds(), entity, SyncLogEntry.Direction.PULL, SyncLogEntry.Outcome.STARTED, "Pull started"))
            val result = runCatching { delegate.pullFromServer() }.fold(
                onSuccess = { it },
                onFailure = { SyncResult.Failure(it) },
            )
            applyResult(entity, result, wasPull = true)
            result is SyncResult.Success
        } ?: false
    }

    /**
     * Pull a batch of entities in dependency order. Entities whose in-batch dependencies are all
     * satisfied are pulled together in a wave; the next wave starts only after the current one
     * completes. If a dependency's pull fails, its dependents are held (left PENDING_PULL) and
     * retried on the next reconcile cycle (reconnect / hourly).
     *
     * On a successful pull the entity's target server checkpoint is recorded so the next bootstrap
     * can skip it while the server stays at that checkpoint.
     */
    private suspend fun pullEntities(entities: Set<SyncEntity>, checkpoints: Map<SyncEntity, String?>) {
        val pending = entities.filter { delegates[it] != null }.toMutableSet()
        val failed = mutableSetOf<SyncEntity>()
        while (pending.isNotEmpty()) {
            val wave = pending.filter { e ->
                delegates[e]?.dependsOn?.none { dep -> dep in pending || dep in failed } ?: false
            }
            if (wave.isEmpty()) {
                log.w { "Holding ${pending.map { it.name }} — dependencies unavailable or failed" }
                break
            }
            val results = coroutineScope {
                wave.map { e -> async { e to executePull(e) } }.awaitAll()
            }
            results.forEach { (e, ok) ->
                pending.remove(e)
                if (ok) {
                    checkpoints[e]?.let { lastPulledCheckpoints[e] = it }
                } else {
                    failed.add(e)
                }
            }
        }
    }

    /**
     * Push an entity after its push-dependencies, in dependency-safe order. Returns true when this
     * entity's push (and every dependency it needed) succeeded; false when it was deferred because a
     * parent had not yet reached the server.
     *
     * Two properties make cross-resource pushes correct and stop the retry storm:
     *
     * 1. **Per-run memoization** ([attempted]): the push DAG is a diamond — `allocation → invoice`
     *    AND `allocation → voucher → invoice → order`, and `party_balance → (almost) everything`.
     *    Without memoization a single trigger re-walks and re-pushes the same parent many times; if
     *    that parent batch is doomed (parent not yet acked) it fails on every pass — the observed
     *    "same batch retried ~6× in ~150ms". Memoizing per top-level invocation pushes each entity at
     *    most once per run.
     *
     * 2. **Defer-on-unsynced-parent**: a dependency push that does not fully succeed means the parent
     *    is not on the server yet, so pushing the child would hit `fk_invoice_order_ref` (409) /
     *    "voucher not found" (404). We DEFER the child instead — leave it PENDING_PUSH (no hard
     *    failure, no DB write, so the reactive observer does not re-fire) and return false so its own
     *    dependents defer too. When the parent later pushes successfully we re-trigger the child.
     */
    private suspend fun executePush(
        entity: SyncEntity,
        attempted: MutableMap<SyncEntity, Boolean> = mutableMapOf(),
    ): Boolean {
        attempted[entity]?.let { return it }
        // No delegate for this entity (not installed) — treat as a satisfied dependency, don't block.
        val delegate = delegates[entity] ?: run { attempted[entity] = true; return true }

        // Push dependencies first; if any could not fully sync, defer this entity.
        for (dep in delegate.pushDependencies) {
            if (!executePush(dep, attempted)) {
                deferPush(entity, dep)
                attempted[entity] = false
                return false
            }
        }

        val succeeded = pushMutexes[entity]?.withLock {
            updateState(entity) { it.copy(status = SyncStatus.Syncing) }
            appendLog(SyncLogEntry(Clock.System.now().toEpochMilliseconds(), entity, SyncLogEntry.Direction.PUSH, SyncLogEntry.Outcome.STARTED, "Push started"))
            val result = runCatching { delegate.pushPendingToServer() }.fold(
                onSuccess = { it },
                onFailure = { SyncResult.Failure(it) },
            )
            applyResult(entity, result, wasPull = false)
            result is SyncResult.Success
        } ?: false
        attempted[entity] = succeeded

        // A parent just reached the server — re-attempt any still-pending entity that was waiting on
        // it, so a deferred child retries as soon as its parent syncs (not only on the next reconnect).
        if (succeeded) {
            pushDependentsOf(entity).forEach { dependent ->
                val status = _syncStates.value[dependent]?.status
                if (status is SyncStatus.PendingPush || status is SyncStatus.Failed) {
                    emit(SyncEvent.TriggerPush(dependent))
                }
            }
        }
        return succeeded
    }

    /** Entities that declare [entity] among their push dependencies (reverse edges of the push DAG). */
    private fun pushDependentsOf(entity: SyncEntity): List<SyncEntity> =
        delegates.filterValues { entity in it.pushDependencies }.keys.toList()

    /**
     * Mark [entity] as waiting on an unsynced parent. The DB row stays PENDING_PUSH (we never cleared
     * it), so nothing is persisted and the reactive observer does not re-fire — this is what stops the
     * retry storm. Only the in-memory status is refreshed so the UI keeps showing "pending".
     */
    private fun deferPush(entity: SyncEntity, blockedBy: SyncEntity) {
        updateState(entity) { state ->
            val count = (state.status as? SyncStatus.PendingPush)?.count ?: 0
            state.copy(status = SyncStatus.PendingPush(count))
        }
        log.d { "Deferring ${entity.name} push — ${blockedBy.name} not synced yet; will retry after it syncs" }
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
