# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Ampairs Mobile Application** is a Kotlin Multiplatform business management client that integrates with the Ampairs Spring Boot backend system. It targets **Android, iOS, and Desktop (JVM)** platforms using **Compose Multiplatform** with an **offline-first architecture**.

### **🏗️ System Integration**

This mobile app is part of a **three-tier Ampairs ecosystem**:

1. **Backend (Spring Boot + Kotlin)** - `/ampairs_service` + domain modules  
2. **Web Frontend (Angular + Material Design 3)** - `/ampairs-web`
3. **Mobile App (Kotlin Multiplatform)** - `/ampairs-mp-app` ← **THIS PROJECT**

**Backend Integration**: Consumes REST APIs from Spring Boot backend with JWT authentication, multi-tenant support, and offline-first synchronization.

## Architecture

### **🔄 Offline-First Architecture with Store5**

- **Pattern**: Store5 for robust offline-first data management
- **Layers**: Presentation (Compose/MVI) → Store5 (Fetcher/SourceOfTruth) → Data (Room/Ktor)
- **Integration**: Spring Boot backend with JWT auth and multi-tenancy

### **Technology Stack**

- **UI Framework**: Jetpack Compose Multiplatform with Material 3 Design System
- **Theme Management**: Reactive theme switching with Light/Dark/System modes (default: System)
- **Dependency Injection**: Koin with modular setup per feature
- **Local Database**: Room Database (replaces SQLDelight) with platform-specific drivers
- **Offline-First**: Store5 for caching, synchronization, and conflict resolution
- **HTTP Client**: Ktor with automatic JWT token refresh and bearer authentication
- **Navigation**: Androidx Navigation Compose with type-safe routing
- **State Management**: MVI pattern with ViewModels and Resource<T> wrappers
- **Image Loading**: Custom image loader with caching support
- **Serialization**: kotlinx.serialization for JSON parsing
- **Background Sync**: Platform-specific background task scheduling

### **Platform Support**

- **Android**: Native Android app with Room SQLite, background sync via WorkManager
- **iOS**: Native iOS app with Core Data integration, background refresh capabilities
- **Desktop (JVM)**: Desktop application with JDBC drivers, timer-based sync coordination

### **Module Structure**

- **Main Module**: `composeApp/` with `commonMain/`, `androidMain/`, `iosMain/`, `desktopMain/`
- **Domain Modules**: auth, workspace, customer, product, order, invoice, tally
- **Support**: core/, common/, shared/

## **🔑 Key Development Patterns**

### **Store5 Implementation Pattern**
- **Pattern**: Each domain module implements Store5 with Fetcher (Network) + SourceOfTruth (Room DB)
- **Usage**: `store.stream(StoreReadRequest.cached(key, refresh = false))`

### **Room Database Architecture**
- **Platform-specific**: Room (Android), Core Data bridge (iOS), JDBC (Desktop)
- **Multi-tenant**: All entities include `tenant_id` for data segregation
- **Sync metadata**: Entities track `syncStatus`, timestamps for offline-first

### **Authentication & Multi-tenancy**
- **JWT Auth**: Phone + OTP flow with device_id for multi-device support
- **Tenant Context**: HTTP headers include workspace/company context
- **Token Storage**: Secure storage via Room database with encryption

### **API Integration with Backend**
- **Endpoints**: Follow backend REST patterns (`/api/v1/{resource}`)
- **Headers**: Include `X-Workspace-ID` for multi-tenant context
- **Responses**: Use backend `ApiResponse<T>` wrapper format

### **Dependency Injection with Koin**
- **Modular**: Feature-based modules per domain
- **Platform-specific**: Separate Android/iOS/Desktop implementations
- **ViewModel pattern**: `koinInject { parametersOf(id) }`
- **Layers**: API → Repository → Store5 → ViewModel

### **Navigation with Type Safety**
- **Routes**: `@Serializable sealed interface Route` with data classes
- **Usage**: `navController.navigate(Route.CustomerDetails(customerId))`
- **Type safety**: `backStackEntry.toRoute<Route.CustomerDetails>()`

## **Build Commands**

### **Android**
```bash
./gradlew composeApp:assembleDebug
./gradlew composeApp:installDebug
```

### **Desktop**
```bash
./gradlew composeApp:run                    # Run desktop app
./gradlew composeApp:package               # Create native distributions
```

### **iOS** (Currently commented out)
```bash
./gradlew composeApp:embedAndSignAppleFrameworkForXcode
```

### **Cleanup**
```bash
./gradlew clean
./cleanup.sh  # Remove IDE files and build artifacts
```

