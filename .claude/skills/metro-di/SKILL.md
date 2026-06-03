# /metro-di — Metro DI Complete Guide

> **Framework:** `dev.zacsweers.metro` + MetroX ViewModel extensions.
> The project has **fully migrated from Koin**. Do not write Koin code.
> Invoked via `/metro-di`. Apply this knowledge when writing any new feature module, ViewModel, or workspace-aware code.

---

## Reference Index

| Topic | File | When to use |
|---|---|---|
| ViewModel wiring, Platform modules, AppGraph contract | `references/metro-di.md` in cmp-practices | Any new screen or module |
| WorkspaceScope child graph, workspace switch lifecycle | This file, sections 4–8 | Workspace-aware code |

---

## 1. Import Reference

```kotlin
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
```

---

## 2. Plain ViewModel

```kotlin
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class CustomerListViewModel(
    private val repository: CustomerRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState: StateFlow<CustomerListUiState> = _uiState.asStateFlow()
}
```

**Screen:**
```kotlin
@Composable
fun CustomerListScreen(
    viewModel: CustomerListViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
}
```

**Rules:**
- Annotation order: `@ContributesIntoMap` → `@ViewModelKey` → `@Inject`
- `metroViewModel()` is always a **trailing default parameter** — never passed from entry provider
- Use `collectAsStateWithLifecycle()`, not `collectAsState()`

---

## 3. Assisted ViewModel (runtime param)

```kotlin
@AssistedInject
class CustomerDetailViewModel(
    @Assisted val customerId: String,
    private val repository: CustomerRepository,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(customerId: String): CustomerDetailViewModel
    }
}
```

**Screen:**
```kotlin
@Composable
fun CustomerDetailScreen(
    customerId: String,
    viewModel: CustomerDetailViewModel = assistedMetroViewModel<CustomerDetailViewModel, CustomerDetailViewModel.Factory>(
        key = customerId
    ) { create(customerId) },
)
```

**Rules:**
- Inner `Factory` must be `fun interface` with a single `fun create(...)` method
- Factory annotation order: `@AssistedFactory` → `@ManualViewModelAssistedFactoryKey` → `@ContributesIntoMap`
- Use `key = id` when the same screen can be opened with different IDs in the same back stack

---

## 4. WorkspaceScope Child Graph Architecture

This is the most important pattern in the project. Every workspace gets its own isolated dependency graph.

```
AppGraph (@DependencyGraph, AppScope)
└── WorkspaceGraph (@GraphExtension, WorkspaceScope)
        └── All workspace-aware databases and delegates live here
```

### Key files

| File | Role |
|---|---|
| `shared/.../di/WorkspaceGraph.kt` | `@GraphExtension(WorkspaceScope::class)` — child graph interface |
| `shared/.../di/WorkspaceManager.kt` | `@SingleIn(AppScope::class)` — creates/destroys child graphs |
| `data/common/.../workspace/WorkspaceClosableRegistry.kt` | `@SingleIn(WorkspaceScope::class)` — tracks closeable DBs in this graph |
| `data/common/.../di/WorkspaceScope.kt` | Scope annotation marker |
| `data/common/.../workspace/WorkspaceConfig.kt` | `data class(workspaceId, workspaceSlug)` — injected into the child graph |

### WorkspaceGraph

```kotlin
@GraphExtension(WorkspaceScope::class)
interface WorkspaceGraph : WorkspaceResources {
    val syncDelegates: Map<SyncEntity, SyncDelegate>
    val syncStateDatabase: SyncStateDatabase
    val metroViewModelFactory: ViewModelProvider.Factory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides config: WorkspaceConfig): WorkspaceGraph
    }
}
```

- `@GraphExtension` inherits ALL parent `AppScope` bindings — workspace VMs resolve both workspace and app deps
- `@Provides config: WorkspaceConfig` on the factory param makes `WorkspaceConfig` injectable everywhere in the child graph

### WorkspaceManager (activateWorkspace)

```kotlin
override fun activateWorkspace(workspaceId: String, workspaceSlug: String, userId: String) {
    val oldSession = _session.value
    oldSession?.graph?.workspaceResources?.close()   // closes all registered DBs from old graph
    centralSyncService.stop()
    centralSyncService.setDelegates { emptyMap() }   // lambda — lazy, not eager

    val config = WorkspaceConfig(workspaceId, workspaceSlug)
    val graph = workspaceGraphFactory.create(config)   // new child graph
    val session = WorkspaceSession(++generationCounter, graph, config)
    _session.value = session

    // Delegates resolved lazily on first sync event — avoids force-creating all DBs upfront
    centralSyncService.setDelegates { graph.syncDelegates }
    centralSyncService.start(graph.syncStateDatabase)
    // ...
}
```

