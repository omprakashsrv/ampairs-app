---
name: KMP Platform Compatibility Rules
description: What NOT to use in commonMain and their KMP-safe replacements — prevents iOS/Desktop compile failures
type: feedback
originSessionId: 35585732-55ed-4e7b-8cf2-fb305112b179
---
## The Core Rule

Never use JVM-specific APIs in `commonMain`. If the import starts with `java.*` or `android.*`, it's wrong.

**Why:** The app targets iOS and Desktop in addition to Android. JVM-specific APIs fail to compile on Kotlin/Native (iOS) and cause subtle runtime issues on Desktop.

**How to apply:** Before writing any code in commonMain, ask "will this API work on iOS?" If unsure, check whether the import is from `kotlin.*`, `kotlinx.*`, or a KMP-specific library. Run `./gradlew shared:compileKotlinIosSimulatorArm64` to validate.

---

## Replacement Table

| Wrong (JVM/Android-only) | Correct (KMP) |
|---|---|
| `System.currentTimeMillis()` | `Clock.System.now().toEpochMilliseconds()` |
| `Date()`, `Calendar`, `LocalDateTime` | `kotlinx.datetime.LocalDateTime`, `kotlinx.datetime.Clock` |
| `String.format("%.2f", value)` | String interpolation or expect/actual |
| `DecimalFormat`, `NumberFormat` | expect/actual formatting |
| `Thread {}`, `synchronized {}` | `kotlinx.coroutines.*`, `@Volatile` |
| `System.getProperty()` | expect/actual |
| `java.io.File`, `java.nio.*` | expect/actual file operations |
| `/tmp/path` or `C:\path` | Platform-specific directory resolution via expect/actual |
| `UUID.randomUUID()` | `com.benasher44:uuid` KMP lib or expect/actual |
| `System.out.println()`, `e.printStackTrace()` | Kermit logger (`kermit` library) |
| `Log.d()` (Android) | Kermit logger |

---

## iOS-Specific Dispatcher Rule

`Dispatchers.IO` is available as an `expect` declaration in `commonMain` (coroutines 1.7+), so it is **safe to use in `commonMain`**.

However, `Dispatchers.IO` is **internal** in the Kotlin/Native runtime — it cannot be accessed directly in `iosMain` source sets. Confirmed by compilation error: "Cannot access 'val IO: CoroutineDispatcher': it is internal in 'kotlinx.coroutines.Dispatchers'".

`DispatcherProvider.ios.kt` must use `Dispatchers.Default` for the `io` dispatcher (this is what `Dispatchers.IO` maps to internally on iOS/Native).

**Summary:**
- `commonMain`: `Dispatchers.IO` ✅ (via expect/actual, safe)
- `iosMain` (actual): `Dispatchers.Default` ✅ (must use this — `Dispatchers.IO` actual is internal on Native)
- `androidMain`/`desktopMain`: `Dispatchers.IO` ✅

**Why the distinction:** The `expect val Dispatchers.IO` in commonMain compiles fine everywhere because it resolves via the expect mechanism. But when writing code in the `iosMain` source set itself (which compiles as Kotlin/Native), the actual backing value is marked internal and cannot be referenced directly. Confirmed against coroutines 1.10.2 / Kotlin 2.3.21.

---

## Compilation Validation Commands

```bash
# Test all 3 major targets
./gradlew shared:compileKotlinIosSimulatorArm64    # iOS (Kotlin/Native)
./gradlew androidApp:compileDebugKotlinAndroid      # Android (JVM)
./gradlew desktopApp:compileKotlin                  # Desktop (JVM)
```

Run these after any significant change to commonMain to catch platform leaks early.
