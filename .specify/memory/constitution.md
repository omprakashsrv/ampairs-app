<!--
Sync Impact Report
===================
Version Change: 1.0.0 → 2.0.0
Type: MAJOR (principle redefinitions — DI framework, data layer, and module
      topology changed; several principles rewritten or replaced)

Modified Principles:
  - III. Workspace-Scoped Database Isolation → now Metro WorkspaceScope child
    graphs (was Koin DatabaseScopeManager + factory scope)
  - V. Backend API Alignment → Response<T> nullable-data contract (no .success)
  - VI. Store5 Integration Pattern → REPLACED by "Offline-First via
    CentralSyncService" (Store5 removed from the project)
  - X. Time/Date Handling → adds business-timezone bucketing guidance

Added Sections / Principles:
  - Principle IV. Metro Dependency Injection & MVI Boundary (new)
  - Principle XI. Compose Resources & Workspace-Locale Formatting (new)
  - Replaced the "Speckit Project Principles" prose block with the modern
    Core Principles set

Removed Sections:
  - Koin dependency-injection guidance (project fully migrated to Metro)
  - Store5 caching guidance (replaced by CentralSyncService / SyncDelegate)
  - composeApp/ single-module topology (replaced by multi-module layout)

Templates Requiring Updates:
  ✅ plan-template.md  — generic, constitution-driven Constitution Check gate (no edit needed)
  ✅ spec-template.md  — no constitution coupling (no edit needed)
  ✅ tasks-template.md — no constitution coupling (no edit needed)

Follow-up TODOs: None
-->

# Ampairs Mobile Application Constitution

## Project Topology

- Ampairs Mobile is a **Kotlin Multiplatform (KMP)** business management client targeting
  **Android, iOS, Desktop (JVM), and WebAssembly** (WASM experimental) using **Compose
  Multiplatform** with an **offline-first** architecture.
- Part of a three-tier ecosystem: Spring Boot backend (`/ampairs_service` + domain modules),
  Angular web (`/ampairs-web`), and this KMP app (`/ampairs-app`). Backend integration is over
  REST with JWT auth, multi-tenant workspace headers, and offline-first synchronization.
- **Multi-module Gradle layout** (the project migrated off the old single `composeApp/`):
  - `shared/` — Compose Multiplatform UI, app navigation (Navigation3), DI root, Firebase
  - `androidApp/`, `desktopApp/`, `iosApp/` — thin platform entry points
  - `data/common/` — DB factories, path providers, DataStore, `ApiUrlBuilder`, sync infrastructure
  - `tallyModule/` — Tally ERP integration (JVM-only)
  - `feature/{name}/` — isolated feature modules (auth, agent, aws, business, customer, event,
    form, inventory, invoice, order, product, store, subscription, tax, unit, update, workspace)
- Material Design 3 (with Material Kolor dynamic color) applied consistently across all platforms
  with light/dark/system theme support.

## Core Principles

### I. KMP Platform Compatibility (NON-NEGOTIABLE)

All shared code MUST use KMP-compatible APIs in `commonMain`. Platform-specific behavior MUST use
the expect/actual pattern in the appropriate platform source set.

**Rationale**: Platform-only APIs (`java.*`, `android.*`, iOS Foundation) leak into shared code and
break compilation on other targets, defeating the cross-platform guarantee.

**Rules**:
- NEVER import `java.*` or `android.*` in `commonMain`.
- Use `kotlinx.datetime.Clock.System.now()` for time (not `System.currentTimeMillis()`).
- Use `kotlinx.coroutines.*` and `@Volatile` for concurrency (not `Thread`/`synchronized`).
- Use string interpolation for formatting (not `String.format()`/`DecimalFormat`).
- File/path/UUID/logging operations MUST use expect/actual or KMP libraries (Kermit for logging).
- `Dispatchers.IO` is safe in `commonMain` (expect), but in `iosMain` actuals use
  `Dispatchers.Default` — the Native `Dispatchers.IO` backing value is internal.
