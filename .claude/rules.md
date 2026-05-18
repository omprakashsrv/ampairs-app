# Project Rules for Claude Code

These rules are enforced for all Claude Code interactions in this project.

---

## Code Rules

### MUST always do
- Use `factory {}` (never `single {}`) for any workspace-aware Koin component (Database, DAO, Repository, Store)
- Generate UIDs in the ViewModel layer using `UidGenerator.generateUid(prefix)` before calling the repository
- Null-check `Response<T>.data` before use — there is no `.success` property
- Use `kotlinx.datetime.Clock.System.now()` for timestamps, never `System.currentTimeMillis()`
- Use string interpolation `"$value"` for formatting, never `String.format()`
- Use `Dispatchers.Default` (or `DispatcherProvider.io`) on iOS — never `Dispatchers.IO`
- Use `Kermit` or the domain-specific logger (`CustomerLogger`, etc.) — never `println()` or `Log.d()`
- Add new Gradle dependencies via `gradle/libs.versions.toml` version catalog only — no hardcoded versions
- Use `ApiUrlBuilder.{domain}Url("v1/path")` for all API URLs — never hardcoded strings
- Load all user-visible strings from resources — never hardcode UI text in Kotlin/Compose source files:
  - **Android** (non-Compose): `context.getString(R.string.xxx)` — strings in the module's own `androidMain/res/values/strings.xml`
  - **Compose Multiplatform UI**: `stringResource(Res.string.xxx)` — strings in the module's own `commonMain/composeResources/values/strings.xml`
- Run `./gradlew shared:compileKotlinIosSimulatorArm64` to validate iOS compilation after any commonMain change

### MUST never do
- Put `java.*` or `android.*` imports in `commonMain` source sets
- Create a new `DataStore<Preferences>` instance — always reuse the existing one from `data/common/`
- Allow the repository to generate UIDs as a fallback
- Use `single {}` for Database/DAO/Repository/Store in workspace-aware feature modules
- Add `AuthRoomDatabase` or `WorkspaceRoomDatabase` as `factory` — these must stay `single`
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

### Koin DI chain for every new feature module
```
PlatformModule.{platform}.kt  → factory<Database>
{Feature}Module.kt (common)   → factory DAOs, factory Repositories, factory Stores, viewModel/viewModelOf ViewModels
```

### New workspace-aware module checklist
- [ ] Database: `factory` in platform module
- [ ] DAOs: `factory` in common module
- [ ] Repositories: `factory` in common module
- [ ] Stores: `factory` in common module
- [ ] ViewModels: `viewModel` or `viewModelOf`
- [ ] Platform DB path handles Android (`workspace_{slug}_{module}.db`) vs iOS/Desktop (`workspace_{slug}/{module}.db`)
- [ ] `DatabaseScopeManager` integrated in platform factory

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