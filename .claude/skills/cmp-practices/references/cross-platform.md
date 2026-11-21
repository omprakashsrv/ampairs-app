# Cross-Platform (KMP) Specifics

> **Project targets:** Android, iOS, Desktop (JVM), WebAssembly. All feature code in `commonMain`. Platform-specific in `androidMain`/`iosMain`/`desktopMain`.

## What Belongs Where

### Always in `commonMain`

- Feature state, intents/events, MVI ViewModel logic
- Calculators, validators, eligibility checks
- Repository interfaces and implementations
- Domain models, use cases
- Shared composables, presentation mapping

### Must be platform-specific (expect/actual or interface)

- Runtime permissions (Moko Permissions — already abstracted)
- Share sheet, clipboard, haptics
- File picker (FileKit — already abstracted)
- Push notifications (FCM / APNs)
- Biometrics, keychain/keystore
- Location (play services / CoreLocation)

### Placement Table

| Concern | Placement | Why |
|---|---|---|
| ViewModel / reducer | `commonMain` | pure, testable, reusable |
| Repository contract + impl | `commonMain` | shared dependency boundary |
| Database factory | platform source sets | platform-specific file path + driver |
| Haptics / share / clipboard | interface + platform impl | easy to fake in tests |
| Date/time | `commonMain` — `kotlinx.datetime` | KMP-compatible |
| UUID generation | `commonMain` — `benasher44:uuid` | KMP-compatible |
| File paths | expect/actual | Android = `context.filesDir`, iOS = Documents, Desktop = `~/.ampairs` |

## Interfaces vs expect/actual

**Default recommendation:**

- **Interface** when the capability has lifetime, DI, fakes, or multiple implementations
- **`expect/actual`** for thin platform facts with no domain meaning (UUID, timestamp, clipboard)

```kotlin
// ✅ Interface for app capability — testable, DI-friendly
interface BackNavigationHandler {
    @Composable fun Handle(onBack: () -> Unit)
}
// androidMain
class AndroidBackNavHandler : BackNavigationHandler {
    @Composable override fun Handle(onBack: () -> Unit) { BackHandler(onBack = onBack) }
}
// iosMain — no hardware back on iOS; side drawer dismiss handles it
class IosBackNavHandler : BackNavigationHandler {
    @Composable override fun Handle(onBack: () -> Unit) { }
}

// ✅ expect/actual for thin primitive
expect fun randomUUID(): String
// androidMain
actual fun randomUUID(): String = java.util.UUID.randomUUID().toString()
// iosMain
actual fun randomUUID(): String = platform.Foundation.NSUUID().UUIDString()
```

## Replacement Table — JVM → KMP

| Wrong (JVM/Android-only) | Correct (KMP) |
|---|---|
| `System.currentTimeMillis()` | `Clock.System.now().toEpochMilliseconds()` |
| `Date()`, `Calendar`, `LocalDateTime` | `kotlinx.datetime.*` |
| `String.format("%.2f", value)` | String interpolation or expect/actual |
| `DecimalFormat`, `NumberFormat` | expect/actual formatting |
| `Thread {}`, `synchronized {}` | `kotlinx.coroutines.*`, `@Volatile` |
| `java.io.File`, `java.nio.*` | expect/actual file ops |
| `UUID.randomUUID()` | `com.benasher44:uuid` |
| `System.out.println()`, `Log.d()` | Kermit logger |
| `BackHandler {}` | expect/actual `BackNavigationHandler` |
| `LocalContext.current` | expect/actual or param |
| `Dispatchers.IO` on iOS (pre-1.7) | Now safe — coroutines 1.7+ supports it on all targets |

## iOS-Specific Rules

```kotlin
// ✅ Dispatchers.IO is available on all KMP targets since kotlinx.coroutines 1.7+
// This project uses 1.10.2 — Dispatchers.IO works on iOS
withContext(Dispatchers.IO) { ... }   // safe

// If you see old code using Dispatchers.Default for IO — that's the coroutines < 1.7 pattern
// No need to change existing working code, but new code can use Dispatchers.IO

// ✅ Foundation APIs require opt-in
@OptIn(ExperimentalForeignApi::class)
fun getIosDatabasePath(databaseName: String): String { ... }   // data/common iosMain

// Consolidated DB files (built in data/database DatabaseModule.*.kt — features never open a DB):
// Android: ampairs_app.db, workspace_{slug}_main.db (flat, app databases dir)
// iOS:     Documents/ampairs_app.db, Documents/workspace_{slug}/main.db
// Desktop: {DataDirectoryManager.getDatabaseDir()}/ampairs_app.db, …/workspace_{slug}/main.db
```

## Validation Before Adding Dependencies to commonMain

Before claiming a library works in `commonMain`, verify it publishes KMP artifacts:

1. Check Maven Central for `-jvm`, `-iosarm64`, `-iosX64`, `-iosSimulatorArm64`, `-wasmjs` artifacts
2. Much of AndroidX is still Android-only — check the specific version
3. KMP-confirmed: `kotlinx.datetime`, `kotlinx.coroutines`, `kotlinx.serialization`, `ktor-client-core`, `room-runtime` (2.7+), `datastore-preferences`, `lifecycle-viewmodel` (2.8+)

## Platform Capabilities as Semantic Effects

Model platform capabilities as effects; the route/shell executes them:

```kotlin
sealed interface ProductEffect {
    data class TriggerHaptic(val type: HapticType) : ProductEffect
    data class ShareQuote(val text: String) : ProductEffect
    data class OpenUrl(val url: String) : ProductEffect
}

// In route composable — not in ViewModel
LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
        when (effect) {
            is ProductEffect.TriggerHaptic -> haptics.perform(effect.type)
            is ProductEffect.ShareQuote -> shareText.share(effect.text)
            is ProductEffect.OpenUrl -> uriHandler.openUri(effect.url)
        }
    }
}
```

## Workspace-Scoped Database — Project-Critical Rule

When users switch workspaces, the workspace database must be replaced. Ampairs has **one**
workspace database, `AmpairsWorkspaceDatabase` (`:data:database`), provided per workspace child graph;
features never provide their own.

```kotlin
// ❌ WRONG — a feature module providing its own database (and in AppScope: stale after a switch)
@ContributesTo(AppScope::class)
interface CustomerPlatformModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideDb(...): CustomerDatabase = ...
    }
}

// ✅ CORRECT — data/database DatabaseModule.*.kt, once per workspace graph
@Provides @SingleIn(WorkspaceScope::class)
fun provideAmpairsWorkspaceDatabase(
    /* platform context/paths */,
    config: WorkspaceConfig,
    closableRegistry: WorkspaceClosableRegistry,
): AmpairsWorkspaceDatabase = Room.databaseBuilder<AmpairsWorkspaceDatabase>(/* workspace_{slug} path */)
    /* driver, dispatcher, migrations */
    .build()
    .also { closableRegistry.register { it.close() } }   // closed on workspace switch

// ✅ Features get unscoped DAOs from WorkspaceDatabaseDaoModule
@Provides fun provideCustomerDao(db: AmpairsWorkspaceDatabase): CustomerDao = db.customerDao()
```

Pre-/cross-workspace data (auth, workspace registry, model catalog) lives in the AppScope
`AmpairsAppDatabase`. Adding tables: CLAUDE.md → *Adding tables for a feature*.