---

## 5. WorkspaceScope Database Pattern

Every workspace-aware database module uses `@SingleIn(WorkspaceScope::class)` and registers itself with `WorkspaceClosableRegistry`:

```kotlin
// feature/{name}/src/androidMain/.../FeatureModule.android.kt
@ContributesTo(WorkspaceScope::class)
interface FeatureAndroidModule {
    companion object {
        @Provides
        @SingleIn(WorkspaceScope::class)
        fun provideFeatureDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): FeatureDatabase {
            return factory.createAndroidDatabase<FeatureDatabase>(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "feature_name",
                workspaceSlug = config.workspaceSlug,
            ).also { closableRegistry.register { it.close() } }
        }
    }
}
```

Same pattern for iOS and Desktop (use `factory.createDatabase<FeatureDatabase>(...)`).

**Critical rules:**
- Scope is `WorkspaceScope`, NOT `AppScope` — this is what isolates the DB per workspace
- `@SingleIn(WorkspaceScope::class)` ensures one DB instance per workspace graph lifetime
- `.also { closableRegistry.register { it.close() } }` ensures DB is closed on workspace switch
- **Always use explicit reified type param**: `createAndroidDatabase<FeatureDatabase>(...)` not `createAndroidDatabase(...)` — Kotlin cannot infer `T` through `.also {}` chains

### ❌ Reified type inference gotcha

```kotlin
// ❌ WRONG — Kotlin cannot infer T through .also { ... }
factory.createAndroidDatabase(
    context = context,
    moduleName = "customer",
    workspaceSlug = config.workspaceSlug,
).also { closableRegistry.register { it.close() } }
// Type inference fails: unresolved T

// ✅ CORRECT — explicit type param
factory.createAndroidDatabase<CustomerDatabase>(
    context = context,
    moduleName = "customer",
    workspaceSlug = config.workspaceSlug,
).also { closableRegistry.register { it.close() } }
```

---

## 6. Workspace-Aware ViewModels

ViewModels that belong to the workspace scope are contributed to `WorkspaceScope::class`, not `AppScope::class`:

```kotlin
@ContributesIntoMap(WorkspaceScope::class)   // NOT AppScope!
@ViewModelKey
@Inject
class BusinessOverviewViewModel(
    private val repository: BusinessRepository,
) : ViewModel() {
    init {
        loadOverview()
        syncFromRemote()
    }
}
```

The Compose layer switches the Metro ViewModel factory when the workspace session changes:

```kotlin
// AppNavigationNav3.kt
val effectiveFactory = remember(workspaceSession) {
    // WorkspaceGraph @GraphExtension inherits all parent (AppScope) bindings, so its factory
    // already resolves both workspace-feature VMs and app/auth VMs.
    workspaceSession?.graph?.metroViewModelFactory ?: appFactory
}

CompositionLocalProvider(LocalMetroViewModelFactory provides effectiveFactory) {
    key(workspaceSession?.generation ?: 0L) {
        NavDisplay(...)
    }
}
```

**Two mechanisms working together:**
1. `key(generation)` — forces NavDisplay and its per-entry ViewModelStores to fully remount (stale VM instances from the old workspace are discarded)
2. `effectiveFactory` swap — `metroViewModel()` on any screen resolves from the new workspace graph, picking up the new DB instances

---

## 7. Stale Data After Workspace Switch — Root Causes and Fixes

This is the most dangerous bug class in the codebase. It has multiple independent root causes.

### Root Cause 1: Stale Ktor `X-Workspace-ID` header

**Symptom:** After switching workspace, new workspace's ViewModels call the API with the OLD workspace ID. Server returns old workspace's data, which gets saved into the new workspace's DB file.

**Mechanism:**
- `TokenRepositoryImpl.cachedWorkspaceId` is set only in `setCurrentUser()` (login time)
- `getWorkspaceIdSync()` returns `cachedWorkspaceId` without hitting the DB
- Ktor's `defaultRequest` plugin calls `getWorkspaceIdSync()` on every request
- `WorkspaceListViewModel.selectWorkspace()` persists the new workspace ID to DB via `setWorkspaceIdForUser()` but does NOT update the in-memory cache

**Fix (WorkspaceListViewModel.selectWorkspace):**
```kotlin
userWorkspaceRepository.setWorkspaceIdForUser(currentUserId, workspaceId)
appPreferences.setLastWorkspaceId(workspaceId)
// Immediately refresh the in-memory token-repo cache so that the Ktor
// defaultRequest plugin sends the correct X-Workspace-ID header for
// every API call that follows (including syncFromRemote in new VMs).
tokenRepository.getWorkspaceId()   // ← THIS is the fix

workspaceActivator.activateWorkspace(workspaceId, workspace.slug, currentUserId)
```

