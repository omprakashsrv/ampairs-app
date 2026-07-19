# /offline-sync — Offline-First Sync Architecture

This skill documents the complete offline-first, push/pull sync architecture used in the Ampairs KMP app.
Apply it whenever adding a new entity, adding a new write operation, or debugging why data isn't reaching the server.

---

## Core Principle

**The repository is local-only. It never talks to the network.** Every local write goes to Room
(`synced = false`) and then flags the entity `PENDING_PUSH` via `SyncStateDao.markPendingPush(...)`.
`CentralSyncService` observes that flag and runs the bulk push through the feature's `SyncDelegate`,
which is the **single place that holds the API**.

```
UI Write → ViewModel → Repository
                          ├─ Room write (synced = false)
                          └─ syncStateDao.markPendingPush(SyncEntity.X)   ← no API here
                                       ↓
        CentralSyncService reactive observer sees PENDING_PUSH
                                       ↓
        CentralSyncService emits TriggerPush → executePush() (per-entity Mutex)
                                       ↓
        SyncDelegate.pushPendingToServer()  ← injects the API + DAO
            reads unsynced rows from the DAO → bulk API call → mark rows synced=true
```

**Layering rule (the important one):**
- **Repository** = local data access only: Room reads/writes + `markPendingPush`. No `Api` injected
  for the entity-update flow.
- **SyncDelegate** = the entire server conversation for that entity: bulk push, batched pull
  (permanently deleting server-`DELETED` rows), and backend-event refresh. Injects `Api` + `Dao`.
- **CentralSyncService** = coordinator. Nothing else triggers the network.

**Allowed exception:** a repository may still inject the `Api` for a *non-sync, UI-invoked* feature
that has no central-sync equivalent — e.g. customer-group/type "import from master /
available-for-import", or the file repo's entity-scoped `pullFromServer(type, uid)` /
`setPrimaryFile`. The entity create/update/delete path must never call the API.


---

## Canonical Sync API Contract (every standard entity)

All standard syncable entities share ONE unified REST contract. Pull and push use the same URL.

```
PULL   GET  {domain}Url("v1/{resource}/sync")
            ?last_sync={ISO8601}&page={Int}&size={Int}&sort_by=updatedAt&sort_dir=ASC
       → Response<PageResponse<T>>   (content + hasNext; INCLUDES soft-deleted rows)

PUSH   POST {domain}Url("v1/{resource}/sync")        (same URL as pull)
       body: List<T>   (active upserts AND soft-deleted rows; client UID-keyed)
       → Response<List<T>>           (delegate batches at 100)

DELETE in-band: the push body carries soft-deleted rows (active=false / status=DELETED).
       There is NO per-row DELETE call in the sync path. On pull, rows the server reports
       DELETED/inactive are permanently hard-deleted locally; local unsynced edits win.
```

- **Query params are snake_case**: `last_sync`, `page`, `size`, `sort_by`, `sort_dir`. `last_sync` is sent only when non-blank.
- The `/sync` GET **must return soft-deleted rows** so deletions propagate to every device.
- Backend mirror: `GET/POST /{module}/v1/{resource}/sync` → `ApiResponse<PageResponse<T>>` / `ApiResponse<List<T>>`. The legacy non-`/sync` list+bulk endpoints were removed.
- **On the contract:** customer, customer_group, customer_type, product, product_catalog (groups/categories/brands/sub-categories, paginated), unit, store/settings, order, invoice.
- **Aggregate-grained on the contract:** **form** — see the Form note below.
- **Off-pattern by design** (do NOT force onto this contract): **Tax** (subscribe/unsubscribe), **File** (entity-scoped multipart, UI-invoked).

> **Form is an aggregate-grained `/sync` resource (spec 011).** A single feed
> `GET/POST v1/config/schema/sync` carries one `FormSchema` aggregate per entityType (uid =
> entityType; each bundles its ordered sections + fields). `FormSyncDelegate` runs it under one
> `SyncEntity.FORM` checkpoint:
> - **Pull replaces** each local aggregate — local members absent from the server copy are deleted
>   (delete-by-absence); local-unsynced aggregate wins until pushed.
> - **Push** sends the dirty aggregate(s) with `base_version`; on a version-conflict response the
>   delegate re-pulls, re-applies local edits, and retries (aggregate-level last-write-wins).
> - Checkpoint advances to `max(updatedAt)` across aggregates.
> Deletions round-trip via absence (no soft-delete column needed — the prior two-feed
> field-configs/attribute-definitions model and its no-delete gap are gone).

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