## **Version Catalog**
**Key Dependencies** (`gradle/libs.versions.toml`):
- Kotlin 2.2.0, Compose Multiplatform 1.8.2, Room 2.7.0-alpha11
- Store5 5.1.0, Ktor 3.2.1, Koin 4.1.0, Kotlinx Serialization 1.7.3

## **Development Environment**
- **IDE**: Android Studio with KMP plugin, `kdoctor` for validation
- **Requirements**: Android Min SDK 24/Target 35, Java 21+, Xcode 15+ (iOS)

## **Business Domain Integration**

The app provides complete feature parity with the backend system:

### **Core Features**
- **Authentication**: Phone/OTP login with JWT tokens and multi-device support
- **Workspace Management**: Multi-tenant workspace selection and configuration  
- **Customer Management**: Comprehensive CRM with address handling and GST compliance
- **Product Catalog**: Product management with categories, tax codes, and image storage
- **Inventory Management**: Stock tracking, movement reporting, and low-stock alerts
- **Order Processing**: Order creation, management, status workflows, and pricing
- **Invoice Generation**: Invoice creation, GST compliance, PDF generation, email delivery
- **Tally Integration**: ERP system synchronization and data exchange

### **Data Flow & Synchronization**
- **Offline-first**: All CRUD operations work offline with auto-sync
- **Conflict Resolution**: Store5 last-write-wins strategy
- **Background Sync**: Platform-specific tasks for data consistency
- **Real-time**: WebSocket connections for live updates when online

## **Platform-Specific Implementations**
- **Android**: Room SQLite, WorkManager sync, local notifications
- **iOS**: Room Core Data bridge, Background App Refresh, push notifications
- **Desktop**: Room JDBC drivers, timer-based sync, native window controls

## **Recent Updates & Migration Notes**
- **Database**: ✅ Migrated SQLDelight → Room with Store5 integration
- **Architecture**: ✅ Store5 offline-first with conflict resolution
- **ViewModels**: ✅ Koin injection with `koinInject { parametersOf() }`

## **Development Guidelines**
- **Naming**: `com.ampairs.{domain}.{layer}`, `@SerialName` for snake_case API compatibility
- **Backend**: Follow REST patterns (`/api/v1/{resource}`), use `ApiResponse<T>` wrapper
- **Quality**: Offline-first design, graceful error recovery, lazy loading

## **Theme Management System**

The app includes a comprehensive theme switching system implemented in January 2025:

### **Architecture**
- **Options**: `ThemePreference.SYSTEM/LIGHT/DARK` (default: LIGHT)
- **Manager**: `ThemeManager` with `StateFlow<ThemePreference>` and `@Composable isDarkTheme()`

### **Implementation Files**
- **Core**: `ThemePreference.kt`, `ThemeManager.kt`, `ThemeRepository.kt`
- **UI**: `AppHeader.kt` with `ThemeToggleButton`
- **Integration**: `App.kt` uses `ThemeManager.isDarkTheme()`

### **Features**
- **UI**: Prominent placement beside user menu with instant reactive updates
- **Integration**: System theme respect, Material 3 color schemes, cross-platform

### **User Experience**
- **Default**: Light theme (changed from System)
- **Access**: Theme icon in header with System/Light/Dark options
- **Performance**: Instant switching with visual feedback

### **Usage in Code**
- **Injection**: `val themeManager: ThemeManager = koinInject()`
- **Theme**: `PlatformAmpairsTheme(darkTheme = themeManager.isDarkTheme())`
- **Set**: `themeManager.setThemePreference(ThemePreference.DARK)`

## **DataStore Configuration & Key-Value Storage**

### **📦 Existing DataStore Implementation (January 2025)**

**IMPORTANT**: The app has a **fully configured DataStore Preferences system** for cross-platform key-value storage. **Always reuse this existing setup** for any new persistence needs.

#### **Key Files & Structure**
- **Common Factory**: `/composeApp/src/commonMain/kotlin/com/ampairs/common/theme/createThemeDataStore.kt`
- **Platform Implementations**: `createThemeDataStore.android.kt`, `createThemeDataStore.desktop.kt`, `createThemeDataStore.ios.kt`
- **Repository Pattern**: `/composeApp/src/commonMain/kotlin/com/ampairs/common/theme/ThemeRepository.kt`
- **Koin Modules**: `androidAppConfigModule`, `desktopAppConfigModule`, `iosAppConfigModule`

