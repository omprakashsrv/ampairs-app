# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@.claude/rules.md
@.claude/memory/project_architecture.md
@.claude/memory/project_versions.md
@.claude/memory/feedback_critical_patterns.md
@.claude/memory/feedback_kmp_rules.md
@.claude/skills/cmp-practices/SKILL.md
@.claude/skills/offline-sync/SKILL.md
@.claude/skills/metro-di/SKILL.md

## Project Overview

**Ampairs Mobile Application** is a Kotlin Multiplatform business management client that integrates with the Ampairs Spring Boot backend system. It targets **Android, iOS, Desktop (JVM), and WebAssembly** platforms using **Compose Multiplatform** with an **offline-first architecture**.

### System Integration

This mobile app is part of a **three-tier Ampairs ecosystem**:

1. **Backend (Spring Boot + Kotlin)** - `/ampairs_service` + domain modules
2. **Web Frontend (Angular + Material Design 3)** - `/ampairs-web`
3. **Mobile App (Kotlin Multiplatform)** - `/ampairs-app` ← **THIS PROJECT**

**Backend Integration**: Consumes REST APIs from Spring Boot backend with JWT authentication, multi-tenant support, and offline-first synchronization.

---

## Module Structure

The project uses a **multi-module Gradle architecture** with clear separation of concerns:

```
ampairs-app/
├── shared/                    # Compose Multiplatform UI, navigation, DI root
│   └── src/{commonMain, androidMain, iosMain, desktopMain, wasmJsMain}/
├── androidApp/                # Android application entry point
├── desktopApp/                # Desktop (JVM) application entry point
├── iosApp/                    # iOS Xcode project wrapper
├── data/common/               # Shared data infrastructure (DB factories, paths, DataStore)
├── tallyModule/               # Tally ERP integration (JVM-only)
├── thirdparty/androidx/paging/compose/   # Custom Paging3 KMP integration
└── feature/                   # 17 isolated feature modules
    ├── auth/                  # Phone/OTP authentication, JWT, device management
    ├── agent/                 # AI chat/agentic actions
    ├── aws/                   # AWS S3 file uploads
    ├── business/              # Business profile and configuration
    ├── customer/              # CRM: customers, groups, types, images
    ├── event/                 # Event management
    ├── form/                  # Dynamic entity form configuration
    ├── inventory/             # Stock tracking and movement
    ├── invoice/               # Invoice creation, GST, PDF
    ├── order/                 # Order management and pricing
    ├── product/               # Product catalog, variants, categories
    ├── store/                 # Workspace settings (offline-sync; module toggles like tax-inclusive pricing, discount visibility)
    ├── subscription/          # In-app billing (StoreKit/Play Billing)
    ├── tax/                   # Tax codes, configurations, calculator
    ├── unit/                  # Unit and unit conversion management
    ├── update/                # In-app update management
    └── workspace/             # Multi-tenant workspace, navigation, members
```

**Each feature module follows this internal structure:**
```
feature/{name}/src/
├── commonMain/kotlin/com/ampairs/{name}/
│   ├── data/api/              # API interface + implementation
│   ├── data/db/               # Room database, DAOs, entities
│   ├── data/repository/       # Repository implementations
│   ├── domain/                # Business logic, domain models
│   ├── di/                    # Metro DI module definitions
│   └── ui/                   # Compose screens and ViewModels
├── androidMain/               # Android-specific DI, DB factories
├── iosMain/                   # iOS-specific DI, DB factories
└── desktopMain/               # Desktop-specific DI, DB factories
```

---

## Technology Stack

| Concern | Library | Version |
|---|---|---|
| Language | Kotlin KMP | 2.4.0 |
| UI | Compose Multiplatform | 1.11.1 |
| Design | Material 3 + Material Kolor | 1.9.0 / 3.0.1 |
| DI | Metro | 1.1.1 |
| Database | Room KMP | 2.8.4 |
| HTTP | Ktor | 3.5.0 |
| Navigation | Navigation3 (AndroidX) | 1.1.1 |
| Image loading | Coil | 3.4.0 |
| Serialization | kotlinx.serialization | (kotlin 2.3.21 bundled) |
| Date/Time | kotlinx.datetime | 0.8.0 |
| Coroutines | kotlinx.coroutines | 1.11.0 |
| Logging | Kermit | 2.1.0 |
| Crash reporting | Sentry KMP | 0.27.0 |
| Firebase | Crashlytics / Analytics / Perf / FCM | BOM 34.14.0 |
| Cloud storage | AWS SDK Kotlin | 1.5.44 |
| Protocol Buffers | Wire | 5.4.0 |
| Real-time | Krossbow (STOMP/WebSocket) | 9.3.0 |
| Preferences | DataStore | 1.2.1 |
| Background sync | WorkManager (Android) | 2.11.1 |
| Paging | AndroidX Paging | 3.3.6 |
| In-app billing | Play Billing / StoreKit | 9.0.0 |
| File picking | FileKit | 0.14.1 |
| Maps | Maps Compose (Android) | 8.3.0 |
| Permissions | Moko Permissions | 0.20.1 |
| Adaptive UI | Material3 Adaptive | 1.2.0 |
| Location | Play Services Location (Android) | 21.3.0 |
| UUID | benasher44/uuid | 0.8.4 |

