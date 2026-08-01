---
name: Dependency Versions
description: Current pinned dependency versions for the Ampairs KMP app (as of July 2026, release 1.0.21)
type: project
originSessionId: 35585732-55ed-4e7b-8cf2-fb305112b179
---
Versions from `gradle/libs.versions.toml` (current as of July 2026 / app release 1.0.21):

| Dependency | Version |
|---|---|
| Kotlin | 2.4.0 |
| AGP (Android Gradle Plugin) | 9.3.0 |
| KSP | 2.3.10 |
| Compose Multiplatform | 1.11.1 |
| Compose Material | 1.9.0 |
| Room KMP (androidx.room3) | 3.0.0 |
| SQLite bundled | 2.7.0 |
| Ktor | 3.5.1 |
| Metro | 1.2.1 |
| Navigation3 | 1.1.1 |
| DataStore | 1.2.1 |
| kotlinx.coroutines | 1.11.0 |
| kotlinx.datetime | 0.8.0 |
| kotlinx.io | 0.9.0 |
| Coil | 3.5.0 |
| Firebase BOM | 34.15.0 |
| Firebase Auth | 24.1.0 |
| Sentry KMP | 0.27.0 |
| Krossbow (STOMP/WebSocket) | 9.3.0 |
| WorkManager | 2.11.2 |
| Play Billing | 9.1.0 |
| Kermit (logging) | 2.1.0 |
| Lifecycle ViewModel (+ Navigation3 integration) | 2.10.0 |
| AndroidX Paging | 3.5.0 |
| FileKit | 0.14.2 |
| Material3 Adaptive | 1.2.0 |
| Accompanist Permissions (Android) | 0.37.3 |
| Grant (dev.brewkits — KMP permissions/bluetooth) | 2.2.1 |
| Maps Compose | 8.3.0 |
| Play Services Location | 21.3.0 |
| Play Services Auth | 21.6.0 |
| Play Integrity | 1.6.0 |
| benasher44 UUID | 0.8.4 |
| LiteRT-LM (on-device LLM, agent) | 0.13.1 |
| whisper-jni (desktop STT) | 1.7.1 |
| whisper.cpp XCFramework (iOS STT) | 1.9.1 |
| Vosk (desktop STT) | 0.3.38 (pinned — see toml comment) |
| Vosk Android | 0.3.75 |
| OpenJFX (desktop WebView print preview) | 21.0.5 |
| JMapViewer (desktop maps) | 2.25 |

**Removed since earlier docs:** Material Kolor, Wire (Protocol Buffers), Moko Permissions, AWS SDK Kotlin — no longer in the version catalog; don't reference them in new code.

**Android SDK**: minSdk 24 / targetSdk 36 / compileSdk 37 (bumped from 36 so androidx-core 1.19.0 and vico:multiplatform-android 2.5.2 satisfy their AAR metadata minCompileSdk requirement)

**App version**: versionName `1.0.21`, versionCode `100021` (in `androidApp/build.gradle.kts`)

**Why:** These versions are in the version catalog and are referenced across all modules. When adding dependencies, always add to `gradle/libs.versions.toml` and reference via version catalog aliases, not hardcoded strings.