#### **Storage Locations**
- **Android**: `context.filesDir/theme_preferences.preferences_pb`
- **Desktop**: `~/.ampairs/theme_preferences.preferences_pb`
- **iOS**: `Documents/theme_preferences.preferences_pb` (requires `@OptIn(ExperimentalForeignApi::class)`)

#### **Integration Pattern**
- All platform Koin modules include their respective app config modules via `includes()`
- DataStore injected as `DataStore<Preferences>` singleton
- Repository pattern with Flow-based reactive updates
- StateFlow conversion via `stateIn()` for Compose integration

#### **Usage Guidelines**
- ✅ **DO**: Inject existing `DataStore<Preferences>` and add new preference keys
- ❌ **DON'T**: Create separate DataStore instances
- **Pattern**: Repository → Manager → UI (with proper error handling and defaults)

## **iOS Target Development**

### **📱 iOS Implementation Status (January 2025)**

**Status**: ✅ **Fully implemented and production-ready**

#### **Key iOS Configurations**
- **Dispatchers**: iOS uses `Dispatchers.Default` for IO operations (no IO dispatcher)
- **Database**: Room with iOS Documents directory paths via `getIosDatabasePath()`
- **Platform APIs**: UIKit integration with `@OptIn(ExperimentalForeignApi::class)` for Foundation APIs
- **Koin**: Proper initialization in `MainViewController` before app launch
- **Navigation**: Side drawer pattern (no hardware back button)

#### **iOS-Specific Requirements**
- **File Paths**: Always use Documents directory for writable storage
- **Time Handling**: Use `kotlin.time.Clock` for cross-platform compatibility
- **Compilation**: `compileKotlinIosSimulatorArm64` for testing
- **Threading**: iOS-specific `synchronized()` and `Volatile` implementations


## **Common Issues & Solutions**

- **iOS**: Use `getIosDatabasePath()`, initialize Koin in `MainViewController`, `DispatcherProvider.io` instead of `Dispatchers.IO`
- **Room**: Check migration scripts when updating schemas
- **Store5**: Timestamp-based conflict resolution for concurrent modifications
- **Network**: Ktor client with proper timeout and retry policies

## **Integration with Backend**

- **Domain Models**: Identical patterns with Spring Boot backend
- **API Contracts**: Exact REST endpoint compatibility with JWT auth and multi-tenancy
- **Feature Parity**: Consistent across web, mobile, and API clients

*Refer to main `/ampairs/CLAUDE.md` for backend guidelines.*

## **🔗 Dynamic Module Navigation System (January 2025)**

### **📋 Overview**
The app implements a sophisticated dynamic module navigation system that integrates backend-installed modules with local navigation implementations, providing seamless module access with proper fallback handling.

### **🏗️ Architecture Components**

#### **Module Registry System**
- **File**: `com/ampairs/workspace/navigation/ModuleRegistry.kt`
- **Purpose**: Central registry mapping module codes to local navigation routes
- **Interface**: `IModuleNavigationProvider` for extensible module registration
- **Features**: Type-safe navigation, dynamic discovery, fallback handling

#### **Module Navigation Providers**
- **File**: `com/ampairs/workspace/navigation/ModuleProviders.kt`
- **Implementations**:
  - `CustomerModuleProvider`: "customer-management" → `Route.Customer`
  - `ProductModuleProvider`: "product-management" → `Route.Product`
  - `OrderModuleProvider`: "order-management" → `Route.Order`
  - `InvoiceModuleProvider`: "invoice-management" → `Route.Invoice`

#### **Enhanced WorkspaceModulesScreen**
- **File**: `com/ampairs/workspace/ui/WorkspaceModulesScreen.kt`
- **Features**:
  - Direct module navigation via registry lookup
  - "Update App" dialog for missing implementations
  - Backward compatibility with existing callback system

#### **DynamicModuleNavigationService Integration**
- **File**: `com/ampairs/workspace/navigation/DynamicModuleNavigationService.kt`
- **Enhancements**:
  - Filters installed modules by local availability
  - Separate tracking of available vs unavailable modules
  - Integration with module implementation detection

### **🚀 Navigation Flow**
```
User clicks module card
    ↓
tryNavigateToModule() checks registry
    ↓
If available: Navigate to Route.{Module}
    ↓
If unavailable: Show "Update App" dialog
    ↓
Fallback: Use original onModuleSelected callback
```

### **🗄️ Workspace Context & Database Isolation**

#### **Unified Context Management**
- **Issue Fixed**: Database paths using "workspace_default" instead of actual slug
- **Root Cause**: Two separate context systems (business vs database) not synchronized
- **Solution**: Enhanced `WorkspaceContextIntegration.setWorkspaceFromDomain()`

