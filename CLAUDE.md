# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@.claude/rules.md
@.claude/memory/project_architecture.md
@.claude/memory/project_versions.md
@.claude/memory/feedback_critical_patterns.md
@.claude/memory/feedback_kmp_rules.md
@.claude/skills/cmp-practices/SKILL.md
@.claude/skills/offline-sync/SKILL.md

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
└── feature/                   # 16 isolated feature modules
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
│   ├── domain/                # Store5 stores, business logic
│   ├── di/                    # Koin module definitions
│   └── ui/                   # Compose screens and ViewModels
├── androidMain/               # Android-specific DI, DB factories
├── iosMain/                   # iOS-specific DI, DB factories
└── desktopMain/               # Desktop-specific DI, DB factories
```

---

## Technology Stack

| Concern | Library | Version |
|---|---|---|
| Language | Kotlin KMP | 2.3.21 |
| UI | Compose Multiplatform | 1.11.0 |
| Design | Material 3 + Material Kolor | 1.9.0 / 3.0.1 |
| DI | Koin | 4.1.1 |
| Database | Room KMP | 2.8.3 |
| Offline cache | Store5 | 5.1.0-alpha08 |
| HTTP | Ktor | 3.3.2 |
| Navigation | Navigation3 (AndroidX) | 1.0.0-alpha06 |
| Image loading | Coil | 3.3.0 |
| Serialization | kotlinx.serialization | (kotlin 2.3.21 bundled) |
| Date/Time | kotlinx.datetime | 0.7.1 |
| Coroutines | kotlinx.coroutines | 1.10.2 |
| Logging | Kermit | 2.0.8 |
| Crash reporting | Sentry KMP | 0.23.1 |
| Firebase | Crashlytics / Analytics / Perf / FCM | BOM 34.9.0 |
| Cloud storage | AWS SDK Kotlin | 1.5.44 |
| Protocol Buffers | Wire | 5.4.0 |
| Real-time | Krossbow (STOMP/WebSocket) | 9.3.0 |
| Preferences | DataStore | 1.2.0 |
| Background sync | WorkManager (Android) | 2.11.1 |
| Paging | AndroidX Paging | 3.3.6 |
| In-app billing | Play Billing / StoreKit | 8.3.0 |
| File picking | FileKit | 0.12.0 |
| Maps | Maps Compose (Android) | 8.1.0 |
| Permissions | Moko Permissions | 0.20.1 |
| UI blur | Haze | 1.7.2 |
| Adaptive UI | Material3 Adaptive | 1.2.0 |
| Location | Play Services Location (Android) | 21.3.0 |
| UUID | benasher44/uuid | 0.8.4 |

**Android SDK**: Min 24 / Target 36 / Compile 36 | **Java**: 21+ | **App version**: 1.0.0.17 (versionCode 17)

---

## Architecture

### Offline-First with Store5

- **Pattern**: Store5 for offline-first data management across all feature modules
- **Layers**: Presentation (Compose/MVI) → Store5 (cache) → Repository → Room DB + Ktor API
- **Sync**: Database-first writes, background server sync, incremental pull with timestamp tracking

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

- **ViewModels**: 52+ ViewModels using `koinInject` / `koinViewModel`
- **State**: `StateFlow<UiState>` + `SharedFlow<UiEvent>` in each ViewModel
- **Resource**: `Resource<T>` wrapper for Loading/Success/Error states

### Dependency Injection with Koin

Each feature has a Koin module hierarchy:

```
{feature}PlatformModule.android.kt   ← Database factory (factory{})
{feature}PlatformModule.desktop.kt
{feature}PlatformModule.ios.kt
{feature}Module.kt (commonMain)      ← DAOs, Repos, Stores, ViewModels
```

**CRITICAL**: All workspace-aware layers must use `factory {}` not `single {}`. See Workspace-Scoped Database section.

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

When users switch workspaces, all databases must be replaced. Using Koin `single {}` caches stale database references.

### The Rule: factory for Every Workspace-Aware Layer

```
Database (factory) → DAOs (factory) → Repositories (factory) → Stores (factory)
→ ViewModels (viewModel/viewModelOf — already correct)
```

**Exceptions** — these remain `single {}`:
- `AuthRoomDatabase` — exists before workspace selection
- `WorkspaceRoomDatabase` — stores the workspace list itself

### Implementation

```kotlin
// ❌ WRONG
val myPlatformModule = module {
    single<MyDatabase> { factory.createDatabase(...) }  // Stale after switch!
}

