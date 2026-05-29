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
```

---

## 1. Plain ViewModel (no runtime params)

When the ViewModel needs only injected dependencies (no ID or dynamic param from the UI):

```kotlin
// ✅ Plain ViewModel
@ContributesIntoMap(AppScope::class)
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
    @ContributesIntoMap(AppScope::class)
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
    @ContributesIntoMap(AppScope::class)
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
- Annotation order on `Factory`: `@AssistedFactory` → `@ManualViewModelAssistedFactoryKey` → `@ContributesIntoMap(AppScope::class)`
- `@Assisted` marks only the runtime params; non-assisted params are injected automatically
- `assistedMetroViewModel<VM, VM.Factory> { create(param) }` — the lambda calls your factory method
- No `key =` param needed unless you need multiple instances of the same VM on one screen; the lambda is the disambiguation

---

## 3. Repository and API — Plain `@Inject` (unscoped)

DAOs and Repositories are **unscoped** — Metro creates a new instance per injection site. This is intentional: after a workspace switch, the new instance picks up the new database.

```kotlin
// ✅ Repository — unscoped @Inject
@Inject
class InventoryRepository(
    val inventoryDao: InventoryDao,
    val inventoryApi: InventoryApi,
) {
    // business logic
}

// ✅ API impl — @Inject + @ContributesBinding binds the impl to the interface
@Inject @ContributesBinding(AppScope::class)
class InventoryApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : InventoryApi {
    // Ktor implementation
}
```

**Never** add `@SingleIn(AppScope::class)` to any workspace-aware class (Repository, DAO, Store, Database). See [[cross-platform]] workspace-scoped DB rule.

---

## 4. Platform `@ContributesTo` Modules

Database provision and other platform-specific bindings use `@ContributesTo` interface modules.

### Common module — DAO provision

```kotlin
// feature/{name}/src/commonMain/.../FeatureModule.kt
@ContributesTo(AppScope::class)
interface FeatureDaoModule {
    companion object {
        @Provides
        fun provideFeatureDao(db: FeatureRoomDatabase): FeatureDao = db.featureDao()
    }
}
```

### Android platform module

```kotlin
// feature/{name}/src/androidMain/.../FeatureModule.android.kt
@ContributesTo(AppScope::class)
interface FeatureAndroidModule {
    companion object {
        @Provides
        fun provideFeatureDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
        ): FeatureRoomDatabase =
            factory.createAndroidDatabase(
                context = context,
                queryDispatcher = Dispatchers.IO,
                moduleName = "feature_name",   // matches DB filename slug
                migrations = emptyList(),
            )
    }
}
```

### iOS platform module

```kotlin
// feature/{name}/src/iosMain/.../FeatureModule.ios.kt
@ContributesTo(AppScope::class)
interface FeatureIosModule {
    companion object {
        @Provides
        fun provideFeatureDatabase(factory: WorkspaceAwareDatabaseFactory): FeatureRoomDatabase =
            factory.createDatabase(
                klass = FeatureRoomDatabase::class,
                moduleName = "feature_name",
                migrations = emptyList(),
            )
    }
}
```

### Desktop platform module

```kotlin
// feature/{name}/src/desktopMain/.../FeatureModule.desktop.kt
@ContributesTo(AppScope::class)
interface FeatureDesktopModule {
    companion object {
        @Provides
        fun provideFeatureDatabase(factory: WorkspaceAwareDatabaseFactory): FeatureRoomDatabase =
            factory.createDatabase(
                klass = FeatureRoomDatabase::class,
                moduleName = "feature_name",
                migrations = emptyList(),
            )
    }
}
```

**Rules:**
- Database provision has **no `@SingleIn`** — unscoped so the new workspace gets a fresh DB
- Exception: `AuthRoomDatabase` and `WorkspaceRoomDatabase` use `@SingleIn(AppScope::class)` because they exist before workspace selection
- `moduleName` must match the slug used in `WorkspaceAwareDatabaseFactory` path logic

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

- [ ] **Database** — `@Provides` (no `@SingleIn`) in each platform `@ContributesTo` module (android/ios/desktop)
- [ ] **DAO** — `@Provides` in common `@ContributesTo` module, reads from the database
- [ ] **Repository** — `@Inject` class, unscoped
- [ ] **API impl** — `@Inject @ContributesBinding(AppScope::class)` class binding to the interface
- [ ] **Plain ViewModel** — `@ContributesIntoMap(AppScope::class)` + `@ViewModelKey` + `@Inject`
- [ ] **Assisted ViewModel** — `@AssistedInject` + inner `Factory` with `@AssistedFactory` + `@ManualViewModelAssistedFactoryKey` + `@ContributesIntoMap(AppScope::class)`
- [ ] **Screen** — `viewModel: XxxViewModel = metroViewModel()` or `assistedMetroViewModel<VM, VM.Factory> { create(param) }` as trailing default param
- [ ] **DB path** — Android: `workspace_{slug}_{module}.db` flat; iOS/Desktop: `workspace_{slug}/{module}.db` directory

---

## Anti-Patterns

```kotlin
// ❌ Old Koin — do not use
val featureModule = module {
    single<FeatureDatabase> { factory.createDatabase(...) }
    factory<FeatureRepository> { FeatureRepository(get(), get()) }
    viewModel { FeatureViewModel(get()) }
}

// ❌ Scoped workspace-aware DB — causes stale data after workspace switch
@Provides @SingleIn(AppScope::class)
fun provideFeatureDatabase(...): FeatureDatabase = ...

// ❌ ViewModel created in entry provider — breaks Metro wiring
NavEntry(key) { CustomerListScreen(viewModel = CustomerListViewModel(...)) }

// ❌ Accessing AppGraph inside composable
val graph = LocalAppGraph.current   // NEVER

// ❌ Passing ViewModel as non-default param (caller must provide it)
@Composable fun MyScreen(viewModel: MyViewModel)   // no default = forced injection from outside
```