#### **Context Integration**
- **Business Context**: `WorkspaceContextManager` for app state
- **Database Context**: `WorkspaceContext` for database paths
- **Unified Setup**: Both contexts set simultaneously on workspace selection
- **Result**: Proper isolation with `workspace_{actual-slug}/module.db` paths

### **📱 Module Code Mappings**
```kotlin
// Local implementations available
"customer-management" → Route.Customer
"product-management" → Route.Product
"order-management" → Route.Order
"invoice-management" → Route.Invoice

// Shows "Update App" dialog
"inventory-management" → Not locally implemented
```

### **🛠️ Usage Patterns**

#### **Adding New Module Support**
1. Create navigation provider in `ModuleProviders.kt`
2. Register in `ModuleRegistry.initialize()`
3. Update availability check in `DynamicModuleNavigationService`
4. Module automatically appears in navigation

#### **Integration Guidelines**
- **Module Discovery**: Automatic backend-driven module availability
- **Local Implementation**: Registry-based route resolution
- **Fallback Strategy**: Graceful degradation for missing modules
- **Type Safety**: Compile-time route validation

### **✅ Production Status**
- **Module Integration**: ✅ Complete and tested
- **Workspace Context**: ✅ Unified and isolated
- **Navigation Flow**: ✅ Type-safe with fallbacks
- **Database Isolation**: ✅ Proper workspace segregation
- **Backward Compatibility**: ✅ No breaking changes

This system provides a robust foundation for dynamic module loading while maintaining type safety and graceful degradation for missing implementations.

## **🧠 KMP Platform Compatibility Guidelines**

### **❌ Common Platform-Specific Mistakes to Avoid**

**CRITICAL**: Always use KMP-compatible APIs in `commonMain`. Platform-specific code should only exist in platform source sets (`androidMain`, `iosMain`, `desktopMain`) using expect/actual pattern.

#### **1. Time/Date APIs**
- ❌ `System.currentTimeMillis()` (JVM-specific)
- ✅ `Clock.System.now().toEpochMilliseconds()` (KMP-compatible)
- ❌ `Date()`, `Calendar`, `LocalDateTime` (Java-specific)
- ✅ `kotlinx.datetime.*` (KMP datetime library)

#### **2. String Formatting**
- ❌ `String.format()` (JVM-specific)
- ✅ String interpolation: `"Value: $value"` or manual formatting
- ❌ `DecimalFormat`, `NumberFormat` (Java-specific)
- ✅ Platform-specific expect/actual for complex formatting

#### **3. Threading/Concurrency**
- ❌ `Thread`, `synchronized` blocks (JVM-specific)
- ✅ `kotlinx.coroutines.*`, `@Volatile` annotation
- ❌ `System.getProperty()`, `Runtime.getRuntime()`
- ✅ Platform-specific expect/actual implementations

#### **4. File System APIs**
- ❌ `java.io.File`, `java.nio.*` (JVM-specific)
- ✅ Platform-specific expect/actual for file operations
- ❌ Hard-coded file paths like `/tmp/`, `C:\`
- ✅ Platform-specific directory resolution

#### **5. Collections & Utilities**
- ❌ `java.util.*` specific implementations
- ✅ Kotlin standard library collections
- ❌ `UUID.randomUUID()` (JVM-specific)
- ✅ KMP UUID libraries or expect/actual implementations

#### **6. Logging & Debugging**
- ❌ `System.out.println()`, `e.printStackTrace()` (JVM-specific)
- ✅ Platform-specific logging or expect/actual pattern
- ❌ `Log.d()` (Android-specific)
- ✅ Logging libraries with KMP support

### **✅ KMP-First Development Approach**

#### **Development Checklist**
1. **Always check if API is available in `commonMain`**
2. **Use kotlinx libraries for cross-platform functionality**
   - `kotlinx.datetime` for time/date operations
   - `kotlinx.coroutines` for concurrency and async operations
   - `kotlinx.serialization` for JSON and data serialization
   - `kotlinx.collections.immutable` for immutable collections
3. **Prefer expect/actual pattern for platform-specific needs**
4. **Test compilation on multiple targets early and often**
5. **Use KMP-compatible dependencies in version catalog**

#### **Quick Validation Pattern**
```kotlin
// ❌ Wrong - Platform-specific import in commonMain
import java.util.Date
import android.util.Log
import java.io.File

