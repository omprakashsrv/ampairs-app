# Project Rules for Claude Code

These rules are enforced for all Claude Code interactions in this project.

---

## Code Rules

### MUST always do
- Use `@Inject` + `@ContributesIntoMap(WorkspaceScope::class)` + `@ViewModelKey` on every plain ViewModel of a workspace feature; `AppScope` only for ViewModels that exist before/outside a workspace (auth, workspace picker, app update)
- Use `@AssistedInject` + inner `Factory` interface (`@AssistedFactory` + `@ManualViewModelAssistedFactoryKey` + `@ContributesIntoMap(WorkspaceScope::class)`, or `AppScope` per the rule above) for ViewModels that need a runtime param (e.g. an ID)
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
- Format money with `formatMoney(amount, LocalAppLocale.current)` (or `currencySymbol(locale.currencyCode)` for the bare symbol) from `com.ampairs.common.locale` — never hardcode `₹`/`$` or call `toInr()`/`asRupee()` in UI. The symbol/grouping comes from the workspace's business currency. See `/cmp-practices` §12.
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
- Hardcode a currency symbol (`₹`/`$`) or call `toInr()`/`asRupee()` in UI — they ignore the workspace's business currency. Use `formatMoney(...)` / `currencySymbol(...)` (`com.ampairs.common.locale`). For non-composable code (print/HTML/export) pass the symbol in as a `String` param from a composable that read `LocalAppLocale.current`
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

**Tables** live in the consolidated `AmpairsWorkspaceDatabase` (`:data:database`) — a feature never
declares its own `@Database` or provides a DB. Entity + DAO go in
`data/database/src/commonMain/kotlin/com/ampairs/{feature}/data/db/`, the DAO is exposed via
`WorkspaceDatabaseDaoModule`, and the version bump + migration follow CLAUDE.md →
*Adding tables for a feature*.

**DAO provider** (`data/database/.../di/WorkspaceDatabaseDaoModule.kt`, unscoped):
```kotlin
@Provides
fun provideMyDao(db: AmpairsWorkspaceDatabase): MyDao = db.myDao()
```

**Common classes** in the feature (annotate directly — no module file needed):
```kotlin
// Repository: local-only — DAO + SyncStateDao, never the Api (that lives in MySyncDelegate)
@Inject class MyRepository(private val dao: MyDao, private val syncStateDao: SyncStateDao)

// Plain ViewModel (workspace feature)
@ContributesIntoMap(WorkspaceScope::class) @ViewModelKey @Inject
class MyViewModel(private val repo: MyRepository) : ViewModel()

// Assisted ViewModel (needs runtime param)
@AssistedInject
class MyViewModel(@Assisted val id: String, private val repo: MyRepository) : ViewModel() {
    @AssistedFactory @ManualViewModelAssistedFactoryKey @ContributesIntoMap(WorkspaceScope::class)
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
- [ ] Entity + DAO in `:data:database` (feature package, `androidx.room3`); mappers stay in the feature
- [ ] Registered in `AmpairsWorkspaceDatabase` + `version` bumped + migration from the exported schema
      JSON, registered in all 3 `DatabaseModule.*.kt` (`fallbackToDestructiveMigration` is banned)
- [ ] DAO `@Provides` in `WorkspaceDatabaseDaoModule`; feature depends on `projects.data.database`,
      applies no `ksp`/`room` plugin
- [ ] Repositories: unscoped `@Inject` class (safe across workspace switches)
- [ ] ViewModels: `@Inject` + `@ContributesIntoMap(WorkspaceScope::class)` + `@ViewModelKey`; or `@AssistedInject` + inner `Factory` if a runtime param is needed

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

- Repository is local-only: write to Room with `synced = false`, then `syncStateDao.markPendingPush(SyncEntity.X, now)`. The repository's create/update/delete path MUST NOT inject or call the feature `Api`
- All entity ↔ server traffic (bulk push, batched pull, backend events) lives in `{Name}SyncDelegate`, which injects the `Api` + `Dao` and is `@ContributesIntoMap(WorkspaceScope::class)`
- Delete soft-deletes (`active = false, synced = false`) — never just `active = false`, or the push (which reads `synced = 0`) won't send it
- Pull is batched and permanently deletes rows the server reports `DELETED`/`active = false`; local unsynced edits win
- Conflict resolution: local unsynced changes always win over server data during pull sync
- Batch sync: default 100 records/batch, max 10,000/cycle, always check `hasNext`
- Sync timestamp authority: use server's `updatedAt` string (ISO 8601) — never client clock for sync tracking
- Allowed exception: the repo may keep the `Api` only for a non-sync, UI-invoked feature (import-from-master / available-for-import; file entity-scoped pull / set-primary)
- ViewModels never call `repository.syncXxx()`; reactive DAO `Flow` for UI + `syncService.emit(TriggerPull/TriggerFullSync(X))` for refresh. Full guide: `/offline-sync`

---

## Git & Build Rules

- Always test all 3 targets before marking a shared-module task complete:
  ```bash
  ./gradlew androidApp:compileDebugKotlin
  ./gradlew shared:compileKotlinIosSimulatorArm64
  ./gradlew desktopApp:compileKotlin
  ```
- Never commit `settings.local.json` — it is gitignored
- Commit `.claude/memory/` and `.claude/rules.md` — they are project-level context