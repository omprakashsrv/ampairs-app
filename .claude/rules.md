# Project Rules for Claude Code

These rules are enforced for all Claude Code interactions in this project.

---

## Code Rules

### MUST always do
- Use `@Inject` + `@ContributesIntoMap(AppScope::class)` + `@ViewModelKey` on every plain ViewModel class
- Use `@AssistedInject` + inner `Factory` interface (`@AssistedFactory` + `@ManualViewModelAssistedFactoryKey` + `@ContributesIntoMap(AppScope::class)`) for ViewModels that need a runtime param (e.g. an ID)
- Declare the ViewModel as a trailing default param in every screen: `viewModel: XxxViewModel = metroViewModel()` or `assistedMetroViewModel<VM, VM.Factory>(key = id) { create(id) }`
- Use `@SingleIn(AppScope::class)` on `@Provides` functions for app-scoped singletons in `@ContributesTo` platform modules
- Generate UIDs in the ViewModel layer using `UidGenerator.generateUid(prefix)` before calling the repository
- Null-check `Response<T>.data` before use — there is no `.success` property
- Use `kotlinx.datetime.Clock.System.now()` for timestamps, never `System.currentTimeMillis()`
- Use string interpolation `"$value"` for formatting, never `String.format()`
- Use `Dispatchers.Default` (or `DispatcherProvider.io`) on iOS — never `Dispatchers.IO`
- Use `Kermit` or the domain-specific logger (`CustomerLogger`, etc.) — never `println()` or `Log.d()`
- Add new Gradle dependencies via `gradle/libs.versions.toml` version catalog only — no hardcoded versions
- Use `ApiUrlBuilder.{domain}Url("v1/path")` for all API URLs — never hardcoded strings
- Load all user-visible strings from Compose resources — never hardcode UI text in Kotlin source files:
  - **Composable context**: `stringResource(Res.string.xxx)`
  - **Non-composable suspend context** (e.g. `androidMain` service/enforcer): `getString(Res.string.xxx)` (suspend, call before `suspendCancellableCoroutine`)
  - Strings go in the module's own `commonMain/composeResources/values/strings.xml`; import from `ampairsapp.{module.path}.generated.resources.*`
  - Do NOT use Android-native `R.string` / `context.getString()` — the KMP android library plugin does not generate an R class
- Run `./gradlew shared:compileKotlinIosSimulatorArm64` to validate iOS compilation after any commonMain change

### MUST never do
- Access `LocalAppGraph.current` inside any `@Composable` — all deps flow through Metro-injected ViewModels
- Access `AppGraphHolder.graph` inside navigation entry providers or screens — only platform entry points (`MainView`, `MainViewController`, `main.kt`) may touch it
- Expose repositories, stores, or feature services in `AppGraph` — only `ThemeManager`, `LocaleManager`, `ImageLoader`, `LocationService` belong there
- Add `fun create*ViewModel()` methods to `AppGraph` — Metro auto-wires ViewModels via `@ContributesIntoMap`
- Put `java.*` or `android.*` imports in `commonMain` source sets
- Create a new `DataStore<Preferences>` instance — always reuse the existing one from `data/common/`
- Allow the repository to generate UIDs as a fallback
- Add feature code directly to `shared/` or `androidApp/`/`desktopApp/` — use the appropriate `feature/` module
- Skip compilation validation after changing a shared module that affects multiple platforms

---

## Architecture Rules

### Module placement
- Feature logic → `feature/{name}/src/commonMain/`
- Platform-specific DB factory → `feature/{name}/src/{android|ios|desktop}Main/`
- Shared infrastructure (DB paths, DataStore, factories) → `data/common/`
- Navigation wiring and top-level DI → `shared/`
- New feature module → create under `feature/` and add to `settings.gradle.kts`

### UI / ViewModel boundary (Metro pattern)

The strict layering rule — no exceptions:
```
Metro injects deps → ViewModel
ViewModel exposes StateFlow / UiEvent → Screen (@Composable)
Screen has zero knowledge of repos, managers, or LocalAppGraph
```