// ✅ Correct - KMP-compatible imports
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
```

#### **Compilation Test Strategy**
- If importing `java.*` or `android.*` in `commonMain` → ❌ Wrong
- If using platform-specific APIs without expect/actual → ❌ Wrong
- If compile fails on iOS/Desktop targets → ❌ Platform-specific code leak
- Always run: `./gradlew compileDebugKotlinAndroid compileKotlinIosSimulatorArm64 compileKotlinDesktop`

#### **Store5 & Room Integration**
- ✅ Use `Fetcher.ofFlow` for reactive data sources
- ✅ Use `Clock.System.now()` for timestamps in entities
- ✅ Use `kotlinx.coroutines.flow.Flow` for reactive streams
- ❌ Avoid `Fetcher.ofSuspending` with platform-specific suspend functions

### **🔍 Error Prevention Patterns**

#### **Before Writing Code**
1. **Check target compatibility**: Will this API work on iOS/Desktop?
2. **Prefer Kotlin stdlib**: Use Kotlin's built-in functions over platform-specific ones
3. **Use version catalog**: Ensure dependencies support KMP
4. **Think expect/actual**: If platform-specific, design the common interface first

#### **During Development**
1. **Compile frequently**: Test all targets during development, not just at the end
2. **Use KMP libraries**: Prefer libraries specifically designed for KMP
3. **Avoid shortcuts**: Don't use JVM-specific APIs for "quick" implementations

#### **Code Review Checklist**
- No platform-specific imports in `commonMain`
- All time/date operations use `kotlinx.datetime`
- All async operations use `kotlinx.coroutines`
- File operations use expect/actual pattern
- String operations avoid Java-specific formatting

**Remember**: The goal is to write "KMP-first" code that naturally works across all platforms, rather than "JVM-first" code that needs platform-specific workarounds.

## **📝 Form UI Standards (January 2025)**

### **Keyboard Navigation Requirements**
- **Focus Management**: Use `LocalFocusManager.current` with proper `KeyboardActions`
- **Field Navigation**: `ImeAction.Next` for fields, `ImeAction.Done` for last field
- **Single Line**: Use `singleLine = true` to ensure Enter moves to next field
- **Save Access**: Include keyboard-accessible save button at bottom of form

### **TopAppBar Guidelines**
- Remove redundant `navigationIcon` when global navigation exists
- Remove `onNavigateBack` parameters from form screens
- Use `AppScreenWithHeader` pattern consistently across all navigation files

### **Forms Updated**
- All customer, product, and tax form screens follow these patterns
- Details screens also cleaned of redundant back buttons

## **🔄 Offline-First Data Management Architecture (September 2025)**

### **📋 Overview**
The app implements a comprehensive offline-first architecture using Store5, Room database, and sophisticated conflict resolution to handle enterprise-scale datasets (10K+ records) with seamless online/offline transitions.

### **🔑 Core Principles**

#### **1. Database-First Operations**
- **Pattern**: All CRUD operations save to local Room database first with `synced = false`
- **Benefit**: Immediate UI response and guaranteed data persistence
- **Background Sync**: Server operations happen asynchronously after local save
- **Fallback**: If server sync fails, data remains locally with sync retry capability

#### **2. Client-Side UID Generation**
- **System**: `UidGenerator.generateUid(prefix)` creates deterministic UIDs locally
- **Format**: `{PREFIX}{YYYYMMDDHHMMSS}{RANDOM}` (32 chars total, e.g., `CUS20250923193834J94YKJREVXB7SA1`)
- **Consistency**: Same UID used throughout create → sync → update lifecycle
- **Conflict Prevention**: Server UID mismatches are corrected to maintain local UID consistency

#### **3. String-Based Timestamp Sync**
- **Method**: ISO 8601 timestamps (`yyyy-mm-ddTHH:mm:ss`) with natural string comparison
- **Efficiency**: Avoids complex millisecond parsing and timezone issues
- **Server Authority**: Uses server's `updatedAt` timestamps for authoritative sync tracking
- **Incremental**: Only syncs records modified after `last_sync` timestamp

#### **4. Paginated Batch Synchronization**
- **Batch Size**: Configurable batches (default: 100 records per request)
- **Memory Efficient**: Processes large datasets without memory overload
- **Progress Tracking**: Real-time sync progress with console logging
- **Safety Limits**: Maximum 10,000 records per sync with infinite loop protection
- **Resume Capability**: Handles network interruptions gracefully

### **🛠️ Implementation Patterns**

#### **Repository Layer Pattern**
```kotlin
suspend fun createEntity(entity: Entity): Result<Entity> {
    // 1. Client-side UID generation (if not set)
    require(entity.uid.isNotBlank()) { "UID must be set by ViewModel" }

    // 2. Database-first save with unsynced status
    val unsyncedEntity = entity.toEntity().copy(synced = false)
    dao.insertEntity(unsyncedEntity)

    // 3. Background server sync
    try {
        val serverEntity = api.createEntity(entity)
        // 4. UID conflict resolution
        if (serverEntity.uid != entity.uid) {
            val corrected = serverEntity.copy(uid = entity.uid)
            dao.insertEntity(corrected.toEntity().copy(synced = true))
            return Result.success(corrected)
        }
        // 5. Mark as synced
        dao.insertEntity(serverEntity.toEntity().copy(synced = true))
        return Result.success(serverEntity)
    } catch (e: Exception) {
        // 6. Graceful fallback - data already saved locally
        return Result.success(entity)
    }
}
```

#### **Batch Sync Pattern**
```kotlin
private suspend fun syncEntitiesFromServerInBatches(batchSize: Int = 100): Result<Int> {
    val lastSync = getLastSyncTime() // ISO 8601 string
    var totalSynced = 0
    var currentPage = 0

    do {
        val pageResponse = api.getEntities(lastSync, currentPage, batchSize, "updatedAt", "ASC")
        val batchEntities = pageResponse.content

        // Process batch with conflict resolution
        val entities = batchEntities.mapNotNull { serverEntity ->
            val existing = dao.getEntityById(serverEntity.uid)
            if (existing != null && !existing.synced) {
                // Skip server entity to preserve local changes
                null
            } else {
                serverEntity.toEntity().copy(synced = true)
            }
        }
        dao.insertEntities(entities)

        totalSynced += entities.size
        currentPage++
    } while (pageResponse.hasNext && totalSynced < 10000)

    // Update sync timestamp using server's max updatedAt
    val maxServerTime = getMaxUpdatedAtFromServerEntities(allBatchEntities)
    if (maxServerTime.isNotBlank()) {
        appPreferences.setLastSyncTime(maxServerTime)
    }

    return Result.success(totalSynced)
}
```

#### **Store5 Integration Pattern**
```kotlin
val entityListStore: Store<EntityListKey, List<EntityListItem>> = StoreBuilder
    .from(
        fetcher = Fetcher.of { key ->
            // Only read from local database - sync handled separately
            if (key.searchQuery.isBlank()) {
                repository.observeEntities().first()
            } else {
                repository.searchEntities(key.searchQuery).first()
            }
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key -> repository.observeEntities() },
            writer = { _, _ -> /* Writing handled through repository */ }
        )
    ).build()
