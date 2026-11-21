# Centralized Sync Architecture

**Module:** `data/sync`  
**Package:** `com.ampairs.sync`

---

## Design Rationale

Previously, each feature module wired its own real-time event listener inside `EventConnectionManager.setupRepositoryListeners()`. This caused:
- No unified view of what was syncing or pending
- ViewModels calling repository sync methods directly (tight coupling)
- No persistence of sync state — pending work was lost on process death
- No way to observe "is customer syncing?" from the UI

The `data/sync` module introduces a single service as the source of truth for all sync state, driven by persistent state rather than periodic polling.

---

## Core Principle: State-Driven Sync

There is **no periodic background sync timer**. Instead:

| Trigger | Resulting state change |
|---|---|
| ViewModel writes locally (`synced=false`) | Entity → `PENDING_PUSH` |
| Backend WebSocket event received | Entity → `PENDING_PULL` |
| App launched with pending states in DB | Service re-queues events |
| WebSocket reconnects | All PENDING_* entities re-processed |

---

## Data Flow

```
── Backend writes (another device) ──────────────────────────────────────────────
RabbitMQ → STOMP/WebSocket → EventManager.events
                                    │
                         EventConnectionManager (collects)
                                    │
                         syncService.onBackendEvent(entityType, entityId, eventType)
                                    │
                         SyncStateDao.upsert(status = PENDING_PULL)   ← persisted
                                    │
                         eventChannel ← SyncEvent.BackendEventReceived
                                    │
                         processEvents() coroutine
                                    │
                         delegate.handleBackendEvent(entityId, eventType)
                                    │
                         Repository → Room DB → Flow notifies UI
                                    │
                         SyncStateDao.upsert(status = IDLE, lastSyncedAt)

── ViewModel local write ────────────────────────────────────────────────────────
ViewModel → repository.createX() / updateX()  (local-first, synced=false)
          → syncService.markPendingPush(SyncEntity.CUSTOMER)
                    │
          SyncStateDao.upsert(status = PENDING_PUSH)   ← persisted
                    │
          eventChannel ← SyncEvent.TriggerPush(CUSTOMER)
                    │
          delegate.pushPendingToServer()
                    │
          API call → Room update (synced=true)
                    │
          SyncStateDao.upsert(status = IDLE)

── App start / reconnect ─────────────────────────────────────────────────────
CentralSyncService.start(workspaceSlug, eventManager)
    │
    ├── SyncStateDao.getAll() → restore in-memory EntitySyncState map
    ├── SyncStateDao.getPending() → re-queue PENDING_* and FAILED
    └── eventManager.connectionState
            .filter { it is Connected }
            .collect { processPendingStates() }    ← fires on every reconnect
```

---

## State Machine (per entity)

```
         ┌──────────────────────────────────────────────────┐
         │                                                  │
    IDLE ──local write───→ PENDING_PUSH ──service picks up──→ SYNCING
    IDLE ──backend event──→ PENDING_PULL ──service picks up──→ SYNCING
         │                                                  │
         ←──────── success (stores lastSyncedAt) ───────────┘
         │                                                  │
    FAILED ←─────── transient failure (retry on reconnect) ─┘

Persisted states: IDLE, PENDING_PUSH, PENDING_PULL, FAILED
SYNCING is never persisted — a crash during sync reverts to PENDING_* for retry.
```

---

## Key Files

| File | Role |
|---|---|
| `data/sync/src/commonMain/.../CentralSyncService.kt` | Orchestrator — event loop, state management, persistence |
| `data/sync/src/commonMain/.../SyncDelegate.kt` | Interface every feature module implements |
| `data/database/src/commonMain/.../sync/SyncEntity.kt` | Enum registry of all syncable entities — `SyncEntity(entityType, moduleCode)` |
| `data/sync/src/commonMain/.../SyncEvent.kt` | Commands: TriggerPull, TriggerPush, BackendEventReceived, etc. |
| `data/sync/src/commonMain/.../SyncStatus.kt` | Status sealed class + persistence name helpers |
| `data/database/src/commonMain/.../sync/db/SyncStateEntity.kt` | `entity_sync_state` table — lives **inside `AmpairsWorkspaceDatabase`** (no separate sync DB) |
| `data/database/src/commonMain/.../sync/db/SyncStateDao.kt` | Persistence queries for sync state rows; exposed as `WorkspaceGraph.syncStateDao` and passed to `CentralSyncService.start(...)` |
| `data/sync/src/commonMain/.../di/SyncModule.kt` | `@Multibinds` declaration for `Map<SyncEntity, SyncDelegate>` |
| `feature/workspace/.../EventConnectionManager.kt` | Collects EventManager events → routes to `syncService.onBackendEvent()` |
| `feature/customer/.../sync/CustomerSyncDelegate.kt` | Reference implementation — template for all other entities |

---

## SyncDelegate Contract

Every offline-capable feature module implements `SyncDelegate` and registers via Metro multibinding:

```kotlin
@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.PRODUCT)   // ← your entity here
class ProductSyncDelegate(
    private val productRepository: ProductRepository,
) : SyncDelegate {

    override val entity = SyncEntity.PRODUCT

    // Called on TriggerPull or PENDING_PULL
    override suspend fun pullFromServer(): SyncResult =
        productRepository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    // Called on TriggerPush or PENDING_PUSH
    override suspend fun pushPendingToServer(): SyncResult =
        productRepository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    // Called when a backend WebSocket event arrives for this entity type
    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { productRepository.handleExternalEvent(entityId, eventType) }.fold(
            onSuccess = { SyncResult.Success(1) },
            onFailure = { SyncResult.Failure(it) },
        )

    // observePendingCount() is NOT overridden — inherits default flowOf(0).
    // PendingPush is triggered by calling syncService.markPendingPush(SyncEntity.PRODUCT)
    // from the ViewModel after any local write.
}
```

