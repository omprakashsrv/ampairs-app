# Metro DI — ViewModel, Repository, and Platform Module Patterns

> **Project DI framework:** Metro (`dev.zacsweers.metro`) + MetroX extensions for ViewModels.
> The project has **migrated away from Koin**. Do not write Koin code.

## Import Reference

```kotlin
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.ContributesTo
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

## 1. Plain ViewModel (no runtime params)

When the ViewModel needs only injected dependencies (no ID or dynamic param from the UI):

```kotlin
// ✅ Plain ViewModel (workspace feature → WorkspaceScope)
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class OrdersViewModel(val orderRepository: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    fun onIntent(intent: OrdersIntent) { ... }
}
```

**Screen usage:**

```kotlin
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

**Rules:**
- Annotation order must be `@ContributesIntoMap` → `@ViewModelKey` → `@Inject` (top to bottom)
- `metroViewModel()` as a **trailing default parameter** — never passed from the entry provider
- Use `collectAsStateWithLifecycle()`, not `collectAsState()`

---

## 2. Assisted ViewModel (runtime param — e.g. an entity ID)

When the ViewModel needs a value only available at runtime (an ID, a route param):

```kotlin
// ✅ Assisted ViewModel — single param
@AssistedInject
class OrderViewViewModel(
    @Assisted val orderId: String,
    val orderRepository: OrderRepository,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(orderId: String): OrderViewViewModel
    }

    // ...
}
```

```kotlin
// ✅ Assisted ViewModel — multiple params (nullable allowed)
@AssistedInject
class OrderViewModel(
    @Assisted fromCustomerId: String?,
    @Assisted toCustomerId: String?,
    @Assisted id: String?,
    val orderRepository: OrderRepository,
    val customerDataService: CustomerDataService,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(fromCustomerId: String?, toCustomerId: String?, id: String?): OrderViewModel
    }
}
```

**Screen usage:**

```kotlin
// Single assisted param
@Composable
fun OrderViewScreen(
    orderId: String,
    onNavigateBack: (orderId: String?) -> Unit,
    viewModel: OrderViewViewModel = assistedMetroViewModel<OrderViewViewModel, OrderViewViewModel.Factory> {
        create(orderId)
    },
)

// Multiple assisted params
@Composable
fun OrderScreen(
    fromCustomerId: String?,
    toCustomerId: String?,
    id: String?,
    viewModel: OrderViewModel = assistedMetroViewModel<OrderViewModel, OrderViewModel.Factory> {
        create(fromCustomerId, toCustomerId, id)
    },
)
```

**Rules:**
- The inner `Factory` interface must be `fun interface` with a single `fun create(...)` method
- Annotation order on `Factory`: `@AssistedFactory` → `@ManualViewModelAssistedFactoryKey` → `@ContributesIntoMap(WorkspaceScope::class)` (`AppScope` only for ViewModels that exist before/outside a workspace — auth, workspace picker, app update)
- `@Assisted` marks only the runtime params; non-assisted params are injected automatically
- `assistedMetroViewModel<VM, VM.Factory> { create(param) }` — the lambda calls your factory method
- No `key =` param needed unless you need multiple instances of the same VM on one screen; the lambda is the disambiguation

---

## 3. Repository and API — Plain `@Inject` (unscoped)

DAOs and Repositories are **unscoped** — Metro creates a new instance per injection site. This is intentional: after a workspace switch, the new instance picks up the new workspace's database.

```kotlin
// ✅ Repository — unscoped @Inject, local-only (DAO + SyncStateDao; the Api lives in the SyncDelegate)
@Inject
class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val syncStateDao: SyncStateDao,
) {
    // Room reads/writes + syncStateDao.markPendingPush(...) — no network (see /offline-sync)
}

// ✅ API impl — @Inject + @ContributesBinding binds the impl to the interface
@Inject @ContributesBinding(AppScope::class)
class InventoryApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : InventoryApi {
    // Ktor implementation — injected only by InventorySyncDelegate
}
```

**Never** add `@SingleIn(AppScope::class)` to any workspace-aware class (Repository, DAO, Store). See [[cross-platform]] workspace-scoped DB rule.

---

## 4. Database & DAO provision — consolidated, not per feature

A feature module **never declares or provides its own Room database**. All workspace tables live in
`AmpairsWorkspaceDatabase` (`:data:database`), provided once per workspace graph — `@SingleIn(WorkspaceScope::class)`
+ registered with `WorkspaceClosableRegistry` — by `data/database/src/{android,desktop,ios}Main/.../di/DatabaseModule.*.kt`.

For a feature's tables (full steps: CLAUDE.md → *Adding tables for a feature*):

```kotlin
// data/database/src/commonMain/kotlin/com/ampairs/inventory/data/db/dao/InventoryDao.kt
@Dao interface InventoryDao { /* androidx.room3 */ }

// data/database/.../AmpairsWorkspaceDatabase.kt — add the entity to entities = [...], bump version, and:
abstract fun inventoryDao(): InventoryDao

// data/database/.../di/WorkspaceDatabaseDaoModule.kt — unscoped DAO provider
@Provides
fun provideInventoryDao(db: AmpairsWorkspaceDatabase): InventoryDao = db.inventoryDao()
```

**Rules:**
- No per-feature `@ContributesTo` DB modules, no `ksp`/`room` plugins in feature modules — the feature
  just depends on `projects.data.database`
