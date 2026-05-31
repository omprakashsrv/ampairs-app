# /offline-sync — Offline-First Sync Architecture

This skill documents the complete offline-first, push/pull sync architecture used in the Ampairs KMP app.
Apply it whenever adding a new entity, adding a new write operation, or debugging why data isn't reaching the server.

---

## Core Principle

**Every local write triggers an automatic server push via `CentralSyncService`. No ViewModel calls sync APIs directly.**

```
UI Write → ViewModel → Repository (writes to Room with synced=false)
                     ↓
         ViewModel calls syncService.markPendingPush(SyncEntity.X)
                     ↓
         CentralSyncService persists PENDING_PUSH state to SyncStateDatabase
                     ↓
         CentralSyncService emits TriggerPush → executePush() via SyncDelegate
                     ↓
         SyncDelegate.pushPendingToServer() → Repository reads unsynced rows → API call
```

---

## Key Files

| File | Role |
|---|---|
| `data/sync/.../CentralSyncService.kt` | Singleton coordinator — holds delegates map, processes events, serializes pushes per entity via Mutex |
| `data/sync/.../SyncDelegate.kt` | Interface each feature implements |
| `data/sync/.../SyncEntity.kt` | Enum of all syncable entity types |
| `data/sync/.../SyncEvent.kt` | Events: TriggerPush, TriggerPull, TriggerFullSync, BackendEventReceived |
| `data/sync/.../SyncStatus.kt` | Sealed class: Idle, PendingPush, PendingPull, Syncing, Success, Failed |
| `data/sync/.../db/SyncPersistStatus.kt` | Enum for DB storage: IDLE, PENDING_PUSH, PENDING_PULL, SYNCING, FAILED |
| `data/sync/.../db/SyncStateEntity.kt` | Room entity — `entityName: SyncEntity`, `statusName: SyncPersistStatus` |
| `data/sync/.../db/SyncStateConverters.kt` | Room TypeConverters for enum ↔ String (keeps TEXT storage, no migration needed) |
| `feature/{name}/sync/{Name}SyncDelegate.kt` | Feature-level delegate: pull, push, handle backend event |

---

## Race Condition Fix (Mutex)

`CentralSyncService` has per-entity `Mutex` instances for push and pull:
```kotlin
private val pushMutexes = SyncEntity.entries.associateWith { Mutex() }
private val pullMutexes = SyncEntity.entries.associateWith { Mutex() }
```

`executePush` and `executePull` use `mutex.withLock { ... }` instead of the old
`if (status is Syncing) return`. This serializes concurrent pushes — the second push waits
for the first to complete, then runs and picks up any records written during the first sync.

---

## SyncDelegate Registration Pattern

```kotlin
@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.MY_ENTITY)
class MyEntitySyncDelegate(
    private val repository: MyEntityRepository,
) : SyncDelegate {
    override val entity = SyncEntity.MY_ENTITY

    override suspend fun pullFromServer(): SyncResult =
        repository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        repository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()
}
```

---

## ViewModel Write Pattern (REQUIRED for every write)

```kotlin
// After any successful local write:
syncService.markPendingPush(SyncEntity.MY_ENTITY)
```

Examples in codebase:
- `ProductFormViewModel` — `syncService.markPendingPush(SyncEntity.PRODUCT)` after create/update
- `CustomerFormViewModel` — `syncService.markPendingPush(SyncEntity.CUSTOMER)` after create/update
- `CustomerGroupFormViewModel` — `syncService.markPendingPush(SyncEntity.CUSTOMER_GROUP)` after save
- `CustomerTypeFormViewModel` — `syncService.markPendingPush(SyncEntity.CUSTOMER_TYPE)` after save
- `UnitFormViewModel` — `syncService.markPendingPush(SyncEntity.UNIT)` after save

**Never call `repository.syncXxx()` directly from a ViewModel.** Use `syncService.emit(SyncEvent.TriggerFullSync(entity))` for manual refreshes.

---

## List ViewModel Pattern (Manual Refresh + Spinner)