```

### **⚡ Conflict Resolution Strategies**

#### **1. Local-First Priority**
- **Unsynced Local Changes**: Always preserved over server data
- **Server UID Conflicts**: Server response corrected to maintain local UID
- **Sync Order**: Local changes pushed first, then server data pulled

#### **2. Automatic Conflict Resolution**
- **Last-Write-Wins**: Server timestamp determines final state for synced entities
- **UID Consistency**: Client-generated UIDs maintained throughout lifecycle
- **Data Loss Prevention**: No local unsynced data overwritten by server sync

#### **3. Failure Recovery**
- **Retry Mechanism**: Failed syncs marked for retry in next sync cycle
- **Graceful Degradation**: App continues functioning with local data during network issues
- **Progressive Sync**: Successful entities marked as synced, failed entities remain unsynced

### **📊 Performance Characteristics**

#### **Memory Management**
- **Batch Processing**: 100-entity batches prevent memory overflow with large datasets
- **Lazy Loading**: Store5 provides efficient lazy loading with caching
- **Background Operations**: Heavy sync operations don't block UI thread

#### **Network Efficiency**
- **Incremental Sync**: Only downloads entities modified since last sync
- **Pagination**: Reduces payload size and enables resumable transfers
- **Compression**: Standard HTTP compression for large batch transfers

#### **Database Optimization**
- **Indexed Queries**: Primary key and timestamp-based queries for fast lookups
- **Batch Inserts**: Multiple entities inserted in single transaction
- **Sync Status Tracking**: Efficient queries for unsynced entities

### **🔧 Configuration & Scaling**

#### **Configurable Parameters**
- **Batch Size**: Adjustable per entity type (default: 100)
- **Sync Frequency**: Auto-sync on screen entry or manual trigger
- **Safety Limits**: Maximum entities per sync (default: 10,000)
- **Retry Logic**: Exponential backoff for failed sync attempts

#### **Enterprise Scale Support**
- **10K+ Records**: Tested with large customer datasets
- **Concurrent Users**: Multiple device sync with conflict resolution
- **Background Processing**: Sync continues in background on mobile platforms
- **Progress Feedback**: Real-time sync progress with user visibility

### **🚨 Critical Implementation Notes**

#### **UID Generation Requirements**
- **ALWAYS**: Generate UIDs in ViewModel layer before repository calls
- **NEVER**: Allow repository to generate fallback UIDs
- **PATTERN**: Use `UidGenerator.generateUid(Constants.UID_PREFIX)` consistently

#### **Sync Timing Considerations**
- **Database First**: Save locally before any network operations
- **Sync Order**: Push local changes before pulling server updates
- **Timestamp Authority**: Use server timestamps for sync tracking

#### **Store5 Best Practices**
- **Separate Concerns**: Keep sync logic in repository, not Store5 fetcher
- **Cache Management**: Clear Store5 cache after successful sync operations
- **Error Handling**: Handle Store5 errors separately from sync errors

This architecture provides enterprise-grade offline capabilities while maintaining excellent user experience and data consistency across all platforms.

## **🔄 Workspace-Scoped Database Management (October 2025)**

### **📋 Overview**

The app implements comprehensive workspace-scoped database management to ensure proper data isolation when switching between workspaces. Each workspace maintains its own isolated database instances that are properly created, cached, and cleaned up during workspace transitions.

### **🎯 Key Concepts**

#### **Database Scope Management**
- **DatabaseScopeManager**: Centralized singleton that manages database lifecycle per workspace
- **Caching Strategy**: Databases cached by `{workspaceSlug}:{moduleName}` key
- **Lifecycle**: Databases created on-demand, cached during use, closed on workspace switch

#### **Koin Dependency Injection Pattern**
- **CRITICAL**: All workspace-aware components must use `factory` instead of `single`
- **Affected Layers**: Database → DAOs → Repositories → Stores
- **Reason**: `single` retains old references even after workspace switch
- **ViewModels**: Already use `viewModel`/`viewModelOf` which creates per-navigation instances

#### **Platform-Specific Path Structures**
- **Android**: `workspace_{slug}_{module}.db` (single file)
- **iOS/Desktop**: `workspace_{slug}/customer.db` (directory structure)
- **Parsing**: Path extraction logic differs per platform

### **⚠️ Critical Rules**

#### **1. Koin Module Definitions**

**❌ WRONG (Causes stale data):**
```kotlin
val customerPlatformModule = module {
    single<CustomerDatabase> {  // ❌ Singleton caches old database
        factory.createDatabase(...)
    }
}
```

**✅ CORRECT:**
```kotlin
val customerPlatformModule = module {
    factory<CustomerDatabase> {  // ✅ Fresh instance on each request
        factory.createDatabase(...)
    }
}
```

#### **2. Complete Dependency Chain**

All layers must use `factory` for workspace-aware components:

```
Database (factory)
    ↓