- Validate every commonMain change on all targets:
  `./gradlew shared:compileKotlinIosSimulatorArm64 androidApp:compileDebugKotlinAndroid desktopApp:compileKotlin`.

### II. Offline-First via CentralSyncService (NON-NEGOTIABLE)

All CRUD MUST write to the local Room database first (`synced = false`) and flag the entity
`PENDING_PUSH`; the network is reached only asynchronously, never from the repository.

**Rationale**: Guarantees immediate UI response and durable local data, and makes every write
offline-safe by construction (one path: write + flag).

**Rules**:
- The **repository is local-only**: it writes Room and calls
  `syncStateDao.markPendingPush(SyncEntity.X, Clock.System.now().toEpochMilliseconds())`. It MUST
  NOT inject or call the feature `Api` in the create/update/delete path.
- All entity↔server traffic (bulk push, batched pull, backend-event refresh) lives in the
  `{Name}SyncDelegate` (`@ContributesIntoMap(WorkspaceScope::class)` + `@SyncEntityKey`), the single
  place that injects the `Api` + `Dao`. `CentralSyncService` is the only coordinator.
- Canonical contract: `GET`/`POST {domain}Url("v1/{resource}/sync")` with snake_case params
  (`last_sync`, `page`, `size`, `sort_by`, `sort_dir`); pull includes soft-deleted rows; delete is
  in-band (push carries `active = false` rows). Batch 100/cycle, cap 10,000, honor `hasNext`.
- Delete is a **soft-delete** (`active = false, synced = false`) — never just `active = false`, or
  the push (which reads `synced = 0`) will never send it.
- Conflict resolution: local unsynced edits always win on pull; server `updatedAt` (ISO 8601) is the
  sync-tracking authority; rows the server reports `DELETED`/inactive are hard-deleted locally.
- Allowed exception: a repository may keep the `Api` only for a non-sync, UI-invoked feature
  (import-from-master / available-for-import; file entity-scoped pull / set-primary).

### III. Workspace-Scoped Isolation via Metro Child Graphs (NON-NEGOTIABLE)

Every workspace gets its own Metro child graph (`WorkspaceScope`); all workspace-aware databases
are scoped to it and torn down on workspace switch.

**Rationale**: Prevents cross-workspace data leakage and stale-DB bugs. This replaced the prior
Koin `factory {}` approach, which was error-prone.

**Rules**:
- Workspace-aware databases: `@Provides @SingleIn(WorkspaceScope::class)` inside a per-platform
  `@ContributesTo(WorkspaceScope::class)` module, registered via
  `.also { closableRegistry.register { it.close() } }`, with an explicit reified type param
  (`createDatabase<MyDatabase>(...)` / `createAndroidDatabase<MyDatabase>(...)`).
- DAOs and repositories are **unscoped** `@Inject` classes (fresh instance per injection site).
- ViewModels in the workspace scope use `@ContributesIntoMap(WorkspaceScope::class)`.
- DB paths: Android flat `workspace_{slug}_{module}.db`; iOS/Desktop directory
  `workspace_{slug}/{module}.db`.
- Exceptions living in `AppScope`: `AuthRoomDatabase` and `WorkspaceRoomDatabase` (they predate
  workspace selection).
- On workspace switch, refresh the Ktor `X-Workspace-ID` cache (`tokenRepository.getWorkspaceId()`)
  before `activateWorkspace()`, and remount Nav via `key(generation)`.

### IV. Metro Dependency Injection & MVI Boundary (NON-NEGOTIABLE)

Dependencies flow only through Metro-injected ViewModels. The project has fully migrated from Koin
— do not write Koin code.

**Rationale**: A strict UI/ViewModel boundary keeps screens free of repositories and graph access,
preserving testability and the workspace-switch guarantees.

