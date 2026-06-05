---
name: Critical Code Patterns
description: Project-specific conventions that cause bugs when violated — Koin factory/single, UID generation, Response null-check, Logger API
type: feedback
originSessionId: 35585732-55ed-4e7b-8cf2-fb305112b179
---
## Rule 1: factory vs single in Koin for workspace-aware components

All workspace-aware DI must use `factory {}` not `single {}` for the full dependency chain:
`Database → DAOs → Repositories → Stores → ViewModels (viewModel/viewModelOf)`

**Why:** `single` caches the old database reference after a workspace switch, causing stale data from the previous workspace to appear in the new workspace. This was a major bug found in production.

**How to apply:** When adding a new feature module, verify every layer uses `factory`. Exceptions: `AuthRoomDatabase` and `WorkspaceRoomDatabase` stay as `single` because they exist before workspace selection and store the workspace list itself.

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
