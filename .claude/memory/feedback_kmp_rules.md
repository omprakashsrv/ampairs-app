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

`Dispatchers.IO` is available on **all** KMP targets — including Kotlin/Native (iOS) — since `kotlinx.coroutines 1.7+`. This project uses 1.10.2, so `Dispatchers.IO` is safe everywhere.

**Summary:**
- `commonMain`: `Dispatchers.IO` ✅
- `iosMain` (actual): `Dispatchers.IO` ✅ (safe since coroutines 1.7+)
- `androidMain`/`desktopMain`: `Dispatchers.IO` ✅

**One nuance**: On Kotlin/Native, `Dispatchers.IO` is backed by the same thread pool as `Dispatchers.Default` — there is no separate IO-dedicated pool as on JVM. The API is consistent, but threading behavior differs from JVM.

**Historical note**: Older coroutines versions had `Dispatchers.IO` as internal on Native, requiring `Dispatchers.Default` in `iosMain`. If you see old code or blog posts using `Dispatchers.Default` for IO on iOS, this is why — it no longer applies at coroutines 1.10.2.

`DispatcherProvider.ios.kt` uses `Dispatchers.IO` for the `io` dispatcher.

---

## Compilation Validation Commands

```bash
# Test all 3 major targets
./gradlew shared:compileKotlinIosSimulatorArm64    # iOS (Kotlin/Native)
./gradlew androidApp:compileDebugKotlinAndroid      # Android (JVM)
./gradlew desktopApp:compileKotlin                  # Desktop (JVM)
```

Run these after any significant change to commonMain to catch platform leaks early.