**Android SDK**: Min 24 / Target 36 / Compile 36 | **Java**: 21+ | **App version**: 1.0.9 (versionCode 109)

---

## Architecture

### Offline-First with CentralSyncService

- **Pattern**: Repository → Room DB (source of truth) + `CentralSyncService` for background sync
- **Layers**: Presentation (Compose/MVI) → ViewModel → Repository → Room DB; network only via `SyncDelegate`
- **Sync**: Database-first writes (`synced = false`), `SyncStateDao.markPendingPush()` triggers automatic bulk push via `CentralSyncService` → `{Name}SyncDelegate`

### Navigation with Navigation3

Navigation3 replaces the old Androidx Navigation Compose. Routes implement `NavKey`:

```kotlin
// Routes.kt - Top-level module routes
@Serializable
sealed interface Route : NavKey {
    @Serializable data object Login : Route
    @Serializable data object Workspace : Route
    @Serializable data object Customer : Route
    @Serializable data object Product : Route
    @Serializable data object Order : Route
    @Serializable data object Invoice : Route
    @Serializable data object Tax : Route
    @Serializable data object Agent : Route
    @Serializable data class FormConfig(val entityType: String = "") : Route
    // ... etc.
}

// Sub-routes per feature
@Serializable sealed interface CustomerRoute : NavKey { ... }
@Serializable sealed interface AuthRoute : NavKey { ... }
```

Navigation is implemented via **entry providers** in `shared/src/commonMain/kotlin/com/ampairs/navigation/providers/`:
- `AuthEntryProvider`, `CustomerEntryProvider`, `ProductEntryProvider`, etc.
- Combined via `CombinedEntryProvider` → `AppNavigationNav3.kt`

### State Management (MVI)

- **ViewModels**: 52+ ViewModels using `metroViewModel()` / `assistedMetroViewModel()`
- **State**: `StateFlow<UiState>` + `SharedFlow<UiEvent>` in each ViewModel
- **Resource**: `Resource<T>` wrapper for Loading/Success/Error states

### Dependency Injection with Metro

Each feature uses Metro DI with `@ContributesTo` platform modules and `@Inject` on repositories/DAOs:

```
{Feature}AndroidModule.kt (androidMain)   ← @ContributesTo(WorkspaceScope), @Provides DB
{Feature}IosModule.kt (iosMain)
{Feature}DesktopModule.kt (desktopMain)
@Inject classes in commonMain             ← DAOs, Repos, ViewModels
```

**CRITICAL**: All workspace-aware databases use `@SingleIn(WorkspaceScope::class)`. See `/metro-di` and the Workspace-Scoped Database section.

---

## Build Commands

### Android
```bash
./gradlew androidApp:assembleDebug
./gradlew androidApp:installDebug
./gradlew androidApp:assembleRelease
```

### Desktop
```bash
./gradlew desktopApp:run
./gradlew desktopApp:package
```

### iOS
```bash
./gradlew shared:embedAndSignAppleFrameworkForXcode
# Or compile for simulator testing:
./gradlew shared:compileKotlinIosSimulatorArm64
```

### Compilation Tests (KMP Validation)
```bash
# Validate all targets compile
./gradlew shared:compileKotlinIosSimulatorArm64
./gradlew androidApp:compileDebugKotlinAndroid
./gradlew desktopApp:compileKotlin
```

### Cleanup
```bash
./gradlew clean
./cleanup.sh
```

---

## Key Development Patterns

### Import Paths — Check These First

```kotlin
// Correct package paths
com.ampairs.common.id_generator.UidGenerator     // NOT .util.UidGenerator
com.ampairs.common.model.Response                 // NOT .core.domain.dto.ApiResponse
```

### API Layer Pattern

