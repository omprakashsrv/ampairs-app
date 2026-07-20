---
name: Project Architecture
description: Multi-module KMP structure, feature modules, navigation system, and key file locations
type: project
originSessionId: 35585732-55ed-4e7b-8cf2-fb305112b179
---
## Module Structure (current as of July 2026)

The project is a fully separated multi-module layout (see `settings.gradle.kts`):

**App entry points**
- `shared/` — Compose Multiplatform UI, app-level navigation (Navigation3), DI root, Firebase
- `androidApp/` — Android entry point (main business app)
- `desktopApp/` — Desktop JVM entry point
- `iosApp/` — Xcode project wrapper
- `clientApp/` — white-label customer storefront app, **pinned to one store** at build time via `-Pclient=<id>` (per-client config in `clients/<id>/`, e.g. `ambika`); no product flavors
- `marketplaceApp/` — multi-store customer ecom app: storefront directory → pick a store → isolated per-store Room DB (mirrors workspace selection in the main app)
- `shared-ecom/` — shared storefront UI/logic consumed by both `clientApp` and `marketplaceApp` (`StorefrontRoot(graph, workspaceSlug?)` — slug pinned vs directory picker)

**Data layer**
- `data/common/` — WorkspaceAwareDatabaseFactory, DatabasePathProvider, DataStore, ApiUrlBuilder, `Response<T>`, UidGenerator, Ktor client base, `WorkspaceDatabaseProvider`
- `data/database/` — consolidated `AmpairsAppDatabase` (durable app-scoped data; `fallbackToDestructiveMigration` banned). Features must NOT depend on this module directly — use `WorkspaceDatabaseProvider` from `data/common`
- `data/sync/` — `CentralSyncService`, `SyncDelegate`, `SyncEntity`, offline push/pull coordination
- `data/event/` — WebSocket/STOMP real-time event infrastructure (Krossbow)

**Integrations**
- `tally/` — Tally ERP integration (JVM only; XML over kotlinx-xml — Wire/protobuf removed)
- `whispercpp/` — whisper.cpp bindings for on-device speech-to-text (agent voice input)
- `printing/core`, `printing/render`, `printing/transport` — printing engine (templates, rendering, transports); `feature/printing` is the UI/feature layer on top

**Feature modules** — 25 implementation modules + 10 thin `-api` contract modules:
agent, auth (+`auth-api`), business, customer (+`customer-api`), ecom (+`ecom-api`), file (+`file-api`), form (+`form-api`), formwidgets, inventory (+`inventory-api`), invoice, notification, order, payment, pricing, printing, product (+`product-api`), purchase, sequence, store, subscription (+`subscription-api`), supplier, tax (+`tax-api`), unit (+`unit-api`), update, workspace.

- `store` = workspace settings: server-driven definition catalog (pull-only, filtered to installed modules) + synced overrides (`SyncEntity.STORE`), consumed via `StoreSettingsProvider`.
- **`:api` / `:impl` split** (see `docs/api-impl-split-pattern.md`): cross-feature dependencies go through the `-api` module only (interfaces + domain models); an impl module never appears in another feature's `build.gradle.kts`. Prevents circular deps and speeds incremental builds.
- Several modules are **published to GitHub Packages** under `com.ampairs` (data-common, auth-api, auth, sync, event) for reuse by other KMP projects — see `docs/published-modules.md`. When adding `maven-publish` to a module with Compose resources, pin `compose.resources { packageOfResClass }` (see `/cmp-practices` §9).

**Why:** The split decouples platforms and features, enables independent module compilation, and lets the storefront apps scale to N clients with no new modules.

**How to apply:** Always add new code to the appropriate feature module or `data/common`. Never add feature code directly to `shared/` or platform apps. The `shared/` module only contains navigation wiring and top-level DI aggregation. Cross-feature access goes through the `-api` module.

## Navigation System

Uses **Navigation3** (androidx.navigation3, version 1.1.1). Routes implement `NavKey`:
- Top-level routes: `shared/src/commonMain/kotlin/com/ampairs/navigation/Routes.kt`
- Per-feature routes: e.g., `CustomerRoute`, `AuthRoute`, `TaxRoute` in their respective modules
- Entry providers: `shared/src/commonMain/kotlin/com/ampairs/navigation/providers/` (one per feature module)
- Combined: `CombinedEntryProvider` → `AppNavigationNav3.kt`

**Not** using `navController.navigate()` / `backStackEntry.toRoute()` — that was the old API.

## Feature Module Internal Layout

```
feature/{name}/src/commonMain/kotlin/com/ampairs/{name}/
├── data/api/          # API interface + Ktor implementation
├── data/db/           # Room DAOs, entities (workspace DB via WorkspaceDatabaseProvider)
├── data/repository/   # Repository implementations (local-only; API lives in SyncDelegate)
├── domain/            # Domain models and business logic
├── sync/              # {Name}SyncDelegate (bulk push / batched pull)
├── di/                # Metro DI (platform @ContributesTo modules + @Inject classes)
└── ui/                # Compose screens and ViewModels
```

## Key Infrastructure Files

- `data/common/.../WorkspaceAwareDatabaseFactory.kt` — expect/actual DB creation
- `data/common/.../DatabasePathProvider.kt` — expect/actual path resolution
- `data/common/.../DataStoreManager.kt` / `createAppDataStore.kt` — shared preferences
- `data/sync/.../CentralSyncService.kt` — sync coordinator (see `/offline-sync`)
- `shared/.../FirebaseModule.kt` — expect/actual Firebase per platform
- `feature/workspace/.../ModuleRegistry.kt` — maps backend module codes to local routes

## Platforms Supported

Android, iOS, Desktop (JVM), WebAssembly (wasmJsMain) — though wasmJs support is experimental/minimal.