// ✅ CORRECT
val myPlatformModule = module {
    factory<MyDatabase> { factory.createDatabase(...) }
}
```

### Database Path Structures

- **Android**: `workspace_{slug}_{module}.db` (flat file)
- **iOS/Desktop**: `workspace_{slug}/{module}.db` (directory + file)

### DatabaseScopeManager

`data/common/src/commonMain/kotlin/.../DatabaseScopeManager.kt` — centralized singleton managing database lifecycle. Caches by `{workspaceSlug}:{moduleName}` key. Always cleared on workspace switch.

### Verification Checklist for New Modules

- [ ] Database: `factory` in platform module
- [ ] DAOs: `factory` in common module
- [ ] Repositories: `factory` in common module
- [ ] Stores: `factory` in common module
- [ ] ViewModels: `viewModel` or `viewModelOf`
- [ ] Path parsing handles platform-specific format
- [ ] `DatabaseScopeManager` integration in platform factory

---

## Offline-First Data Architecture

### Core Principles

**1. Database-First Writes**
All CRUD saves to Room first (`synced = false`), then syncs to server async. If sync fails, data remains locally.

**2. Client-Side UID Generation**
`UidGenerator.generateUid(prefix)` → `{PREFIX}{YYYYMMDDHHMMSS}{RANDOM}` (32 chars). Generate in ViewModel, never in repository.

**3. Incremental Sync with String Timestamps**
ISO 8601 strings (`yyyy-MM-ddTHH:mm:ss`) for natural string comparison. Server's `updatedAt` is authoritative for sync tracking.

**4. Paginated Batch Sync**
Default 100 records/batch, max 10,000/sync cycle. Loop protection with `hasNext` guard.

### Repository Create Pattern

```kotlin
suspend fun createEntity(entity: Entity): Result<Entity> {
    require(entity.uid.isNotBlank()) { "UID must be set by ViewModel" }
    dao.insertEntity(entity.toEntity().copy(synced = false))
    return try {
        val serverEntity = api.createEntity(entity)
        val resolved = if (serverEntity.uid != entity.uid) serverEntity.copy(uid = entity.uid) else serverEntity
        dao.insertEntity(resolved.toEntity().copy(synced = true))
        Result.success(resolved)
    } catch (e: Exception) {
        Result.success(entity)  // Graceful fallback — already saved locally
    }
}
```

### Conflict Resolution

- Unsynced local changes always win over server data during pull sync
- Server UID conflicts: correct server response to maintain local UID
- Last-write-wins (server timestamp) for synced entities

### Store5 Integration

Keep sync logic in Repository, not Store5 fetcher. Clear Store5 cache after successful sync.

```kotlin
val entityStore = StoreBuilder.from(
    fetcher = Fetcher.of { key -> repository.observeEntities().first() },
    sourceOfTruth = SourceOfTruth.of(
        reader = { key -> repository.observeEntities() },
        writer = { _, _ -> }  // Writes handled by repository
    )
).build()
```

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
- **Usage**: `val themeManager: ThemeManager = koinInject()`
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
- **Koin Init**: Must be initialized in `MainViewController` before app launch
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
| Stale data after workspace switch | Check entire DI chain uses `factory`, not `single` |
| iOS database path wrong | Use `getIosDatabasePath()`, not hardcoded paths |
| `Dispatchers.IO` crash in `iosMain` | Use `Dispatchers.Default` — the Native actual is internal; `Dispatchers.IO` only works in `commonMain` |
| `java.*` compile error in commonMain | Use KMP equivalent (kotlinx.datetime, etc.) |
| Store5 stale cache | Clear cache explicitly after successful sync |
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
- **factory vs single**: Workspace-aware = factory; cross-workspace singletons = single