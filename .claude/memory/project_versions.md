---
name: Dependency Versions
description: Current pinned dependency versions for the Ampairs KMP app (as of May 2025)
type: project
originSessionId: 35585732-55ed-4e7b-8cf2-fb305112b179
---
Versions from `gradle/libs.versions.toml`:

| Dependency | Version |
|---|---|
| Kotlin | 2.3.21 |
| AGP (Android Gradle Plugin) | 9.2.1 |
| Compose Multiplatform | 1.11.0 |
| Compose Material | 1.9.0 |
| Room KMP | 2.8.3 |
| Ktor | 3.3.2 |
| Koin | 4.1.1 |
| Store5 | 5.1.0-alpha08 |
| Navigation3 | 1.0.0-alpha06 |
| DataStore | 1.2.0 |
| kotlinx.coroutines | 1.10.2 |
| kotlinx.datetime | 0.7.1 |
| Coil | 3.3.0 |
| Material Kolor | 3.0.1 |
| Firebase BOM | 34.9.0 |
| Sentry KMP | 0.23.1 |
| AWS SDK Kotlin | 1.5.44 |
| Wire (Protocol Buffers) | 5.4.0 |
| Krossbow (STOMP/WebSocket) | 9.3.0 |
| WorkManager | 2.11.1 |
| Play Billing | 8.3.0 |
| Kermit (logging) | 2.0.8 |
| Lifecycle ViewModel | 2.9.6 |
| Moko Permissions | 0.20.1 |
| FileKit | 0.12.0 |
| Haze (UI blur) | 1.7.2 |
| Material3 Adaptive | 1.2.0 |
| Maps Compose | 8.1.0 |
| Play Services Location | 21.3.0 |
| Play Services Auth | 21.5.0 |
| Play Integrity | 1.6.0 |
| benasher44 UUID | 0.8.4 |
| Firebase Auth | 24.0.1 |
| JMapViewer (desktop maps) | 2.24 |

**Android SDK**: minSdk 24 / targetSdk 36 / compileSdk 36

**App version**: versionName `1.0.0.17`, versionCode `17` (in `androidApp/build.gradle.kts`)

**Why:** These versions are in the version catalog and are referenced across all modules. When adding dependencies, always add to `gradle/libs.versions.toml` and reference via version catalog aliases, not hardcoded strings.