```kotlin
@Inject class MyListViewModel(
    private val repository: MyRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {
    init {
        observeData()
        // Drive isRefreshing from sync state, not from coroutine lifecycle
        syncService.observeEntity(SyncEntity.MY_ENTITY)
            .onEach { state -> _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        // Initial pull on screen open
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.MY_ENTITY))
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.MY_ENTITY))
    }
}
```

**Do NOT** call `repository.sync()` or `repository.fullSync()` from `init {}`. UI-driven sync is removed.

---

## Tally Sync → Server Push Pattern

`TallySyncScheduler` calls `centralSyncService.markPendingPush(entity)` after writing Tally data:
```kotlin
if (result.customerGroupsSynced > 0) centralSyncService.markPendingPush(SyncEntity.CUSTOMER_GROUP)
if (result.customersSynced > 0)      centralSyncService.markPendingPush(SyncEntity.CUSTOMER)
if (result.productsSynced > 0)       centralSyncService.markPendingPush(SyncEntity.PRODUCT)
```

---

## Repository Push Pattern

The `pushPendingToServer()` method in repositories:
1. Reads all rows with `synced = false` from Room
2. For deletions (active = false): calls DELETE API, then hard-deletes locally
3. For creates/updates: tries UPDATE first, falls back to CREATE on 404
4. On success: marks the row `synced = true` in Room
5. Never generates UIDs — expects them pre-set (UID generation is ViewModel responsibility)

---

## Adding a New Syncable Entity — Checklist

- [ ] Add entry to `SyncEntity` enum
- [ ] Add `observeUnsyncedCount(): Flow<Int>` to DAO (optional but useful)
- [ ] Repository has `pushPendingToServer(): Result<Int>` and `pullFromServer(): Result<Int>`
- [ ] Create `{Name}SyncDelegate` with `@SyncEntityKey(SyncEntity.X)` in feature's `sync/` package
- [ ] All ViewModel write operations call `syncService.markPendingPush(SyncEntity.X)` on success
- [ ] List ViewModel observes `syncService.observeEntity(X)` for `isRefreshing`; manual refresh calls `emit(TriggerFullSync(X))`
- [ ] If Tally writes this entity, add `markPendingPush(X)` in `TallySyncScheduler`

---

## Reactive Observation of SyncStateEntity

`CentralSyncService.start()` observes `SyncStateDao.observeAll()` filtered to PENDING states:

```kotlin
syncDao.observeAll()
    .map { rows -> rows.filter { it.statusName == PENDING_PUSH || it.statusName == PENDING_PULL } }
    .distinctUntilChanged()
    .collect { pendingRows ->
        pendingRows.forEach { row ->
            when (row.statusName) {
                PENDING_PUSH -> emit(TriggerPush(row.entityName))
                PENDING_PULL -> emit(TriggerPull(row.entityName))
            }
        }
    }
```

This means:
- **App restart**: persisted PENDING_PUSH states are immediately observed and re-triggered (no need for WebSocket to reconnect first)
- **Any DB write that sets PENDING_PUSH** — whether via `markPendingPush` or anything else — automatically fires the sync
- **`.distinctUntilChanged()` is required** — `executePush`/`executePull` write PENDING_PUSH/PENDING_PULL to the DB inside the mutex (for process-death safety). Without deduplication, each write re-triggers the observer, queuing another execution in an infinite loop. Failed syncs retry via `processPendingStates()` on reconnect — not via the reactive observer.

---

## Process Death Safety

Before starting each push/pull, `CentralSyncService` persists `PENDING_PUSH` / `PENDING_PULL` to `SyncStateDatabase`. On restart, the DAO observation in `start()` immediately picks up any persisted pending states and re-triggers the sync.

---

## SyncStatusName Enum (DB storage)

`SyncPersistStatus` enum (stored as TEXT via Room TypeConverter, same SQL as before):
- `IDLE` — nothing pending
- `PENDING_PUSH` — local writes need to reach server
- `PENDING_PULL` — server event received, need fresh pull
- `SYNCING` — never persisted directly
- `FAILED` — last attempt failed, will retry on reconnect