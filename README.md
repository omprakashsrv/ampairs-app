# Ampairs Applications

[![License](https://img.shields.io/badge/License-PolyForm_Noncommercial_1.0.0-orange.svg)](LICENSE.md)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-blue)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.11.1-brightgreen)
![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20iOS%20%7C%20Desktop-orange)

A Kotlin Multiplatform business management client for the Ampairs ecosystem. Built with Compose Multiplatform and an offline-first architecture, targeting **Android, iOS, Desktop (JVM), and WebAssembly**.

---

## Overview

Ampairs Mobile is part of a three-tier business management ecosystem:

| Layer | Technology |
|---|---|
| Backend | Spring Boot + Kotlin REST API |
| Web Frontend | Angular + Material Design 3 |
| **Mobile App** | **Kotlin Multiplatform ← THIS PROJECT** |

### Key Capabilities

- **Offline-First** — Full CRUD works without internet; background sync with conflict resolution
- **Multi-Tenant** — Workspace isolation with per-workspace database scoping
- **AI Agent** — Conversational agentic actions across business domains
- **Real-Time** — WebSocket/STOMP live updates when online
- **Business Modules** — CRM, Products, Orders, Invoices, Inventory, Tax, Tally ERP
- **In-App Billing** — Google Play Billing (Android) and StoreKit (iOS)

---

## Technology Stack

### Core

| Library | Version | Purpose |
|---|---|---|
| Kotlin KMP | 2.4.0 | Language & multiplatform |
| Compose Multiplatform | 1.11.1 | Declarative UI across platforms |
| Material 3 + Material Kolor | 1.9.0 / 3.0.1 | Design system + dynamic colors |
| Metro | 1.1.1 | Dependency injection (compile-time) |
| Room KMP | 2.8.4 | Local database |
| Ktor | 3.5.0 | HTTP client + WebSockets |
| Navigation3 | 1.1.1 | Back-stack navigation with NavKey |
| kotlinx.coroutines | 1.11.0 | Async & concurrency |
| kotlinx.datetime | 0.8.0 | Cross-platform date/time |
| DataStore | 1.2.1 | Key-value preferences |

### Platform & Integration

| Library | Version | Purpose |
|---|---|---|
| Firebase BOM | 34.14.0 | Crashlytics, Analytics, Perf, FCM |
| Sentry KMP | 0.27.0 | Error monitoring |
| Krossbow | 9.3.0 | STOMP/WebSocket real-time |
| Wire | 5.4.0 | Protocol Buffers (Tally ERP) |
| Coil | 3.4.0 | Image loading & caching |
| Kermit | 2.1.0 | Multiplatform logging |
| Moko Permissions | 0.20.1 | Cross-platform permissions |
| FileKit | 0.14.1 | Cross-platform file picker |
| WorkManager | 2.11.1 | Android background sync |
| Play Billing | 9.0.0 | Android in-app purchases |
| Maps Compose | 8.3.0 | Android Google Maps |
| Adaptive Layouts | 1.2.0 | Multi-pane adaptive UI |
| Lifecycle ViewModel | 2.10.0 | ViewModel + SavedState |

### Platform Targets

| Platform | Min Version | Notes |
|---|---|---|
| Android | SDK 24 (Android 7) | Target SDK 36 |
| iOS | iOS 14+ | iosArm64 + iosSimulatorArm64 |
| Desktop | JVM 21+ | Windows, macOS, Linux |
| WebAssembly | — | Experimental (wasmJsMain) |

---

## Project Structure

```
ampairs-app/
├── androidApp/              # Android entry point (versionName 1.0.9)
├── desktopApp/              # Desktop JVM entry point
├── iosApp/                  # Xcode project wrapper
├── shared/                  # Compose UI, navigation (Nav3), DI root, Firebase
│   └── src/{commonMain, androidMain, iosMain, desktopMain, wasmJsMain}/
├── data/common/             # DB factories, DataStore, ApiUrlBuilder, DatabaseScopeManager
├── tallyModule/             # Tally ERP integration (JVM, Wire protocol)
├── thirdparty/
│   └── androidx/paging/    # Custom KMP Paging3 wrapper
├── feature/
│   ├── auth/               # Phone/OTP login, JWT, device management, Firebase Auth
│   ├── agent/              # AI agent chat & agentic actions
│   ├── business/           # Business profile, tax config, custom attributes
│   ├── customer/           # CRM: customers, groups, types, images, states
│   ├── event/              # Event management
│   ├── form/               # Dynamic entity form configuration
│   ├── inventory/          # Stock tracking and movement
│   ├── invoice/            # Invoice creation, GST, PDF, email
│   ├── order/              # Order management, pricing, status workflow
│   ├── product/            # Catalog, variants, categories, images
│   ├── store/              # Workspace settings (offline-sync; module toggles e.g. tax-inclusive pricing, discount visibility)
│   ├── subscription/       # In-app billing (StoreKit / Play Billing)
│   ├── tax/                # Tax codes, configurations, calculator
│   ├── unit/               # Unit definitions and conversions
│   ├── update/             # In-app update management
│   └── workspace/          # Multi-tenant workspaces, members, dynamic module nav
├── gradle/
│   └── libs.versions.toml  # Centralized version catalog
├── .claude/
│   ├── rules.md            # Claude Code project rules
│   └── memory/             # Claude Code project memory
└── CLAUDE.md               # Development guidelines (imports .claude/)
```

### Feature Module Internal Layout

Each feature module follows a consistent structure:

```
feature/{name}/src/
├── commonMain/kotlin/com/ampairs/{name}/
│   ├── data/api/           # Ktor API interface + implementation
│   ├── data/db/            # Room database, DAOs, entities
│   ├── data/repository/    # Repository implementations
│   ├── domain/             # Domain models and business logic
│   └── ui/                 # Compose screens + ViewModels (@Inject / @AssistedInject)
├── androidMain/            # Android DB factory (@ContributesTo platform module)
├── iosMain/                # iOS DB factory (@ContributesTo platform module)
└── desktopMain/            # Desktop DB factory (@ContributesTo platform module)
```

---

## Architecture

### Offline-First

```
Compose UI  →  ViewModel (MVI/StateFlow)
                    ↓
              Repository (local-only: Room + mark PENDING_PUSH)
                    ↓                         ↑ observes flag
              Room DB  ←———————  CentralSyncService
                    ↑                         ↓ drives push/pull
              SyncDelegate (the only layer with the Ktor API)  →  Ktor API
```

The **repository is local-only** — it never talks to the network. Every write goes to Room with
`synced = false` and flags the entity `PENDING_PUSH` via `SyncStateDao`. `CentralSyncService` observes
that flag and runs a **bulk push** through the feature's `SyncDelegate`, which is the single place that
holds the API; pulls are **batched** and permanently delete rows the server reports as deleted.
ViewModels observe Room DAOs via reactive `Flow` for UI updates and trigger manual refreshes with
`syncService.emit(TriggerFullSync(entity))`. If a push fails, data is preserved locally and retried on
reconnect. See `.claude/skills/offline-sync/SKILL.md` for the full architecture.

### Workspace-Scoped Databases

Each workspace gets its own Metro child graph (`WorkspaceScope`). Each platform's `@ContributesTo(WorkspaceScope::class)` module provides the database via `@Provides @SingleIn(WorkspaceScope::class)` and registers it with `WorkspaceClosableRegistry` so it is closed on workspace switch. DAOs and Repositories are unscoped (`@Inject` without `@SingleIn`) so they are created fresh per injection site and always see the current workspace's DB.

### Dependency Injection (Metro)

Metro provides compile-time DI with zero runtime reflection. The layering rule is strict:

```
Metro injects deps → ViewModel
ViewModel exposes StateFlow / UiEvent → Screen (@Composable)
Screen has zero knowledge of repositories or the DI graph
```

- **Plain ViewModel**: `@Inject` + `@ContributesIntoMap(AppScope::class)` + `@ViewModelKey`; screen uses `metroViewModel()`
- **Assisted ViewModel** (needs a runtime param like an ID): `@AssistedInject` + inner `Factory` interface; screen uses `assistedMetroViewModel<VM, VM.Factory>(key = id) { create(id) }`
- **Platform bindings**: `@ContributesTo(AppScope::class)` interfaces with `@Provides` companion objects in each platform source set

The `AppGraph` interface exposes exactly four properties for use at platform entry points: `themeManager`, `localeManager`, `imageLoader`, `locationService`.

### Navigation (Navigation3)

Uses AndroidX Navigation3 with user-owned back stack. Routes implement `NavKey`:

```kotlin
@Serializable sealed interface Route : NavKey {
    @Serializable data object Customer : Route
    @Serializable data object Order : Route
    // ...
}
```

Entry providers per feature module are combined in `shared/` via `CombinedEntryProvider`. Entry providers only wire navigation callbacks and route key params — all dependencies flow through Metro-injected ViewModels.

### Dynamic Module Navigation

Backend controls which modules are installed per workspace. The `ModuleRegistry` in `feature/workspace` maps backend module codes to local `Route` implementations. Unimplemented modules show an "Update App" dialog.

---

## Getting Started

### Prerequisites

- **Java 21+**
- **Android Studio** (Hedgehog or newer) with KMP plugin
- **macOS + Xcode 15+** for iOS development
- **CocoaPods** for iOS dependencies

### Setup

```bash
# Verify KMP environment
brew install kdoctor && kdoctor

# Clone and open
git clone <repository-url>
cd ampairs-app
# Open in Android Studio — switch to "Project" view to see all modules
```

---

## Build & Run

### Android

```bash
./gradlew androidApp:assembleDebug
./gradlew androidApp:installDebug
./gradlew androidApp:assembleRelease
```

In Android Studio: select `androidApp` run configuration.

### Desktop

```bash
./gradlew desktopApp:run
./gradlew desktopApp:package      # native installer (dmg / exe / deb)
```

Output: `desktopApp/build/compose/binaries`

### iOS

```bash
# Framework for Xcode
./gradlew shared:embedAndSignAppleFrameworkForXcode

# Or for simulator testing
./gradlew shared:compileKotlinIosSimulatorArm64
```

Open `iosApp/iosApp.xcodeproj` in Xcode, select a simulator, and run.

For physical device — update `iosApp/Configuration/Config.xcconfig` with your Team ID (`kdoctor --team-ids`).

### Compilation Validation (all platforms)

```bash
./gradlew androidApp:compileDebugKotlinAndroid
./gradlew shared:compileKotlinIosSimulatorArm64
./gradlew desktopApp:compileKotlin
```

### Cleanup

```bash
./gradlew clean
./cleanup.sh
```

---

## Development Guidelines

### KMP Rules — commonMain must be platform-agnostic

| Avoid (JVM-only) | Use instead (KMP) |
|---|---|
| `System.currentTimeMillis()` | `Clock.System.now().toEpochMilliseconds()` |
| `java.util.Date`, `Calendar` | `kotlinx.datetime.*` |
| `String.format()` | String interpolation `"$value"` |
| `Thread {}`, `synchronized {}` | `kotlinx.coroutines.*` |
| `java.io.File` | expect/actual file operations |
| `UUID.randomUUID()` | `com.benasher44:uuid` |
| `Log.d()`, `println()` | Kermit logger |

On iOS: use `Dispatchers.Default` — `Dispatchers.IO` does not exist on Kotlin/Native.

### Metro DI Rules

- **Never** access `LocalAppGraph.current` inside any `@Composable`
- **Never** access `AppGraphHolder.graph` inside entry providers or screens — only platform entry points (`MainView`, `MainViewController`, `main.kt`) may touch it
- **Never** add repositories or services to `AppGraph` — only the four infrastructure properties belong there
- All feature dependencies flow exclusively through Metro-injected ViewModels

### Offline-First Rules

- Repository is local-only: write to Room with `synced = false`, then `syncStateDao.markPendingPush(...)` — never call the API from the repository's write path
- All entity ↔ server traffic (bulk push, batched pull, events) lives in `{Name}SyncDelegate`, which holds the API
- Delete soft-deletes (`active = false, synced = false`) so the push picks it up
- Generate UIDs in ViewModel: `UidGenerator.generateUid(prefix)` — never in Repository
- Preserve local unsynced changes over server data during pull sync; permanently delete server-`DELETED` rows on pull
- Batch sync: 100 records/batch, max 10,000/cycle

### API Conventions

```kotlin
ApiUrlBuilder.customerUrl("v1/groups")                             // URL building
if (response.data != null && response.error == null) { }          // response check
SomeLogger.w("Tag", "message", exception)                         // logger: w/e/i/d
```

---

## Backend Integration

- **API Base**: `/api/v1/{resource}`
- **Auth**: JWT bearer with auto-refresh via Ktor plugin
- **Multi-tenancy**: `X-Workspace-ID` header
- **Response**: `Response<T>` wrapper from `com.ampairs.common.model`
- **Packages**: `com.ampairs.{feature}.{layer}` (e.g. `com.ampairs.customer.data.api`)

---

## Documentation

| File | Purpose |
|---|---|
| `CLAUDE.md` | Architecture, patterns, and development guidelines |
| `.claude/rules.md` | Enforced coding rules for Claude Code |
| `.claude/memory/` | Project context and version reference |

---

## License

**Source-available, noncommercial** — licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE.md).

You may use, modify, and share this software for any **noncommercial** purpose. **Commercial use is not permitted** — including use by or for a business to generate revenue, sell the software, or charge customers for products or services built with it. This is *not* an OSI-approved open-source license (it restricts commercial use).

For a commercial license, contact the copyright holder. Copyright © 2026 Om Prakash.

---

*Built with Kotlin Multiplatform and Compose Multiplatform*