DAOs (factory)
    ↓
Repositories (factory)
    ↓
Stores (factory)
    ↓
ViewModels (viewModel/viewModelOf - already correct)
```

#### **3. Non-Workspace Databases**

Some databases should remain as `single`:
- **AuthRoomDatabase**: Login happens before workspace selection
- **WorkspaceRoomDatabase**: Stores the workspace list itself

### **🔍 Debugging Workspace Switching**

The implementation includes comprehensive logging to trace database lifecycle:

**Expected Log Flow:**
```
1. Workspace Switch:
   WorkspaceListScreen: 🔄 Switching to workspace: Store B
   DatabaseScopeManager: 🧹 Clearing databases for workspace: store-a
   DatabaseScopeManager: Keys to remove: [store-a:customer, store-a:product]
   DatabaseScopeManager: Cache after clear: []

2. Module Navigation:
   [Platform]DatabaseFactory: Creating database for module=customer, workspace=store-b
   DatabaseScopeManager: 🆕 Creating NEW database for key: store-b:customer
```

**Problem Indicators:**
- `✅ Returning cached database` with wrong workspace slug
- Missing "Creating NEW database" log after workspace switch
- Database not appearing in "Keys to remove" list

### **🛠️ Common Issues & Solutions**

#### **Issue: Stale Data After Workspace Switch**

**Symptoms:**
- Database created with correct workspace but shows old data
- Logs show cached database being returned

**Root Causes:**
1. Platform module still uses `single` instead of `factory`
2. DAO/Repository/Store layer uses `single`
3. ViewModel retained by navigation backstack

**Fix:**
1. Change all workspace-aware Koin definitions to `factory`
2. Verify entire dependency chain uses `factory`
3. Ensure ViewModels use `viewModel`/`viewModelOf`

#### **Issue: Wrong Module Name in Logs**

**Symptoms:**
- `Creating database for module=unknown`
- Path parsing extracting incorrect module name

**Root Cause:**
- Path parsing logic doesn't match actual path structure

**Fix:**
- Android: Parse `workspace_{slug}_{module}.db` format
- iOS/Desktop: Parse `workspace_{slug}/module.db` directory format

### **📦 Files Involved**

**Core Components:**
- `DatabaseScopeManager.kt` - Central database lifecycle management
- `CoroutineExceptionHandling.kt` - Cancellation exception filtering

**Platform Factories:**
- `AndroidDatabaseFactory.kt`
- `WorkspaceAwareDatabaseFactory.desktop.kt`
- `WorkspaceAwareDatabaseFactory.ios.kt`

**Koin Modules (All must use `factory`):**
- `CustomerPlatformModule.{platform}.kt`
- `ProductModule.{platform}.kt`
- `TaxModule.{platform}.kt`
- `TallyModule.{platform}.kt`
- Common: `CustomerModule.kt` (DAOs, Repositories, Stores)

### **✅ Verification Checklist**

When implementing new workspace-aware modules:

- [ ] Database defined as `factory` in platform module
- [ ] DAOs defined as `factory` in common module
- [ ] Repositories defined as `factory` in common module
- [ ] Stores defined as `factory` in common module
- [ ] ViewModels use `viewModel` or `viewModelOf`
- [ ] Path parsing handles platform-specific structure
- [ ] DatabaseScopeManager integration in platform factory

**Reference Commit**: `a0db3e7` - Complete workspace-scoped database implementation (October 2025)

## **🔧 Backend DTO Alignment & API Integration Patterns (January 2025)**

### **📋 DTO Migration Best Practices**

When aligning mobile DTOs with backend changes, follow this systematic approach:

#### **Migration Order**
`Backend Analysis → Domain Models → Entities → Repositories → ViewModels → UI Components`

**Critical**: Fix import issues before logic issues, compile frequently.

#### **Reference Files for Patterns**
- **Field Additions**: See `CustomerGroup.kt` and `CustomerType.kt` for @SerialName patterns
- **Entity Updates**: See `CustomerGroupEntity.kt` for Room entity field additions
- **API Integration**: See `CustomerGroupApiImpl.kt` for correct URL building and response handling

### **🎯 Project-Specific Conventions**

#### **Import Paths (Check These First)**
- `com.ampairs.common.id_generator.UidGenerator` (not `.util.UidGenerator`)
- `com.ampairs.common.model.Response` (not `.core.domain.dto.ApiResponse`)

#### **API Patterns**
- **URL Building**: Use `ApiUrlBuilder.customerUrl("v1/groups")` pattern
- **Response Handling**: Check `response.data != null && response.error == null`
- **Logger Usage**: `CustomerLogger.w("TagName", "message", exception)` signature

#### **Form Architecture**
- **Dynamic Data**: Use separate ID + display name fields in form states
- **Reference**: See `CustomerFormViewModel.kt` for string-based customer type handling
- **UI Dropdowns**: Load dynamic data from repositories, not hardcoded enums

### **🧹 "Master" Data Pattern**

When user says "there is no MasterCustomerX":
1. Remove classes entirely from domain models
2. Update API interfaces to remove getMaster* methods
3. Update repositories to use base types instead of Master types
4. Clean up ViewModels and UI references

**Reference Files**: `CustomerTypeRepository.kt`, `CustomerGroupRepository.kt`

### **⚠️ Common Pitfalls**

#### **Response Handling**
- `Response<T>.data` is nullable - always null check first
- No `.success` property exists

#### **Logger Methods**
- Use `w`, `e`, `i`, `d` method names (not `warn`, `error`)
- Three-parameter signature: `(tag, message, exception)`

#### **Form State Management**
- Store backend IDs as strings, not object references
- Separate display names from backend values
- Reference: `CustomerFormState` in `CustomerFormViewModel.kt`

### **🔄 Testing Strategy**

- **Layer-by-Layer**: Don't batch multiple layer changes
- **User Feedback**: When users correct patterns, apply exactly as specified
- **Compilation**: Test after each major structural change

**Reference Implementation**: Customer module DTO alignment (January 2025) - demonstrates complete migration from enum-based to dynamic string-based customer types.