```kotlin
// URL building
ApiUrlBuilder.customerUrl("v1/groups")

// Response handling — data is nullable, no .success property
if (response.data != null && response.error == null) { ... }

// Logger signature
CustomerLogger.w("TagName", "message", exception)   // NOT warn()
```

### UID Generation

```kotlin
// ALWAYS generate UIDs in ViewModel before calling repository
val uid = UidGenerator.generateUid(Constants.UID_PREFIX)
// Format: {PREFIX}{YYYYMMDDHHMMSS}{RANDOM} — 32 chars total
```

### Form State Management

- Store backend IDs as `String`, not object references
- Separate display names from backend values
- Reference: `CustomerFormState` in `CustomerFormViewModel.kt`
- UI Dropdowns: load dynamic data from repositories, never hardcoded enums

### DTO Migration Order

`Backend Analysis → Domain Models → Entities → Repositories → ViewModels → UI`

Fix import issues before logic issues. Compile after each layer change.

---

## KMP Platform Compatibility — CRITICAL

Always use KMP-compatible APIs in `commonMain`. Platform-specific code goes in platform source sets via expect/actual.

### What NOT to use in commonMain

| Wrong (JVM-only) | Correct (KMP) |
|---|---|
| `System.currentTimeMillis()` | `Clock.System.now().toEpochMilliseconds()` |
| `Date()`, `Calendar`, `LocalDateTime` | `kotlinx.datetime.*` |
| `String.format()` | String interpolation `"$value"` |
| `Thread`, `synchronized {}` | `kotlinx.coroutines.*`, `@Volatile` |
| `java.io.File`, `java.nio.*` | expect/actual file operations |
| `UUID.randomUUID()` | KMP UUID library or expect/actual |
| `System.out.println()`, `Log.d()` | Kermit logger or expect/actual |
| `java.util.*` specifics | Kotlin stdlib collections |

### Quick Validation

```kotlin
// ❌ Wrong - platform import in commonMain
import java.util.Date
import android.util.Log

// ✅ Correct
import kotlinx.datetime.Clock
import kotlinx.coroutines.flow.Flow
```

### Platform-Specific Implementations

- `data/common/src/` contains `DatabasePathProvider.kt` (expect) + android/ios/desktop actuals
- `WorkspaceAwareDatabaseFactory.kt` (expect) + platform actuals
- `BackNavigationHandler.kt` (expect) + platform actuals
- Firebase modules use expect/actual per platform (no Firebase on Desktop — stub)

---

## Workspace-Scoped Database Management

### The Problem

When users switch workspaces all databases must be replaced. Each workspace gets its own Metro child graph (`WorkspaceScope`) so stale DB instances are impossible when the pattern is followed correctly.

### The Rule: WorkspaceScope for Every Workspace-Aware DB

```
@SingleIn(WorkspaceScope::class) DB → unscoped @Inject DAOs → unscoped @Inject Repositories
→ @ContributesIntoMap(WorkspaceScope::class) ViewModels
```

**Exceptions** — these live in `AppScope`:
- `AuthRoomDatabase` — exists before workspace selection
- `WorkspaceRoomDatabase` — stores the workspace list itself

### Implementation

```kotlin
// ✅ CORRECT — platform @ContributesTo module
@ContributesTo(WorkspaceScope::class)
interface MyFeatureAndroidModule {
    companion object {
        @Provides @SingleIn(WorkspaceScope::class)
        fun provideDatabase(
            factory: WorkspaceAwareDatabaseFactory,
            context: Context,
            config: WorkspaceConfig,
            closableRegistry: WorkspaceClosableRegistry,
        ): MyDatabase = factory.createAndroidDatabase<MyDatabase>(
            context = context,
            moduleName = "my_feature",
            workspaceSlug = config.workspaceSlug,
        ).also { closableRegistry.register { it.close() } }
    }
}
```

See `/metro-di` for the complete Metro DI workspace pattern.

### Database Path Structures

- **Android**: `workspace_{slug}_{module}.db` (flat file)
- **iOS/Desktop**: `workspace_{slug}/{module}.db` (directory + file)

### Verification Checklist for New Modules

- [ ] Database: `@Provides @SingleIn(WorkspaceScope::class)` in platform `@ContributesTo(WorkspaceScope::class)` module
- [ ] DB registered: `.also { closableRegistry.register { it.close() } }`
- [ ] DAOs and Repositories: `@Inject` class, unscoped
- [ ] ViewModels: `@ContributesIntoMap(WorkspaceScope::class)` + `@ViewModelKey` + `@Inject`
- [ ] Path handles Android flat vs iOS/Desktop directory format
- [ ] Explicit reified type param `createDatabase<MyDatabase>(...)` — never omit