No changes needed in any DI module file — Metro picks up the `@ContributesIntoMap` automatically.

---

## ViewModel Pattern

```kotlin
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class CustomerListViewModel(
    private val syncService: CentralSyncService,
    private val customerDao: CustomerDao,
    private val workspacePrefs: AppPreferencesDataStore,
) : ViewModel() {

    // Observe sync state for this entity
    val syncState: StateFlow<EntitySyncState?> =
        syncService.observeEntity(SyncEntity.CUSTOMER)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Observe data from local DB (source of truth for UI)
    val customers: StateFlow<List<CustomerListItem>> =
        customerDao.getAllCustomers()
            .map { it.map(CustomerEntity::toListItem) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Trigger pull (no direct API call)
    fun onRefresh() {
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.CUSTOMER))
    }
}

// After a local write in a form ViewModel:
fun onSave(customer: Customer) {
    viewModelScope.launch {
        val uid = UidGenerator.generateUid(CustomerConstants.CUSTOMER_PREFIX)
        customerRepository.createCustomer(customer.copy(uid = uid))   // local-first
        syncService.markPendingPush(SyncEntity.CUSTOMER)              // async push
    }
}
```

---

## Workspace Lifecycle

`CentralSyncService.start()` and `.stop()` must be called at workspace selection and exit:

```kotlin
// WorkspaceListViewModel.selectWorkspace():
eventConnectionManager.connectToWorkspace(workspaceId, userId, deviceId, scope)
centralSyncService.start(workspace.slug)

// On logout / workspace switch:
centralSyncService.stop()
eventConnectionManager.disconnect()
```

`EventConnectionManager.connectToWorkspace()` must be called first so the WebSocket is established. The `EventConnectionManager` internally subscribes to `ConnectionState.Connected` and calls `syncService.onConnectionRestored()` on every reconnect — `CentralSyncService` itself has no direct dependency on `IEventManager`.

---

## Adding a New Syncable Entity

The authoritative checklist is in **`/offline-sync` → "Adding a New Syncable Entity — Checklist"**;
the database steps are in **CLAUDE.md → "Adding tables for a feature"**. In short:

1. **Entity + DAO in `:data:database`** (feature package), registered in `AmpairsWorkspaceDatabase`
   with a version bump + migration. The entity carries `synced` (+ `active` for soft delete).
2. **`SyncEntity` entry with its `moduleCode`** in `data/database/.../sync/SyncEntity.kt`:
   ```kotlin
   PAYMENT("payment", "payment-collection"),
   ```
3. **Repository is local-only** — injects the `Dao` + `SyncStateDao` (never the `Api`); every write
   saves with `synced = false` and calls `syncStateDao.markPendingPush(SyncEntity.PAYMENT, now)`.
4. **`PaymentSyncDelegate` owns all server traffic** — injects the `Api` + `Dao`:
   ```kotlin
   @Inject @ContributesIntoMap(WorkspaceScope::class) @SyncEntityKey(SyncEntity.PAYMENT)
   class PaymentSyncDelegate(
       private val api: PaymentApi,
       private val dao: PaymentDao,
       private val syncStateDao: SyncStateDao,   // only if the pull is timestamp-incremental
   ) : SyncDelegate {
       override val entity = SyncEntity.PAYMENT
       override suspend fun pullFromServer(): SyncResult = /* batched pull; hard-delete server-DELETED rows */
       override suspend fun pushPendingToServer(): SyncResult = /* bulk push of synced = 0 rows */
       override suspend fun handleBackendEvent(entityId: String, eventType: String) = pullFromServer()
   }
   ```
5. **Wire the EventType mapping** — `WorkspaceEvent.entityType` must match `SyncEntity.PAYMENT.entityType`.

No changes to `CentralSyncService`, `EventConnectionManager`, or any DI module file are needed.

---

## Persistence Model

**Database:** the `entity_sync_state` table inside the workspace's `AmpairsWorkspaceDatabase`  
**Table:** `entity_sync_state`

| Column | Type | Notes |
|---|---|---|
| `entityName` | TEXT PK | `SyncEntity.name` — e.g. `"CUSTOMER"` |
| `statusName` | TEXT | `"IDLE"`, `"PENDING_PUSH"`, `"PENDING_PULL"`, `"FAILED"` |
| `lastSyncedAt` | INTEGER? | Epoch milliseconds of last successful sync |
| `lastSyncedAtIso` | TEXT? | Server `updatedAt` checkpoint for incremental pulls (preserved by `markPendingPush`) |
| `pendingCount` | INTEGER | Count of unsynced local rows (for `PENDING_PUSH`) |
| `errorMessage` | TEXT? | Last error message (for `FAILED`) |
| `updatedAt` | INTEGER | When this row was last written |

`SYNCING` is never persisted — if the process dies while syncing, the status on restart will still be `PENDING_*`, ensuring the sync retries automatically.

---

## What Is NOT in This Module

- No periodic WorkManager polling — state drives sync, not timers
- No per-repository `setupEventListener()` calls — all events flow through `EventConnectionManager` → `CentralSyncService`
- No API calls in `CentralSyncService` itself — it delegates entirely to `SyncDelegate` implementations
- No UI code — this is pure infrastructure