**Rules**:
- Plain ViewModel: `@ContributesIntoMap(AppScope::class)` + `@ViewModelKey` + `@Inject`. Runtime-param
  ViewModel: `@AssistedInject` + inner `fun interface Factory` (`@AssistedFactory` +
  `@ManualViewModelAssistedFactoryKey` + `@ContributesIntoMap`).
- Screens declare the ViewModel as a trailing default param via `metroViewModel()` /
  `assistedMetroViewModel<VM, VM.Factory>(key = id) { create(id) }`; never passed from entry providers.
- Screens collect state with `collectAsStateWithLifecycle()`; state is `StateFlow<UiState>` and
  one-off effects use a buffered `SharedFlow`/`Channel`. No business logic in composables.
- NEVER read `LocalAppGraph.current` inside a `@Composable`; only platform entry points
  (`MainView`, `MainViewController`, `main.kt`) may touch the graph.
- `AppGraph` exposes exactly four cross-cutting deps — `themeManager`, `localeManager`,
  `imageLoader`, `locationService` — plus the ViewModel factory. No repositories/services/factories.

### V. Backend API Alignment

Mobile DTOs MUST match backend contracts: snake_case JSON, `Response<T>` wrapper, `Instant`
timestamps, and multi-tenant headers.

**Rationale**: Keeps the client wire-compatible with the Spring Boot backend and avoids runtime
serialization failures.

**Rules**:
- Use `@SerialName("snake_case")` for fields; the backend serializes camelCase as snake_case globally.
- Responses use `com.ampairs.common.model.Response<T>` whose `data` is **nullable** — check
  `response.data != null && response.error == null`. There is NO `.success` property.
- Timestamps are `kotlinx.datetime.Instant` (backend `java.time.Instant`, UTC).
- Build URLs with `ApiUrlBuilder.{domain}Url("v1/{resource}")` — never hardcoded strings.
- Ktor sends the JWT bearer token (auto-refresh) and the `X-Workspace-ID` header on tenant requests.
- DTO migration order on backend changes: Backend Analysis → Domain Models → Entities →
  Repositories → ViewModels → UI; fix imports before logic; compile after each layer.

### VI. Client-Side UID Generation

Entity UIDs MUST be generated in the ViewModel layer before calling the repository, via
`UidGenerator.generateUid(prefix)` (import `com.ampairs.common.id_generator.UidGenerator`).

**Rationale**: A fully-formed entity reaching the repository keeps the create→sync→update lifecycle
consistent; a repository fallback would diverge the local UID from the tracked UID.

**Rules**:
- Format `{PREFIX}{YYYYMMDDHHMMSS}{RANDOM}` (32 chars); examples `CUS`, `PRD`, `ORD`, `INV`.
- Repository create methods assert `require(entity.uid.isNotBlank())`; repositories MUST NOT
  generate UIDs as a fallback.

### VII. Material Design 3 Exclusivity

UI MUST use Compose Multiplatform with Material 3 exclusively.

**Rationale**: A single design system ensures consistent UX, accessibility, and maintainability.

**Rules**:
- Import only from `androidx.compose.material3`; no other UI frameworks.
- Use `MaterialTheme.colorScheme.*` tokens — never hardcoded color literals. Material Kolor provides
  dynamic color generation.
- Theme via `ThemeManager` (`StateFlow<ThemePreference>`, options SYSTEM/LIGHT/DARK), applied through
  `PlatformAmpairsTheme`; provided as a `CompositionLocal` in `App.kt`.
- Icon-only/interactive elements MUST set `contentDescription`. Lazy lists MUST supply stable `key`s.

### VIII. Navigation3 Routing

Navigation MUST use Navigation3 with `@Serializable` `NavKey` routes and entry providers.

**Rationale**: Type-safe, multiplatform navigation; the old `navController.navigate()` /
`backStackEntry.toRoute()` API is removed.

**Rules**:
- Top-level routes in `shared/.../navigation/Routes.kt`; per-feature sub-routes as
  `@Serializable sealed interface ...Route : NavKey`.