The delegate injects the **API + DAO** (not the repository) and owns the full server conversation.
It is contributed to `WorkspaceScope::class` (every workspace-aware delegate lives in the workspace
child graph — see `/metro-di`). `SyncStateDao` is only needed when the pull tracks an incremental
`lastSyncedAtIso` checkpoint.

```kotlin
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.MY_ENTITY)
class MyEntitySyncDelegate(
    private val api: MyEntityApi,
    private val dao: MyEntityDao,
    private val syncStateDao: SyncStateDao,   // only if the pull is timestamp-incremental
) : SyncDelegate {
    override val entity = SyncEntity.MY_ENTITY

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    // Bulk push: read unsynced rows from the DAO, send in batches of 100, mark synced=true.
    private suspend fun pushPending(): Result<Int> { /* ... */ }

    // Batched pull: page through the server; permanently delete rows the server marks DELETED;
    // local unsynced edits win; upsert the rest.
    private suspend fun pull(): Result<Int> { /* ... */ }
}
```

**Reference delegates** (copy these): `CustomerSyncDelegate`, `UnitSyncDelegate`,
`CustomerGroupSyncDelegate`, `ProductSyncDelegate`, `FileSyncDelegate`.

---

## Repository Write Pattern (the pending-push flag lives in the repository)

The repository marks the entity pending on every successful local write — this is what makes the
push automatic. The repository injects `SyncStateDao`, never the `Api`.

```kotlin
@Inject
class MyEntityRepository(
    private val dao: MyEntityDao,
    private val syncStateDao: SyncStateDao,
) {
    suspend fun create(entity: MyEntity): Result<MyEntity> {
        require(entity.uid.isNotBlank())            // UID is set by the ViewModel
        dao.insert(entity.toEntity().copy(synced = false))
        markPending()
        return Result.success(entity)
    }

    suspend fun delete(id: String): Result<Unit> {
        val existing = dao.getById(id) ?: return Result.success(Unit)
        dao.insert(existing.copy(active = false, synced = false))   // soft-delete + unsynced
        markPending()
        return Result.success(Unit)
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.MY_ENTITY, Clock.System.now().toEpochMilliseconds())
}
```

`SyncStateDao.markPendingPush(entity, now)` is an `INSERT … ON CONFLICT DO UPDATE` upsert that sets
`PENDING_PUSH` **without wiping `lastSyncedAtIso`** (the pull checkpoint).

> Delete must set `synced = false` (not only `active = false`) — the push reads `synced = 0`, so a
> row that's merely inactive would never be pushed. Several DAOs' `deleteXxx` only set `active = 0`;
> fetch + `copy(active = false, synced = false)` in the repo instead.

**ViewModels** no longer need to call `markPendingPush` (the repo does it). A redundant
`syncService.markPendingPush(...)` from a VM is harmless but unnecessary. **Never call
`repository.syncXxx()` from a ViewModel** — use `syncService.emit(SyncEvent.TriggerFullSync(entity))`
for manual refreshes.

**Reference repositories** (copy these): `CustomerRepository`, `UnitRepository`,
`CustomerGroupRepository`, `ProductRepository`.

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

## Delegate Push Pattern (the API lives here, not the repository)