---

## Offline-First Data Architecture

### Core Principles

**1. Database-First Writes**
All CRUD saves to Room first (`synced = false`) and flags the entity `PENDING_PUSH` via
`SyncStateDao.markPendingPush(...)`. `CentralSyncService` then runs the bulk push through the
feature's `SyncDelegate` (the only layer that holds the API). The repository never calls the network.
If the push fails, data stays local and retries on reconnect.

**2. Client-Side UID Generation**
`UidGenerator.generateUid(prefix)` → `{PREFIX}{YYYYMMDDHHMMSS}{RANDOM}` (32 chars). Generate in ViewModel, never in repository.

**3. Incremental Sync with String Timestamps**
ISO 8601 strings (`yyyy-MM-ddTHH:mm:ss`) for natural string comparison. Server's `updatedAt` is authoritative for sync tracking.

**4. Paginated Batch Sync**
Default 100 records/batch, max 10,000/sync cycle. Loop protection with `hasNext` guard.

### Repository Create Pattern (local-only — the API lives in the SyncDelegate)

The repository never touches the network. It writes to Room and flags the entity for an automatic
bulk push via `SyncStateDao`; the `{Name}SyncDelegate` owns all API traffic. See `/offline-sync`.

```kotlin
@Inject
class EntityRepository(
    private val dao: EntityDao,
    private val syncStateDao: SyncStateDao,   // NOT the Api
) {
    suspend fun createEntity(entity: Entity): Result<Entity> {
        require(entity.uid.isNotBlank()) { "UID must be set by ViewModel" }
        dao.insertEntity(entity.toEntity().copy(synced = false))
        syncStateDao.markPendingPush(SyncEntity.ENTITY, Clock.System.now().toEpochMilliseconds())
        return Result.success(entity)
    }
    // delete: soft-delete (active = false, synced = false) + markPending — never just active = false
}
```

The push (bulk), pull (batched; permanently deletes server-`DELETED` rows) and backend-event refresh
live in `{Name}SyncDelegate`, which injects the `Api` + `Dao`. CentralSyncService observes the
`PENDING_PUSH` flag and drives the push automatically.

### Conflict Resolution

- Unsynced local changes always win over server data during pull sync
- Server UID conflicts: correct server response to maintain local UID
- Last-write-wins (server timestamp) for synced entities

---

## Dynamic Module Navigation

### Module Registry

`feature/workspace/src/commonMain/.../ModuleRegistry.kt` maps backend module codes to local routes:

```kotlin
"customer-management"  → Route.Customer
"product-management"   → Route.Product
"order-management"     → Route.Order
"invoice-management"   → Route.Invoice
"inventory-management" → not implemented → shows "Update App" dialog
```

### Navigation Flow

```
User taps module → tryNavigateToModule()
    → Registry lookup → found: navigate to Route.{Module}
    → Not found: show "Update App" dialog
    → Fallback: onModuleSelected callback
```

### Adding New Module Support

1. Create provider in `ModuleProviders.kt`
2. Register in `ModuleRegistry.initialize()`
3. Update `DynamicModuleNavigationService`

---

## DataStore — Key-Value Persistence

**IMPORTANT**: A fully configured `DataStore<Preferences>` already exists. Always reuse it for new persistence needs.

### Files
- `data/common/src/commonMain/.../createAppDataStore.kt` (expect + platform actuals)
- `data/common/src/commonMain/.../DataStoreAppPreferences.kt`
- `data/common/src/commonMain/.../DataStoreManager.kt`

### Storage Paths
- **Android**: `context.filesDir/app_preferences.preferences_pb`
- **Desktop**: `~/.ampairs/app_preferences.preferences_pb`
- **iOS**: `Documents/app_preferences.preferences_pb` (`@OptIn(ExperimentalForeignApi::class)`)

### Rules
- Add new preference keys to existing DataStore — never create a separate instance
- Pattern: `DataStore<Preferences>` singleton → Repository → Manager → UI (StateFlow)

---

## Theme System

- **Options**: `ThemePreference.SYSTEM / LIGHT / DARK` (default: LIGHT)
- **Manager**: `ThemeManager` with `StateFlow<ThemePreference>` and `@Composable isDarkTheme()`
- **Usage**: injected via Metro ViewModel or provided as `CompositionLocal` in `App.kt`
- **Apply**: `PlatformAmpairsTheme(darkTheme = themeManager.isDarkTheme())`
- **Set**: `themeManager.setThemePreference(ThemePreference.DARK)`
- **Colors**: Material Kolor 3.0.1 for dynamic color generation