**Screens**: declare `viewModel: XxxViewModel = metroViewModel()` as a trailing default param. Never pass a ViewModel from an entry provider — let Metro create it.

**Entry providers**: only wire navigation callbacks and route key params. Never read `LocalAppGraph.current` — all dependencies reach screens through Metro-injected ViewModels.

**Cross-cutting ambient values** (ThemeManager, LocaleManager): provided as typed `CompositionLocal`s in `App.kt`; never access via the graph inside a composable.

**AppGraph interface** (`shared/commonMain/di/AppGraph.kt`): contains exactly four properties — `themeManager`, `localeManager`, `imageLoader`, `locationService`. No repositories, no services, no factories, no `fun create*ViewModel()` methods.

---

### Metro DI chain for every new feature module

**Platform `@ContributesTo` module** (per platform source set):
```kotlin
@ContributesTo(AppScope::class)
interface MyFeaturePlatformModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideDatabase(...): MyFeatureDatabase = ...
    }
}
```

**Common classes** (annotate directly — no module file needed):
```kotlin
@Inject class MyDao(private val db: MyFeatureDatabase)
@Inject class MyRepository(private val dao: MyDao, private val api: MyApi)

// Plain ViewModel
@ContributesIntoMap(AppScope::class) @ViewModelKey @Inject
class MyViewModel(private val repo: MyRepository) : ViewModel()

// Assisted ViewModel (needs runtime param)
@AssistedInject
class MyViewModel(@Assisted val id: String, private val repo: MyRepository) : ViewModel() {
    @AssistedFactory @ManualViewModelAssistedFactoryKey @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory { fun create(id: String): MyViewModel }
}
```

**Screen**:
```kotlin
@Composable fun MyScreen(
    id: String,
    viewModel: MyViewModel = assistedMetroViewModel<MyViewModel, MyViewModel.Factory>(key = id) { create(id) }
)
```

### New workspace-aware module checklist
- [ ] Database: `@Provides @SingleIn(AppScope::class)` in platform `@ContributesTo` module
- [ ] DAOs and Repositories: `@Inject` class (unscoped — new instance per injection site, safe across workspace switches)
- [ ] Stores: `@Inject` class (unscoped)
- [ ] ViewModels: `@Inject` + `@ContributesIntoMap(AppScope::class)` + `@ViewModelKey`; or `@AssistedInject` + inner `Factory` if a runtime param is needed
- [ ] Platform DB path handles Android (`workspace_{slug}_{module}.db`) vs iOS/Desktop (`workspace_{slug}/{module}.db`)
- [ ] `DatabaseScopeManager` integrated in the platform `@Provides` function

---

## API & DTO Rules

### Migration order when backend changes DTOs
`Backend Analysis → Domain Models → Entities → Repositories → ViewModels → UI`

Fix import errors before logic errors. Compile after each layer.

### Response handling
```kotlin
if (response.data != null && response.error == null) { /* success */ }
```

### Logger signature
```kotlin
SomeLogger.w("Tag", "message", exception)   // w/e/i/d — NOT warn/error/info/debug
```

### Form state
- Store IDs as `String`, never as domain object references
- Separate display name from backend value
- Load dropdowns from repositories, never hardcoded enums

---

## Offline-First Rules

- Database-first: always write to Room with `synced = false` before any network call
- Conflict resolution: local unsynced changes always win over server data during pull sync
- Batch sync: default 100 records/batch, max 10,000/cycle, always check `hasNext`
- Sync timestamp authority: use server's `updatedAt` string (ISO 8601) — never client clock for sync tracking
- Store5: keep sync logic in Repository, not in the Store5 fetcher or sourceOfTruth writer

---

## Git & Build Rules

- Always test all 3 targets before marking a shared-module task complete:
  ```bash
  ./gradlew androidApp:compileDebugKotlinAndroid
  ./gradlew shared:compileKotlinIosSimulatorArm64
  ./gradlew desktopApp:compileKotlin
  ```
- Never commit `settings.local.json` — it is gitignored
- Commit `.claude/memory/` and `.claude/rules.md` — they are project-level context