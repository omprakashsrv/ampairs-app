# Ampairs Mobile Application

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-blue)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.11.0-brightgreen)
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
- **Cloud Storage** — AWS S3 image/file uploads
- **In-App Billing** — Google Play Billing (Android) and StoreKit (iOS)

---

## Technology Stack

### Core

| Library | Version | Purpose |
|---|---|---|
| Kotlin KMP | 2.3.21 | Language & multiplatform |
| Compose Multiplatform | 1.11.0 | Declarative UI across platforms |
| Material 3 + Material Kolor | 1.9.0 / 3.0.1 | Design system + dynamic colors |
| Koin | 4.1.1 | Dependency injection |
| Room KMP | 2.8.3 | Local database |
| Store5 | 5.1.0-alpha08 | Offline-first cache layer |
| Ktor | 3.3.2 | HTTP client + WebSockets |
| Navigation3 | 1.0.0-alpha06 | Back-stack navigation with NavKey |
| kotlinx.coroutines | 1.10.2 | Async & concurrency |
| kotlinx.datetime | 0.7.1 | Cross-platform date/time |
| DataStore | 1.2.0 | Key-value preferences |

### Platform & Integration

| Library | Version | Purpose |
|---|---|---|
| Firebase BOM | 34.9.0 | Crashlytics, Analytics, Perf, FCM |
| Sentry KMP | 0.23.1 | Error monitoring |
| AWS SDK Kotlin | 1.5.44 | S3 file storage |
| Krossbow | 9.3.0 | STOMP/WebSocket real-time |
| Wire | 5.4.0 | Protocol Buffers (Tally) |
| Coil | 3.3.0 | Image loading & caching |
| Kermit | 2.0.8 | Multiplatform logging |
| Moko Permissions | 0.20.1 | Cross-platform permissions |
| FileKit | 0.12.0 | Cross-platform file picker |
| WorkManager | 2.11.1 | Android background sync |
| Play Billing | 8.3.0 | Android in-app purchases |
| Maps Compose | 8.1.0 | Android Google Maps |
| Haze | 1.7.2 | UI blur / glassmorphism |
| Adaptive Layouts | 1.2.0 | Multi-pane adaptive UI |

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
├── androidApp/              # Android entry point (versionName 1.0.0.17)
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
│   ├── aws/                # AWS S3 file/image uploads
│   ├── business/           # Business profile, tax config, custom attributes
│   ├── customer/           # CRM: customers, groups, types, images, states
│   ├── event/              # Event management
│   ├── form/               # Dynamic entity form configuration
│   ├── inventory/          # Stock tracking and movement
│   ├── invoice/            # Invoice creation, GST, PDF, email
│   ├── order/              # Order management, pricing, status workflow
│   ├── product/            # Catalog, variants, categories, images
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
│   ├── domain/             # Store5 store definitions
│   ├── di/                 # Koin module (common)
│   └── ui/                 # Compose screens + ViewModels
├── androidMain/            # Android DB factory, platform DI
├── iosMain/                # iOS DB factory, platform DI
└── desktopMain/            # Desktop DB factory, platform DI
```

---

## Architecture

### Offline-First with Store5

```
Compose UI  →  ViewModel (MVI/StateFlow)
                    ↓
              Store5 (cache layer)
                    ↓
              Repository (business logic)
               ↙          ↘
         Room DB         Ktor API
       (local first)   (background sync)
```

All writes go to Room first with `synced = false`. Server sync happens asynchronously. If sync fails, data is preserved locally and retried on next sync cycle.

### Workspace-Scoped Databases

Each workspace gets isolated Room database instances. The `DatabaseScopeManager` in `data/common` manages the lifecycle. All workspace-aware Koin components (`Database → DAO → Repository → Store`) must use `factory {}` scope to avoid stale references after workspace switching.

### Navigation (Navigation3)

Uses AndroidX Navigation3 with user-owned back stack. Routes implement `NavKey`:

```kotlin
@Serializable sealed interface Route : NavKey {
    @Serializable data object Customer : Route
    @Serializable data object Order : Route
    // ...
}
```

Entry providers per feature module are combined in `shared/` via `CombinedEntryProvider`.

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

### Koin Scoping

- `factory {}` — all workspace-aware components (Database, DAOs, Repositories, Stores)
- `single {}` — only `AuthRoomDatabase` and `WorkspaceRoomDatabase`
- `viewModel {}` / `viewModelOf {}` — all ViewModels

### Offline-First Rules

- Write to Room with `synced = false` before any network call
- Generate UIDs in ViewModel: `UidGenerator.generateUid(prefix)` — never in Repository
- Preserve local unsynced changes over server data during pull sync
- Batch sync: 100 records/batch, max 10,000/cycle

### API Conventions

```kotlin
ApiUrlBuilder.customerUrl("v1/groups")          // URL building
if (response.data != null && response.error == null) { }  // response check
SomeLogger.w("Tag", "message", exception)        // logger: w/e/i/d
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

Apache License 2.0 — see LICENSE file for details.

---

*Built with Kotlin Multiplatform and Compose Multiplatform*
