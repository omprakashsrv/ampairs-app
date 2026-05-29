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
fun getIosDatabasePath(slug: String, module: String): String { ... }

// iOS database path: Documents/workspace_{slug}/{module}.db
// Android database path: workspace_{slug}_{module}.db (flat file)
// Desktop database path: ~/.ampairs/workspace_{slug}/{module}.db
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

When users switch workspaces, **all databases must be replaced**. Using Metro `@SingleIn(AppScope::class)` caches stale database references.

```kotlin
// ❌ WRONG — stale DB after workspace switch
@ContributesTo(AppScope::class)
interface CustomerPlatformModule {
    companion object {
        @Provides @SingleIn(AppScope::class)
        fun provideDb(factory: WorkspaceAwareDatabaseFactory): CustomerDatabase =
            factory.createDatabase(CustomerDatabase::class, "customer")
    }
}

// ✅ CORRECT — new instance per injection, fresh after workspace switch
@ContributesTo(AppScope::class)
interface CustomerPlatformModule {
    companion object {
        @Provides   // no @SingleIn — unscoped = new instance per injection site
        fun provideDb(factory: WorkspaceAwareDatabaseFactory): CustomerDatabase =
            factory.createDatabase(CustomerDatabase::class, "customer")
    }
}
```

Exceptions that stay `@SingleIn`: `AuthRoomDatabase` and `WorkspaceRoomDatabase` (pre-workspace singletons).