The `pushPending()` method **inside the delegate**:
1. Reads all rows with `synced = false` from the DAO (active upserts AND soft-deleted rows)
2. Bulk-upserts the whole set to `POST v1/{resource}/sync` in batches of 100 — deletions ride along **in-band** as soft-deleted rows (`active = false` / `status = DELETED`). No per-row DELETE call.
3. On success: marks active rows `synced = true`; **hard-deletes the `active = false` rows locally** (they're now confirmed on the server)
5. Never generates UIDs — expects them pre-set (UID generation is ViewModel responsibility)
6. Returns `Result.failure()` when there were items to push but none succeeded (see Rule 2 below)

## Delegate Pull Pattern

The `pull()` method **inside the delegate** pages through the server and reconciles each row:
- Local **unsynced** edits win → skip the server row
- Server row marked `DELETED` / `active = false` → **permanently hard-delete** the local row
- Otherwise → upsert as `synced = true`
- Advance the `lastSyncedAtIso` checkpoint (timestamp-incremental pulls only)

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

> **Exception — entities that other entities declare as a `pushDependencies`:** partial success is
> **not** OK there. `CentralSyncService.executePush` only defers a dependent (e.g. INVOICE) when the
> dependency's (ORDER's) push signals `SyncResult.Failure` — it has no visibility into "3 of 4 rows
> synced". If ORDER's `pushPending()` reports `Result.success` after a partial multi-batch failure
> (`synced > 0 && failed > 0`), CentralSyncService clears ORDER's pending state and lets INVOICE push
> in the *same cycle* — including any invoice referencing one of the still-unsynced orders, which the
> backend rejects with a `fk_invoice_order_ref` (409) violation. Live bug: `OrderSyncDelegate`/
> `InvoiceSyncDelegate` used `if (synced == 0 && failed > 0)`; fixed to `if (failed > 0)` so ANY failed
> batch reports failure — the successfully-synced rows are still durably marked `synced=1` in Room
> either way (that part doesn't roll back), this only changes the *signal* CentralSyncService acts on.
> Rule of thumb: leaf entities (no dependents) → total-failure-only reporting is fine; entities with
> dependents → any-failure reporting is required.

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
- [ ] **Repository (local-only):** inject `Dao` + `SyncStateDao` — **not** the `Api`. `create/update/delete`
      write to Room (`synced = false`) and call `syncStateDao.markPendingPush(SyncEntity.X, now)`
- [ ] Delete soft-deletes (`active = false, synced = false`) so the push picks it up
- [ ] **Delegate (`{Name}SyncDelegate`):** inject `Api` + `Dao` (+ `SyncStateDao` if the pull is
      timestamp-incremental). Implement bulk `pushPending()`, batched `pull()` (delete server-`DELETED`
      rows), and `handleBackendEvent()`. Annotate `@ContributesIntoMap(WorkspaceScope::class)` +
      `@SyncEntityKey(SyncEntity.X)`. Set `dependsOn` / `pushDependencies` for FK ordering
- [ ] List ViewModel observes `syncService.observeEntity(X)` for `isRefreshing`; opens with
      `emit(TriggerPull(X))`; manual refresh calls `emit(TriggerFullSync(X))`. Do **not** call repo sync
- [ ] If Tally writes this entity, add `markPendingPush(X)` in `TallySyncScheduler`

### When the repository may still hold the `Api`
Only for a **non-sync, UI-invoked** feature with no central-sync path: import-from-master /
available-for-import (customer group/type), or the file repo's entity-scoped
`pullFromServer(type, uid)` / `setPrimaryFile`. The create/update/delete path stays API-free.

### Entities intentionally NOT on this pattern
- **Tax** — writes are online subscribe/unsubscribe (the server mints the workspace tax code); a
  3-repo cluster (code/rule/component) whose sync is aggregated in `MyTaxCodesViewModel`.
- **Ecom** — separate storefront module (catalog/order pull-only; address has its own CRUD).

> **Form is an aggregate-grained `/sync` resource** (spec 011): one feed
> `GET/POST v1/config/schema/sync` carrying one `FormSchema` per entityType (uid = entityType;
> sections + fields bundled). Pull replaces the local aggregate (delete-by-absence); push carries
> `base_version` with aggregate-level last-write-wins + re-pull/retry on version conflict. Deletions
> round-trip via absence. See the Form note under the Canonical Sync API Contract section.

> **Order / Invoice are now on the canonical `/sync` contract** (`GET/POST v1/orders/sync`,
> `v1/invoices/sync`). Note: the app still has a separate non-sync list pull
> (`getOrderResource` / `getInvoiceResource` via the legacy `GET v1/{orders|invoices}` with
> `last_updated`) used by the list ViewModels — a follow-up should converge those onto the
> delegate-only pull. The backend `GET /invoice/v1/invoices` (last_updated) was kept alive for
> that reason; the order equivalent never existed on the backend.

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