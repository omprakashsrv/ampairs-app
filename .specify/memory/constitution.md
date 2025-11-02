<!--
Sync Impact Report
===================
Version Change: N/A → 1.0.0
Type: MAJOR (Initial constitution establishment)
Modified Principles: N/A (new document)
Added Sections:
  - Project Topology
  - Core Principles (I-X)
  - Architecture Standards
  - Platform-Specific Guidelines
  - Testing & Quality Gates
  - Development Workflow
  - Governance
Templates Requiring Updates:
  ✅ plan-template.md - Constitution Check references KMP patterns
  ✅ spec-template.md - Capture offline-first and M3 requirements
  ✅ tasks-template.md - Reflect KMP and offline-first workflows
Follow-up TODOs: None
-->

# Ampairs Mobile Application Constitution

## Project Topology

- Ampairs Mobile is a **Kotlin Multiplatform (KMP)** business management client targeting **Android, iOS, and Desktop (JVM)** platforms using **Compose Multiplatform**.
- Backend integration with Spring Boot REST APIs (see `ampairs-backend/`) using JWT authentication and multi-tenant workspace architecture.
- Module structure: `composeApp/` contains shared UI (`commonMain`), platform launchers (`androidMain`, `iosMain`, `desktopMain`), and domain modules (auth, workspace, customer, product, order, invoice, tally).
- Offline-first architecture using **Store5** for caching/sync and **Room Database** for local persistence across all platforms.
- Material Design 3 design system applied consistently via Compose Multiplatform with light/dark theme support.

## Speckit Project Principles

### Purpose

- Deliver a cohesive Ampairs experience across web, mobile, and desktop by sharing Compose-first, offline-ready UX and tenants-aware data flows.
- Treat Speckit as the canonical reference for offline-first MVI design, setting expectations for future modules and platform extensions.

### Guiding Tenets

1. **Domain-led architecture** — Keep business logic in shared modules, exposing platform code only for shell integrations or hardware access; respect module boundaries (`composeApp`, `shared`, `core`, `tallyModule`, `tasks`, `thirdparty`) and add new scope-aware code beneath the closest `com/ampairs` domain package.
2. **Parity through shared flows** — Implement features once in shared code; platform launchers supply UI chrome, navigation, and background triggers (WorkManager, Background App Refresh, timers) while Store5 contracts surface `StateFlow` state to the UI.
3. **Quality at every layer** — Adhere to Kotlin 2.2.20 conventions, trailing commas on multiline constructs, and constructor injection through Koin scopes; guard regressions with `./gradlew :composeApp:check` plus targeted `commonTest` suites, adding platform tests when behavior diverges.
4. **Security and tenant safety** — Persist secrets in `local.properties`, use encrypted DataStore/Room helpers for tenant context, and enforce JWT-authenticated REST/WebSocket calls with tenant headers at repository boundaries.
5. **Collaborative delivery** — Follow Conventional Commits with scoped modules, link AMP issues, and open PRs that capture motivation, module impact, validation commands, and screenshots or payload diffs when UI shifts.

### Implementation Pillars

- **Offline-first lifecycle**: Compose presentation → Store5 cache/invalidation → data sources (Room, Ktor); keep sync jobs inside shared code and orchestrate them per launcher.
- **Dependency discipline**: Add libraries via `gradle/libs.versions.toml`, wire them through Koin modules, and document environment impacts in `ENVIRONMENT_CONFIG.md`.
- **UI consistency**: House shared composables in `composeApp/src/commonMain`, use PascalCase naming, and limit overrides to `androidMain`, `iosMain`, or `desktopMain` when platform constraints require divergence.
- **Tooling readiness**: Run `sdk use java 25` before Gradle tasks, prefer `./gradlew :composeApp:run` for previews, and clean via `./cleanup.sh` when switching branches.

### Working Agreements

- Run relevant Gradle/MVI checks before requesting review and attach logs when failures occur.
- Update feature briefs, environment notes, and impacted specs alongside code changes to keep knowledge centralized.
- Extend existing automation under `tasks/` or `.specify/templates/` rather than introducing isolated tooling.

### Decision Process

- Prefer small, incremental PRs and escalate architectural shifts with written proposals to the owning squad.
- Resolve disagreements by referencing this constitution, project guidelines, and measurable user impact; defer to the feature owner when decisions are evenly balanced.