- Wire screens via entry providers under `navigation/providers/`, combined through
  `CombinedEntryProvider` → `AppNavigationNav3`. Entry providers wire only callbacks and route
  params — they never read the graph or pass ViewModels.
- Dynamic module navigation resolves backend module codes to local routes via `ModuleRegistry`;
  unmapped modules show an "Update App" dialog.

### IX. Form UI Standards

Form screens MUST implement keyboard navigation, focus management, and consistent headers.

**Rationale**: Consistent, accessible data entry across platforms.

**Rules**:
- Use `LocalFocusManager.current` with `KeyboardActions`; `ImeAction.Next` for non-last fields and
  `ImeAction.Done` for the last; `singleLine = true` for proper IME handling.
- Use the `AppScreenWithHeader` pattern; no redundant `navigationIcon`/`onNavigateBack` when a global
  nav drawer exists.
- Form state stores backend IDs as `String` with a separate display name — never full domain object
  references; dropdowns load from repositories, never hardcoded enums.

### X. Time/Date Handling

All timestamps MUST use `kotlinx.datetime.Instant`, stored/synced in UTC.

**Rationale**: Aligns with the backend UTC `Instant` standard and avoids timezone/DST bugs.

**Rules**:
- `Clock.System.now()` for current time; ISO 8601 string ordering for sync comparison.
- **Display and bucketing use the workspace business timezone, not the device timezone.** Bucketing
  an `Instant` to a calendar day/month with `TimeZone.currentSystemDefault()` is a bug when the
  business timezone differs. In composables read `LocalAppLocale.current`; in non-composable code
  inject the business timezone (`BusinessLocaleProvider`) and convert with `TimeZone.of(...)`.

### XI. Compose Resources & Workspace-Locale Formatting

User-visible text, money, and dates MUST be localized — never hardcoded.

**Rationale**: Strings must be translatable and amounts/dates must reflect the active workspace's
business locale (currency, timezone, date/time format), while storage stays UTC.

**Rules**:
- All UI strings come from Compose resources: `stringResource(Res.string.x)` in composables,
  `getString(Res.string.x)` in suspend non-composable code. Strings live in each module's
  `commonMain/composeResources/values/strings.xml`. Do NOT use Android `R.string`.
- Money: `formatMoney(amount, LocalAppLocale.current)` / `currencySymbol(locale.currencyCode)` from
  `com.ampairs.common.locale` — never hardcode `₹`/`$` or call `toInr()`/`asRupee()` in UI.
- Dates: `formatDate`/`formatDateTime(..., LocalAppLocale.current)` — never device-timezone or
  hardcoded patterns. Non-composable builders (print/HTML/export) take the symbol/timezone as params.
- When adding `maven-publish` to a module with `composeResources/`, pin
  `compose.resources { packageOfResClass = "ampairsapp.{module.path}.generated.resources" }`.

## Architecture Standards

### Module Placement

- Feature logic → `feature/{name}/src/commonMain/`; platform DB factories →
  `feature/{name}/src/{android|ios|desktop}Main/`.
- Shared infrastructure (DB paths, DataStore, factories, sync) → `data/common/`.
- Navigation wiring and top-level DI aggregation → `shared/`.
- New feature module → create under `feature/` and register in `settings.gradle.kts`.
- NEVER add feature code directly to `shared/`, `androidApp/`, or `desktopApp/`.

### Dependency Injection (Metro)

- Layers: `@Inject` DAO → `@Inject` Repository (local-only) → `{Name}SyncDelegate` (holds `Api`) →
  `@ContributesIntoMap` ViewModel. App-scoped singletons use `@SingleIn(AppScope::class)` in
  `@ContributesTo` platform modules; workspace-aware DBs use `@SingleIn(WorkspaceScope::class)`.
- New dependencies are added via the `gradle/libs.versions.toml` version catalog only — no hardcoded
  versions; verify KMP support before adding.