- Every new table needs an `AmpairsWorkspaceDatabase` version bump + a migration (built from the
  exported schema JSON) registered in all three `DatabaseModule.*.kt`; `fallbackToDestructiveMigration` is banned
- Pre-/cross-workspace data (auth, workspace registry, model catalog) lives in the AppScope `AmpairsAppDatabase`

---

## 5. AppGraph — Strict Contract

`AppGraph` interface in `shared/commonMain/di/AppGraph.kt` contains **exactly four properties**:

```kotlin
interface AppGraph {
    val themeManager: ThemeManager
    val localeManager: LocaleManager
    val imageLoader: ImageLoader
    val locationService: LocationService
}
```

**Never add to AppGraph:**
- Repositories, stores, or feature services
- `fun create*ViewModel()` factory methods
- Any feature-specific dependency

All feature dependencies reach screens via Metro-injected ViewModels.

---

## 6. New Feature Module Checklist

- [ ] **Tables** — entity + DAO in `:data:database` (feature package), registered in `AmpairsWorkspaceDatabase` with a version bump + migration
- [ ] **DAO** — unscoped `@Provides` in `WorkspaceDatabaseDaoModule`
- [ ] **Repository** — `@Inject` class, unscoped, local-only (DAO + `SyncStateDao`)
- [ ] **API impl** — `@Inject @ContributesBinding(AppScope::class)` class binding to the interface; injected by the `SyncDelegate`
- [ ] **Plain ViewModel** — `@ContributesIntoMap(WorkspaceScope::class)` + `@ViewModelKey` + `@Inject`
- [ ] **Assisted ViewModel** — `@AssistedInject` + inner `Factory` with `@AssistedFactory` + `@ManualViewModelAssistedFactoryKey` + `@ContributesIntoMap(WorkspaceScope::class)`
- [ ] **Screen** — `viewModel: XxxViewModel = metroViewModel()` or `assistedMetroViewModel<VM, VM.Factory> { create(param) }` as trailing default param

---

## 7. WorkspaceScope — Child Graph for Per-Workspace Isolation

Each workspace gets its own isolated dependency graph. The graph owns that workspace's
`AmpairsWorkspaceDatabase` (Android `workspace_{slug}_main.db`, iOS/Desktop `workspace_{slug}/main.db`);
workspace-aware ViewModels and sync delegates contribute to `WorkspaceScope`, not `AppScope`.

```kotlin
// ✅ Workspace-aware ViewModel
@ContributesIntoMap(WorkspaceScope::class)   // NOT AppScope!
@ViewModelKey
@Inject
class BusinessOverviewViewModel(private val repo: BusinessRepository) : ViewModel()
```

### Workspace switch — ensuring fresh ViewModels

In `AppNavigationNav3.kt`, wrapping `NavDisplay` in `key(generation)` forces complete remount:

```kotlin
key(workspaceSession?.generation ?: 0L) {
    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        // ...
    )
}
```

And the factory is swapped to the new workspace graph:

```kotlin
val effectiveFactory = remember(workspaceSession) {
    workspaceSession?.graph?.metroViewModelFactory ?: appFactory
}
CompositionLocalProvider(LocalMetroViewModelFactory provides effectiveFactory) { ... }
```

### Stale Ktor header after workspace switch

`TokenRepositoryImpl.cachedWorkspaceId` is set only at login. After switching workspace, call `tokenRepository.getWorkspaceId()` (the async version that reads from DB and updates the cache) **before** `activateWorkspace()`:

```kotlin
userWorkspaceRepository.setWorkspaceIdForUser(currentUserId, workspaceId)
tokenRepository.getWorkspaceId()   // refresh cache so Ktor uses correct X-Workspace-ID
workspaceActivator.activateWorkspace(workspaceId, workspace.slug, currentUserId)
```

Without this, the Ktor `defaultRequest` plugin sends the old workspace ID on every API call in the new workspace's ViewModels, corrupting the new workspace's DB with old data.

---

## Anti-Patterns

```kotlin
// ❌ Old Koin — do not use
val featureModule = module {
    single { /* ... */ }
    factory<FeatureRepository> { FeatureRepository(get(), get()) }
    viewModel { FeatureViewModel(get()) }
}

// ❌ A feature declaring/providing its own database — add tables to AmpairsWorkspaceDatabase instead
@Database(entities = [...]) abstract class FeatureDatabase : RoomDatabase()

// ❌ AppScope for the workspace DB — stale data after workspace switch
@Provides @SingleIn(AppScope::class) fun provideAmpairsWorkspaceDatabase(...)

// ❌ Repository injecting the Api — network traffic belongs in the SyncDelegate
@Inject class FeatureRepository(dao: FeatureDao, api: FeatureApi)

// ❌ No key(generation) wrapper around NavDisplay — old VM instances survive workspace switch

// ❌ Eager sync delegate resolution — forces all DBs to initialize on workspace switch
centralSyncService.setDelegates(graph.syncDelegates)   // pass a lambda instead

// ❌ ViewModel created in entry provider — breaks Metro wiring
NavEntry(key) { CustomerListScreen(viewModel = CustomerListViewModel(...)) }

// ❌ Accessing AppGraph inside composable
val graph = LocalAppGraph.current   // NEVER

// ❌ Passing ViewModel as non-default param (caller must provide it)
@Composable fun MyScreen(viewModel: MyViewModel)   // no default = forced injection from outside

// ❌ Forgetting to refresh token cache before workspace activation
workspaceActivator.activateWorkspace(...)   // Ktor still sends old workspace ID!
```