### Amendments

- Amend Speckit principles via PRs that outline motivation, affected modules, and validation strategy; require approval from the owning squad and at least one cross-platform reviewer.

## Core Principles

### I. KMP Platform Compatibility (NON-NEGOTIABLE)

All shared code MUST use KMP-compatible APIs in `commonMain`. Platform-specific code MUST use expect/actual pattern.

**Rationale**: Platform-specific APIs (java.*, android.*, iOS-specific) leak into shared code causing compilation failures on other targets, breaking cross-platform guarantees.

**Rules**:
- NEVER import `java.*`, `android.*`, or iOS-specific APIs in `commonMain` code
- Use `kotlinx.datetime.Clock.System.now()` for time operations (not `System.currentTimeMillis()`)
- Use `kotlinx.coroutines.*` for concurrency (not `Thread`, `synchronized`, or platform-specific threading)
- Use string interpolation or KMP libraries for formatting (not `String.format()`, `DecimalFormat`)
- File operations MUST use expect/actual pattern with platform-specific implementations
- Compile and test on ALL targets (Android, iOS, Desktop) during development
- Store5 `Fetcher.ofFlow` MUST use KMP-compatible data sources only

### II. Offline-First Architecture (NON-NEGOTIABLE)

All CRUD operations MUST save to local Room database first with `synced = false`, then sync to server asynchronously.

**Rationale**: Ensures immediate UI response, guaranteed data persistence, and seamless operation during network interruptions or poor connectivity.

**Rules**:
- Database writes happen BEFORE network requests
- Client-side UID generation via `UidGenerator.generateUid(prefix)` for deterministic identifiers
- Server sync operations run in background with graceful failure handling
- Unsynced local changes MUST be preserved over server data during conflict resolution
- Batch synchronization using paginated requests (default: 100 records per batch, max 10,000 per sync)
- String-based ISO 8601 timestamp comparison for incremental sync tracking
- Server UID conflicts MUST be corrected to maintain local UID consistency

### III. Workspace-Scoped Database Isolation (NON-NEGOTIABLE)

Each workspace MUST maintain isolated database instances managed by `DatabaseScopeManager`.

**Rationale**: Prevents data leakage between workspaces and ensures proper data isolation when switching workspace contexts.

**Rules**:
- Database instances cached by `{workspaceSlug}:{moduleName}` key
- All workspace-aware Koin dependencies MUST use `factory` (not `single`) scope
- Dependency chain: Database (factory) → DAOs (factory) → Repositories (factory) → Stores (factory) → ViewModels (viewModel)
- `DatabaseScopeManager.clearDatabases()` MUST be called on workspace switch
- Platform-specific path parsing: Android (`workspace_{slug}_{module}.db`), iOS/Desktop (`workspace_{slug}/module.db`)
- ViewModels MUST use `viewModel` or `viewModelOf` (never `single`)
- Non-workspace databases (AuthRoomDatabase, WorkspaceRoomDatabase) may remain as `single`

### IV. Material Design 3 Exclusivity

UI components MUST use Compose Multiplatform with Material 3 design system exclusively.

**Rationale**: Single design system ensures consistent UX, accessibility, and maintainability across all platforms.

**Rules**:
- Import only from `androidx.compose.material3` for Material components
- No other UI frameworks (PrimeNG, Bootstrap, custom CSS frameworks)
- Theme management via `ThemeManager` with Light/Dark/System preferences (default: System)
- Icons from Material Design Icons only
- Reactive theme switching with `StateFlow<ThemePreference>`
- Platform-specific Material 3 adaptations documented when necessary

### V. Backend API Alignment

Mobile DTOs MUST align with backend contracts using snake_case serialization and standard response wrappers.

**Rationale**: Ensures API compatibility with Spring Boot backend and reduces integration friction.

**Rules**:
- Use `@SerialName` annotations for snake_case JSON properties (e.g., `@SerialName("created_at") val createdAt: Instant`)
- All API responses wrapped in `Response<T>` (mobile) matching backend `ApiResponse<T>` structure
- Response shape: `{"success": Boolean, "data": T?, "error": ErrorDetails?, "timestamp": String}`
- Use `java.time.Instant` for timestamps matching backend UTC timestamp standard
- HTTP client (Ktor) includes `X-Workspace-ID` header for multi-tenant requests
- JWT tokens with automatic refresh and bearer authentication