### Data Layer

- Room is the single source of truth; reactive DAO `Flow`s drive the UI. UI refresh uses
  `syncService.emit(TriggerPull/TriggerFullSync(X))`; ViewModels never call `repository.syncXxx()`.
- A single configured `DataStore<Preferences>` exists in `data/common/` — reuse it; never create a
  second instance.

### Background Sync

- Android: WorkManager; iOS: Background App Refresh; Desktop: timer-based coordination. Failed
  pushes/pulls persist `PENDING_*` state and retry on reconnect (process-death safe).

## Platform-Specific Guidelines

### Android

- minSdk 24 / targetSdk 36 / compileSdk 36; Java 21+. Room with native SQLite drivers; WorkManager
  for sync; full Firebase (Crashlytics/Analytics/Performance/FCM).

### iOS

- Use `Dispatchers.Default` for IO in `iosMain` actuals. Writable storage via `getIosDatabasePath()`
  (Documents). Metro app graph created in `MainViewController` before launch. Foundation APIs need
  `@OptIn(ExperimentalForeignApi::class)`. Side-drawer navigation (no hardware back).

### Desktop (JVM)

- Room with bundled SQLite; timer-based sync; native window controls and theme parity. Firebase is
  stubbed (no JVM SDK).

### WebAssembly

- `wasmJsMain` target is experimental/minimal; do not assume parity.

## Testing & Quality Gates

- Compile all three primary targets before marking a shared-module change complete:
  ```bash
  ./gradlew androidApp:compileDebugKotlinAndroid
  ./gradlew shared:compileKotlinIosSimulatorArm64
  ./gradlew desktopApp:compileKotlin
  ```
- Run `./gradlew check` / targeted `commonTest` for cross-platform validation; add `iosTest`/
  `desktopTest` when touching native bridges. Critical offline-first sync flows MUST be covered.
- CI: all targets MUST compile and tests MUST pass before merge.

## Development Workflow

### Code Organization

- Package pattern `com.ampairs.{feature}.{layer}` (e.g. `com.ampairs.customer.data.api`).
- Use the domain logger (`CustomerLogger.w/e/i/d(tag, message, throwable)`) or Kermit — never
  `println()` / `Log.d()`.

### Branching & Commits

- Feature branches `###-feature-name`; Conventional Commits (`feat:`, `fix:`, `refactor:`,
  `chore:`); reference AMP/backend issues when applicable.

### Review & CI Expectations

- PRs describe scope, affected modules, and validation commands; attach screenshots for UI changes
  across platforms; demonstrate offline-first behavior where relevant.
- Update `CLAUDE.md` and `.claude/memory/` when introducing new architectural patterns.

## Governance

### Amendment Procedure

This constitution supersedes other practice guides. Amendments require:

1. Documentation of the proposed change with rationale.
2. Impact analysis across platforms and modules.
3. Architect approval.
4. A migration plan for breaking changes.
5. Updates to dependent templates (plan, spec, tasks) where coupling exists.

### Versioning Policy

Semantic versioning (`MAJOR.MINOR.PATCH`):
- **MAJOR**: Backward-incompatible governance changes or principle removals/redefinitions.
- **MINOR**: New principle/section or materially expanded guidance.
- **PATCH**: Clarifications, wording, or factual corrections.

### Compliance Review

- Every PR MUST verify compliance with these principles; deviations require documented justification
  in `plan.md` under Complexity Tracking.
- Templates MUST stay aligned with the constitution. Review the constitution quarterly.

### Living Document

The constitution evolves with real-world lessons. Encode new patterns (Metro DI refinements,
CentralSyncService changes, KMP best practices) through the amendment procedure and mirror durable
knowledge into `CLAUDE.md` and `.claude/memory/`.

**Version**: 2.0.0 | **Ratified**: 2025-01-11 | **Last Amended**: 2026-06-15
