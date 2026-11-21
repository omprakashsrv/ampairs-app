---
name: Critical Code Patterns
description: Project-specific conventions that cause bugs when violated — Metro WorkspaceScope, UID generation, Response null-check, Logger API
type: feedback
originSessionId: 35585732-55ed-4e7b-8cf2-fb305112b179
---
## Rule 1: One workspace database, WorkspaceScope (Metro DI)

All workspace data lives in the consolidated `AmpairsWorkspaceDatabase` (`:data:database`), provided `@SingleIn(WorkspaceScope::class)` by `DatabaseModule.*.kt` and registered with `WorkspaceClosableRegistry`. Features never declare or provide their own database. DAOs (`@Provides` in `WorkspaceDatabaseDaoModule`) and Repositories are unscoped (new instance per injection site — safe across workspace switches).

`@SingleIn(WorkspaceScope::class) AmpairsWorkspaceDatabase → unscoped DAO @Provides → unscoped @Inject Repositories → @ContributesIntoMap(WorkspaceScope::class) ViewModels`

**Why:** Each workspace gets its own Metro child graph (`WorkspaceScope`). When a workspace switch occurs, the old child graph is torn down (all registered DB closables are closed) and a new one is created. Unscoped DAOs/Repos therefore always receive the current workspace's DB. This replaced the old Koin `factory {}` pattern, which was error-prone and had stale-data bugs.

**How to apply:** For a new feature's tables, put the entity + DAO in `:data:database` (feature package), register them in `AmpairsWorkspaceDatabase`, bump its version and add a migration built from the exported schema JSON (CLAUDE.md → *Adding tables for a feature*). Data that exists before/across workspaces (auth, workspace registry, model catalog) goes in the AppScope `AmpairsAppDatabase` instead.

---

## Rule 2: UID generation belongs in ViewModel, never in Repository

Always generate UIDs in the ViewModel layer before calling the repository:
```kotlin
val uid = UidGenerator.generateUid(Constants.UID_PREFIX)
```
Import: `com.ampairs.common.id_generator.UidGenerator` (not `.util.UidGenerator`)

**Why:** The repository must receive a fully-formed entity. If the repository generates UIDs as a fallback, the UID used locally and the UID tracked in the UI diverge, breaking the create → sync → update lifecycle.

**How to apply:** Always assert `require(entity.uid.isNotBlank())` at the top of repository create methods.

---

## Rule 3: Response<T>.data is nullable — no .success property

```kotlin
// CORRECT
if (response.data != null && response.error == null) { ... }

// WRONG — will not compile
if (response.success) { ... }
```

Import: `com.ampairs.common.model.Response` (not `.core.domain.dto.ApiResponse`)

**Why:** The `Response<T>` model from common wraps nullable data. There is no `.success` computed property.

---

## Rule 4: Logger signature is 3 params with short method names

```kotlin
// CORRECT
CustomerLogger.w("TagName", "message", exception)
CustomerLogger.e("TagName", "message", exception)

// WRONG — method names don't exist
CustomerLogger.warn(...)
CustomerLogger.error(...)
```

**How to apply:** Use `w`, `e`, `i`, `d` for warn/error/info/debug.

---

## Rule 5: API URL builder pattern

```kotlin
ApiUrlBuilder.customerUrl("v1/groups")    // customer feature
ApiUrlBuilder.productUrl("v1/items")      // product feature
// NOT hardcoded string URLs
```

---

## Rule 6: Form state uses String IDs, not object references

Store backend entity IDs as `String` in form state, with a separate display name string. Never store the full domain object reference in form state.

**Reference:** `CustomerFormState` in `CustomerFormViewModel.kt`

---

## Rule 7: Repositories are local-only — the API lives in the SyncDelegate

The entity create/update/delete flow must **not** inject or call the feature `Api`. The repository
writes to Room (`synced = false`) and flags the entity for push via
`syncStateDao.markPendingPush(SyncEntity.X, Clock.System.now().toEpochMilliseconds())`.
`CentralSyncService`'s reactive observer turns that flag into an automatic bulk push, executed by the
`{Name}SyncDelegate` — the **single place** that injects the `Api` (+ `Dao`) and owns bulk push,
batched pull (permanently deleting server-`DELETED` rows), and backend-event refresh.

```kotlin
// ✅ Repository — local-only
@Inject class CustomerRepository(dao: CustomerDao, syncStateDao: SyncStateDao) {
    suspend fun createCustomer(c: Customer): Result<Customer> {
        dao.insertCustomer(c.toEntity().copy(synced = false))
        syncStateDao.markPendingPush(SyncEntity.CUSTOMER, Clock.System.now().toEpochMilliseconds())
        return Result.success(c)
    }
}

// ❌ Repository calling the API directly in the write path — do not do this
val server = customerApi.createCustomer(c)   // belongs in CustomerSyncDelegate
```

**Why:** keeping the API out of the repository makes every write offline-first by construction
(one path: write + flag), yields a single bulk push/pull per entity, and lets multi-device
WebSocket events drive pulls. **Delete must set `synced = false` (not just `active = false`)** or the
push (which reads `synced = 0`) will never send it.

**Allowed exception:** the repo may keep the `Api` only for a *non-sync, UI-invoked* feature with no
central-sync path — import-from-master / available-for-import (customer group/type), or the file
repo's entity-scoped `pullFromServer(type, uid)` / `setPrimaryFile`.

**Reference:** `CustomerRepository` + `CustomerSyncDelegate`. Full guide: `/offline-sync`.
