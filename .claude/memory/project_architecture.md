---
name: Project Architecture
description: Multi-module KMP structure, feature modules, navigation system, and key file locations
type: project
originSessionId: 35585732-55ed-4e7b-8cf2-fb305112b179
---
## Module Structure (current as of May 2025)

The project migrated from `composeApp/` to a fully separated multi-module layout:

- `shared/` — Compose Multiplatform UI, app-level navigation (Navigation3), DI root, Firebase
- `androidApp/` — Android entry point
- `desktopApp/` — Desktop JVM entry point
- `iosApp/` — Xcode project wrapper
- `data/common/` — DatabaseScopeManager, WorkspaceAwareDatabaseFactory, DataStore, ApiUrlBuilder
- `tallyModule/` — Tally ERP integration (JVM only)
- `thirdparty/androidx/paging/compose/` — Custom KMP Paging3 wrapper
- `feature/{name}/` — 16 isolated feature modules (auth, agent, aws, business, customer, event, form, inventory, invoice, order, product, subscription, tax, unit, update, workspace)

**Why:** The migration decouples platforms and enables independent module compilation, improving build times and separation of concerns.

**How to apply:** Always add new code to the appropriate feature module or `data/common`. Never add feature code directly to `shared/` or platform apps. The `shared/` module only contains navigation wiring and top-level DI aggregation.

## Navigation System

Uses **Navigation3** (androidx.navigation3, version 1.0.0-alpha06). Routes implement `NavKey`:
- Top-level routes: `shared/src/commonMain/kotlin/com/ampairs/navigation/Routes.kt`
- Per-feature routes: e.g., `CustomerRoute`, `AuthRoute`, `TaxRoute` in their respective modules
- Entry providers: `shared/src/commonMain/kotlin/com/ampairs/navigation/providers/` (one per feature module)
- Combined: `CombinedEntryProvider` → `AppNavigationNav3.kt`

**Not** using `navController.navigate()` / `backStackEntry.toRoute()` — that was the old API.

## Feature Module Internal Layout

```
feature/{name}/src/commonMain/kotlin/com/ampairs/{name}/
├── data/api/          # API interface + Ktor implementation
├── data/db/           # Room database class, DAOs, entities
├── data/repository/   # Repository implementations
├── domain/            # Store5 store factories
├── di/                # Koin module (common + platform variants)
└── ui/                # Compose screens and ViewModels
```

## Key Infrastructure Files

- `data/common/.../DatabaseScopeManager.kt` — workspace-scoped DB lifecycle (singleton)
- `data/common/.../WorkspaceAwareDatabaseFactory.kt` — expect/actual DB creation
- `data/common/.../DatabasePathProvider.kt` — expect/actual path resolution
- `data/common/.../DataStoreManager.kt` / `createAppDataStore.kt` — shared preferences
- `shared/.../FirebaseModule.kt` — expect/actual Firebase per platform
- `feature/workspace/.../ModuleRegistry.kt` — maps backend module codes to local routes
- `feature/workspace/.../DatabaseScopeManager` integration in platform factories

## Platforms Supported

Android, iOS, Desktop (JVM), WebAssembly (wasmJsMain) — though wasmJs support is experimental/minimal.
