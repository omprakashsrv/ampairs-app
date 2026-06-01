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

## CRITICAL: Failure Propagation Rules

These bugs have recurred across multiple entities. Apply every rule mechanically when writing or reviewing any repository push method.

### Rule 1 — `saveLocally()` must fail fast if file/local write fails

**Wrong** — silently continues with null localPath, inserts a PENDING DB row that can never upload:
```kotlin
val localPath = try {
    fileManager.saveImageToCache(uid, data, fileName)
} catch (e: Exception) {
    null  // ❌ WRONG — DB record inserted below with null path, retried forever
}
dao.insert(entity.copy(localPath = localPath))
return Result.success(entity)
```

**Correct** — fail fast before inserting the DB record:
```kotlin
val localPath = try {
    fileManager.saveImageToCache(uid, data, fileName)
} catch (e: Exception) {
    return Result.failure(e)  // ✅ No DB record, ViewModel shows error to user
}
dao.insert(entity.copy(localPath = localPath))
return Result.success(entity)
```

**Why:** A PENDING record with `localPath = null` will be retried on every push cycle and fail every time (no file to upload), burning network and polluting logs forever.

---

### Rule 2 — `pushPendingToServer()` must return `Result.failure()` when all items fail

**Wrong** — returns success with count=0 even when every upload failed. CentralSyncService logs "sync success", ViewModel shows green, user has no idea uploads failed:
```kotlin
// After looping through unsynced images...
Result.success(syncedCount)  // ❌ syncedCount = 0 but items are FAILED — caller sees false green
```

**Correct** — return failure when there were items to push but none succeeded:
```kotlin
if (syncedCount == 0 && entitiesToUpdate.any { it.uploadStatus == STATUS_FAILED }) {
    Result.failure(Exception("Failed to upload pending items — will retry on reconnect"))
} else {
    Result.success(syncedCount)
}
```

**Why:** `SyncResult.Success(0)` tells CentralSyncService everything is fine. The delegate's `.fold(onSuccess = SyncResult.Success, onFailure = SyncResult.Failure)` chain only propagates failure if the repository returns `Result.failure()`. Partial success (some uploaded, some failed) is OK to report as success — the FAILED rows will retry next push cycle.

---

### Rule 3 — Upload timeout: Ktor's blanket timeout fires before `withTimeout()`

The shared `httpClient()` sets `requestTimeoutMillis = 30_000`. A `withTimeout(60_000L)` wrapper in the repository is irrelevant — Ktor cancels the request at 30s first.

For large file uploads, pass a per-request override via `postMultiPart(..., requestTimeoutMillis = 120_000L)`. The `withTimeout()` in the repository should be set higher than the Ktor timeout (e.g. `130_000L`) to serve as a hard kill-switch only:

```kotlin
// API layer — override the 30s blanket timeout for this call only
postMultiPart(client, url, parts, requestTimeoutMillis = 120_000L)

// Repository layer — safety net above the API timeout
withTimeout(130_000L) {
    api.uploadMultipart(...)
}
```

---

### Checklist — add to every new push repository method

- [ ] `saveLocally()` returns `Result.failure(e)` immediately if local file/cache write fails — no DB insert
- [ ] `pushPendingToServer()` returns `Result.failure()` when `syncedCount == 0` AND any entity ended up FAILED
- [ ] Upload API calls use `postMultiPart(..., requestTimeoutMillis = 120_000L)` and `withTimeout(130_000L)` in the repo
- [ ] Items with no local file during push are marked FAILED (not skipped silently) — these indicate a bug in `saveLocally()`

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