---

## Firebase Integration

Firebase is integrated per-platform with expect/actual:

- **Android/iOS**: Full Firebase (Crashlytics, Analytics, Performance, FCM)
- **Desktop**: Stub implementations (no Firebase SDK for JVM)

Files in `shared/src/`:
- `FirebaseModule.kt` (commonMain expect)
- `FirebaseModule.android.kt`, `FirebaseModule.ios.kt`, `FirebaseModule.desktop.kt`
- `FirebaseCrashlytics.kt`, `FirebaseAnalytics.kt`, `FirebasePerformance.kt`, `FirebaseMessaging.kt`

---

## iOS Platform Notes

- **Dispatchers**: Use `Dispatchers.Default` for IO in `iosMain` — `Dispatchers.IO` actual is internal on Kotlin/Native even at coroutines 1.10.2; `Dispatchers.IO` is safe only in `commonMain` (via expect)
- **File Paths**: Always use Documents directory (`getIosDatabasePath()`)
- **Metro Init**: App graph is created in `MainViewController` before launch
- **Foundation APIs**: Require `@OptIn(ExperimentalForeignApi::class)`
- **Navigation**: Side drawer pattern (no hardware back button)
- **Compilation test**: `./gradlew shared:compileKotlinIosSimulatorArm64`

---

## Form UI Standards

- **Focus Management**: `LocalFocusManager.current` with `KeyboardActions`
- **Field Navigation**: `ImeAction.Next` for non-last fields, `ImeAction.Done` for last
- **Single Line**: Use `singleLine = true` for proper IME action handling
- **TopAppBar**: No redundant `navigationIcon` when global nav drawer exists; no `onNavigateBack` param on form screens
- **Pattern**: Use `AppScreenWithHeader` consistently across all navigation files

---

## Common Issues & Solutions

| Issue | Solution |
|---|---|
| Stale data after workspace switch | Verify DB uses `@SingleIn(WorkspaceScope::class)` and is registered with `WorkspaceClosableRegistry` |
| iOS database path wrong | Use `getIosDatabasePath()`, not hardcoded paths |
| `Dispatchers.IO` crash in `iosMain` | Use `Dispatchers.Default` — the Native actual is internal; `Dispatchers.IO` only works in `commonMain` |
| `java.*` compile error in commonMain | Use KMP equivalent (kotlinx.datetime, etc.) |
| Room migration error | Add migration script, check schema version increment |
| `String.format()` compile error | Use string interpolation `"$value"` |
| `Response<T>.data` NPE | Always null-check; no `.success` property exists |
| Logger compile error | Use `w/e/i/d` not `warn/error`; 3-param signature |

---

## Backend Integration Reference

- **Endpoints**: `/api/v1/{resource}`
- **Headers**: `X-Workspace-ID` for multi-tenant context
- **Response wrapper**: `ApiResponse<T>` / `Response<T>` from `com.ampairs.common.model`
- **Auth**: JWT bearer token, auto-refresh via Ktor plugin
- **UID prefix examples**: `CUS` (customer), `PRD` (product), `ORD` (order), `INV` (invoice)

*Refer to main `/ampairs/CLAUDE.md` for backend guidelines.*

---

## Development Guidelines

- **Package naming**: `com.ampairs.{feature}.{layer}` (e.g., `com.ampairs.customer.data.api`)
- **API models**: Use `@SerialName("snake_case")` for backend field compatibility
- **No platform imports in commonMain**: If `java.*` or `android.*` appear → wrong file
- **Compile all targets frequently**: Don't wait until end of feature to test iOS/Desktop
- **KMP libraries only in version catalog**: Verify KMP support before adding dependency
- **WorkspaceScope vs AppScope**: Workspace-aware DBs = `@SingleIn(WorkspaceScope::class)`; cross-workspace singletons = `@SingleIn(AppScope::class)`
- **Money/locale formatting**: Render amounts with `formatMoney(amount, LocalAppLocale.current)` from `com.ampairs.common.locale` — never hardcode `₹`/`$` or call `toInr()`/`asRupee()` in UI. `LocalAppLocale` carries the workspace business currency (and timezone/date format), sourced per-workspace from `BusinessLocaleProvider` on `WorkspaceGraph` and provided in `AppNavigationNav3`. Storage/sync stay UTC; this is display-only. See `/cmp-practices` §12.