### VI. Store5 Integration Pattern

Data layer MUST use Store5 with Fetcher (network) + SourceOfTruth (Room DB) for offline-first caching.

**Rationale**: Store5 provides robust caching, synchronization, and conflict resolution for offline-first architecture.

**Rules**:
- Fetcher reads from local database (not network) - sync handled separately in repositories
- SourceOfTruth observes Room database via `Flow<T>`
- Writer operations handled through repository layer, not Store5 directly
- Use `store.stream(StoreReadRequest.cached(key, refresh = false))` for reads
- Clear Store5 cache after successful sync operations
- Repository layer coordinates: local write → background sync → cache invalidation

### VII. DTO Migration & Backend Synchronization

Mobile DTOs MUST stay synchronized with backend schema changes following systematic migration patterns.

**Rationale**: Prevents runtime serialization failures and API integration issues when backend contracts evolve.

**Rules**:
- Migration order: Backend Analysis → Domain Models → Entities → Repositories → ViewModels → UI
- Reference backend response DTOs for field names and types
- Import paths: `com.ampairs.common.id_generator.UidGenerator`, `com.ampairs.common.model.Response`
- API patterns: `ApiUrlBuilder.{module}Url("v1/{resource}")`
- Dynamic form data: Store IDs as strings, separate display names from values
- Test layer-by-layer after structural changes, not in batches

### VIII. Form UI Standards

Form screens MUST implement keyboard navigation, proper focus management, and consistent TopAppBar patterns.

**Rationale**: Ensures excellent UX with keyboard accessibility and consistent navigation patterns.

**Rules**:
- Use `LocalFocusManager.current` with proper `KeyboardActions`
- Field navigation: `ImeAction.Next` for fields, `ImeAction.Done` for last field
- `singleLine = true` to ensure Enter moves to next field
- Include keyboard-accessible save button at bottom of form
- Remove redundant `navigationIcon` when global navigation exists
- Use `AppScreenWithHeader` pattern consistently
- No redundant `onNavigateBack` parameters in form screens

### IX. Dynamic Module Navigation System

Module navigation MUST integrate backend-installed modules with local implementations via `ModuleRegistry`.

**Rationale**: Enables backend-driven feature discovery with type-safe local routing and graceful degradation.

**Rules**:
- `ModuleRegistry` maps module codes to local `Route.*` destinations
- Module providers implement `IModuleNavigationProvider` interface
- Direct navigation via `tryNavigateToModule()` with registry lookup
- "Update App" dialog for modules without local implementation
- Workspace context synchronized: business context (`WorkspaceContextManager`) + database context (`WorkspaceContext`)
- Module code mappings: `"customer-management"` → `Route.Customer`, etc.

### X. Time/Date Handling

All timestamps MUST use `kotlinx.datetime.Instant` for KMP compatibility and UTC consistency.

**Rationale**: Aligns with backend `java.time.Instant` standard and ensures timezone correctness across platforms.

**Rules**:
- Use `Clock.System.now()` for current time (not `System.currentTimeMillis()`)
- Database columns store timestamps as ISO 8601 strings or Unix epoch milliseconds
- JSON serialization via `@Serializable` with `kotlinx.datetime.Instant`
- Display timestamps in user's local timezone via platform-specific formatters
- Sync comparisons use ISO 8601 string natural ordering

## Architecture Standards

### Module Structure

- **Main Module**: `composeApp/` with `commonMain/`, `androidMain/`, `iosMain/`, `desktopMain/`
- **Domain Modules**: Separate packages for auth, workspace, customer, product, order, invoice, tally
- **Shared Resources**: Strings, colors, themes in `commonMain/resources`
- **Platform Launchers**: Thin platform-specific entry points delegating to shared code

### Dependency Injection (Koin)

- **Modular Setup**: Feature-based modules per domain
- **Platform-Specific**: Separate Android/iOS/Desktop implementations
- **ViewModel Pattern**: `koinInject { parametersOf(id) }` for parameterized injection
- **Workspace-Aware**: Use `factory` scope for workspace-scoped components
- **Layers**: API → Repository → Store5 → ViewModel