**Why `getWorkspaceId()` works:** The async version reads from the DB and updates `cachedWorkspaceId` as a side effect. It must be called before `activateWorkspace()` so new VMs see the correct header from their very first API call.

---

### Root Cause 2: No ViewModel remount on workspace switch

**Symptom:** After switching workspace, the same ViewModel instance (from the old workspace) is reused — it still holds a reference to the old DB's DAO.

**Fix:** Wrap `NavDisplay` in `key(workspaceSession?.generation ?: 0L)`:
```kotlin
key(workspaceSession?.generation ?: 0L) {
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        ...
    )
}
```

When `generation` changes, Compose discards all child state including `ViewModelStore`s from `rememberViewModelStoreNavEntryDecorator()`. Every `metroViewModel()` call creates a fresh instance from the new workspace's factory.

---

### Root Cause 3: Stale DB instances from eager delegate resolution

**Symptom:** Creating a new workspace graph immediately materializes ALL sync delegates (and thus ALL databases) before they are actually needed, making DB creation eager and potentially using wrong state.

**Fix:** Lazy delegate resolver in `CentralSyncService`:
```kotlin
// WorkspaceManager — pass a lambda, not the map itself
centralSyncService.setDelegates { graph.syncDelegates }   // ✅ lazy
// NOT:
centralSyncService.setDelegates(graph.syncDelegates)       // ❌ eager — forces all DBs immediately
```

`graph.syncDelegates` accessing the map property is deferred until the first sync event fires.

---

### Root Cause 4: Old graph resources not closed

**Symptom:** Old workspace's DB file handles remain open, causing write conflicts or Room schema conflicts when the new workspace tries to open its DB.

**Fix:** `WorkspaceClosableRegistry.closeAll()` is called by `workspaceResources.close()` in `WorkspaceManager.activateWorkspace()`. This is automatic IF databases registered via `.also { closableRegistry.register { it.close() } }`.

---

## 8. New Workspace-Aware Module Checklist

- [ ] Database `@ContributesTo(WorkspaceScope::class)` module (per platform)
- [ ] `@Provides @SingleIn(WorkspaceScope::class)` on the DB provider function
- [ ] Explicit reified type param `createDatabase<MyDatabase>(...)` — never omit it
- [ ] `.also { closableRegistry.register { it.close() } }` on DB creation
- [ ] `config: WorkspaceConfig` injected for `workspaceSlug` (and `workspaceId` if needed)
- [ ] DAOs: `@Provides` in common `@ContributesTo(WorkspaceScope::class)` module (unscoped)
- [ ] Repositories: `@Inject` class, unscoped
- [ ] ViewModels: `@ContributesIntoMap(WorkspaceScope::class)` + `@ViewModelKey` + `@Inject`
- [ ] DB path: Android flat `workspace_{slug}_{module}.db`, iOS/Desktop directory `workspace_{slug}/{module}.db`

---

## 9. AppGraph Contract (must not grow)

`AppGraph` in `shared/commonMain/di/AppGraph.kt` has exactly four properties:

```kotlin
interface AppGraph {
    val themeManager: ThemeManager
    val localeManager: LocaleManager
    val imageLoader: ImageLoader
    val locationService: LocationService
}
```

**Never add** repositories, services, `fun create*ViewModel()` methods, or any feature dep. All feature dependencies flow through Metro-injected ViewModels.

---

## 10. Anti-Patterns Summary

```kotlin
// ❌ Koin — do not use
val featureModule = module { single { ... } }

// ❌ AppScope for workspace-aware DB
@Provides @SingleIn(AppScope::class)
fun provideFeatureDatabase(...): FeatureDb = ...   // stale after workspace switch!

// ❌ Missing reified type param through .also
factory.createAndroidDatabase(...).also { ... }    // T inference fails

// ❌ Eager delegate resolution — forces all DBs on workspace switch
centralSyncService.setDelegates(graph.syncDelegates)

// ❌ No key(generation) — old VMs survive workspace switch
NavDisplay(backStack = backStack, ...)   // without key() wrapper

// ❌ Wrong factory scope for workspace VMs
@ContributesIntoMap(AppScope::class)     // workspace VM in app scope — wrong!
class BusinessOverviewViewModel(...)

// ❌ Forgetting tokenRepository.getWorkspaceId() before activating new workspace
workspaceActivator.activateWorkspace(...)   // Ktor header still stale!

// ❌ LocalAppGraph.current inside composable
val graph = LocalAppGraph.current   // NEVER

// ❌ ViewModel passed from entry provider
NavEntry(key) { MyScreen(viewModel = MyViewModel(...)) }   // breaks Metro wiring
```