### Navigation

- **Type-Safe Routes**: `@Serializable sealed interface Route` with data class implementations
- **Navigation**: `navController.navigate(Route.CustomerDetails(customerId))`
- **Type Extraction**: `backStackEntry.toRoute<Route.CustomerDetails>()`
- **Side Drawer**: Primary navigation pattern (no hardware back button on iOS)

### Background Sync

- **Android**: WorkManager for periodic/constraint-based sync
- **iOS**: Background App Refresh with proper entitlements
- **Desktop**: Timer-based coordination for scheduled sync
- **Retry Logic**: Exponential backoff for failed sync attempts

## Platform-Specific Guidelines

### Android

- **Min SDK**: 24, **Target SDK**: 35
- **Database**: Room SQLite with native drivers
- **Background**: WorkManager for sync tasks
- **Notifications**: Local notifications via NotificationCompat

### iOS

- **Requirements**: Xcode 15+, iOS 15+ deployment target
- **Database**: Room with Core Data bridge
- **Dispatchers**: Use `Dispatchers.Default` for IO operations (no `Dispatchers.IO`)
- **File Paths**: Documents directory for writable storage via `getIosDatabasePath()`
- **Koin**: Initialize in `MainViewController` before app launch
- **APIs**: `@OptIn(ExperimentalForeignApi::class)` for Foundation APIs

### Desktop (JVM)

- **Database**: Room with JDBC drivers
- **Sync**: Timer-based background coordination
- **Window**: Native window controls and theming parity

## Testing & Quality Gates

- **Build Commands**: `./gradlew compileDebugKotlinAndroid compileKotlinIosSimulatorArm64 compileKotlinDesktop`
- **Android Tests**: `./gradlew composeApp:testDebugUnitTest`
- **Multiplatform**: `./gradlew check` for cross-platform validation
- **Platform Tests**: Add `desktopTest`, `iosTest` when touching native bridges
- **Coverage**: Critical flows ≥80%, integration tests for offline-first sync
- **CI**: All targets must compile and test before merge

## Development Workflow

### Code Organization

- **Package Pattern**: `com.ampairs.{domain}.{layer}` (e.g., `com.ampairs.customer.repository`)
- **Naming**: `@SerialName` for snake_case API compatibility
- **DTOs**: Separate request/response DTOs aligned with backend contracts
- **Entities**: Room entities with `@Entity`, `@PrimaryKey`, workspace isolation

### Branching & Commits

- **Feature Branches**: `###-feature-name` matching backend convention
- **Commits**: Conventional Commits (`feat:`, `fix:`, `refactor:`)
- **Scope**: Per logical unit, reference backend issues when applicable

### Review & CI Expectations

- **PRs**: Describe scope, affected modules, validation commands
- **Compilation**: All platforms MUST compile before requesting review
- **Screenshots**: Attach UI changes for Android/iOS/Desktop
- **Testing**: Demonstrate offline-first sync behavior

### Documentation Requirements

- **CLAUDE.md**: Update when introducing new architectural patterns
- **Platform Docs**: Document platform-specific workarounds or limitations
- **Migration Notes**: Record DTO alignment changes with backend versions

## Governance

### Amendment Procedure

This constitution supersedes other practice guides. Amendments require:

1. Documentation of proposed changes with rationale
2. Impact analysis across platforms and modules
3. Architect approval
4. Migration plan for breaking changes
5. Updates to dependent templates (plan, spec, tasks)

### Versioning Policy

- Uses semantic versioning (`MAJOR.MINOR.PATCH`)
- **MAJOR**: Breaking governance changes or principle redefinitions
- **MINOR**: New principles or materially expanded guidance
- **PATCH**: Clarifications, wording fixes, or typo corrections

### Compliance Review

- All pull requests MUST verify compliance with constitution principles
- Deviations require documented justification in `plan.md` under Complexity Tracking
- Templates MUST stay aligned with constitution
- Review constitution quarterly to confirm ongoing relevance

### Living Document

The constitution evolves with real-world lessons. When new patterns emerge (Store5 optimizations, KMP best practices), encode them through the amendment procedure and document knowledge in CLAUDE.md.

**Version**: 1.0.0 | **Ratified**: 2025-01-11 | **Last Amended**: 2025-01-11
