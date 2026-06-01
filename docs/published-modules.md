---
name: published-modules
description: "Wire Ampairs published KMP modules into a consumer project. Use when setting up a new KMP project that needs auth, sync, or common infra from Ampairs packages, or when adding a new module to the published set."
trigger: /published-modules
---

# /published-modules — Ampairs KMP Modules Integration Skill

When invoked, actively perform the integration task described by the user.
Default task when no argument is given: wire all relevant published modules into the current KMP project.

---

## What to do on invocation

1. **Identify the task** — is the user:
   - (a) Setting up a consumer project to use these modules?
   - (b) Publishing a new/updated module from this repo?
   - (c) Adding a new entity to the sync system?

2. **Read the current project state** — check `settings.gradle.kts` and target module's `build.gradle.kts` before writing anything.

3. **Apply the correct steps** from the sections below. Write the actual code, don't just describe it.

---

## Task A — Wire modules into a consumer KMP project

### Step 1: Add GitHub Packages to `settings.gradle.kts`

Find `dependencyResolutionManagement { repositories { } }` and add:

```kotlin
maven {
    name = "AmpairsGitHubPackages"
    url = uri("https://maven.pkg.github.com/omprakashsrv/ampairs-app")
    credentials {
        username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
        password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
    }
}
```

Tell the user to add to `~/.gradle/gradle.properties` (never commit):
```
gpr.user=omprakashsrv
gpr.key=<PAT with read:packages scope>
```

### Step 2: Add only the modules the consumer actually needs

Inside `kotlin { sourceSets { commonMain { dependencies { } } } }`:

| Need | Dependency to add |
|---|---|
| JWT token, Ktor client, user session | `implementation("com.ampairs:auth-api:1.0.0")` |
| Full OTP auth UI + Room DB + Firebase | `implementation("com.ampairs:auth:1.0.0")` |
| `ApiUrlBuilder`, `Response<T>`, `UidGenerator`, DataStore | `implementation("com.ampairs:data-common:1.0.0")` |
| `CentralSyncService`, `SyncDelegate`, offline push/pull | `implementation("com.ampairs:sync:1.0.3")` |
| WebSocket / STOMP real-time events | `implementation("com.ampairs:event:1.0.0")` |

**Do not add transitive deps explicitly** — `auth` pulls in `auth-api` and `data-common` automatically.

### Step 3: Verify — run a test compile

```bash
./gradlew :{module}:compileDebugKotlinAndroid
./gradlew :{module}:compileKotlinIosSimulatorArm64
```

---

## Task B — Publish a module (or bump version)

### Rules before touching build files

- **Check for `composeResources/`** in the module. If present, add:
  ```kotlin
  compose.resources {
      packageOfResClass = "ampairsapp.{module.path}.generated.resources"
  }
  ```
  Package formula: drop leading `:` from Gradle path, replace `:` with `.`, append `.generated.resources`.
  E.g. `:feature:customer` → `ampairsapp.feature.customer.generated.resources`

- **Check for Room `@Database` with `expect object`**. If present, do NOT add `kspCommonMainMetadata` (causes K2 same-module expect/actual conflict). Declare `override fun initialize()` in the `expect object` body. See `data/sync` as the reference.

### Publishing block to add to each module

```kotlin
plugins {
    // ... existing
    `maven-publish`
}

group = "com.ampairs"
version = "1.0.0"  // bump before every publish

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/omprakashsrv/ampairs-app")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

For `data/common` also add `base.archivesName.set("data-common")` so the artifact ID isn't just `common`.

### Publish commands (must run from macOS for iOS targets)

```bash
./gradlew :data:common:publish --no-configuration-cache
./gradlew :feature:auth-api:publish --no-configuration-cache
./gradlew :feature:auth:publish --no-configuration-cache
./gradlew :data:sync:publish --no-configuration-cache
./gradlew :data:event:publish --no-configuration-cache
```

### 409 Conflict recovery

GitHub Packages rejects overwrites. If a partial publish leaves an artifact at version X:
1. Bump `version` in `build.gradle.kts` by one patch (e.g. `1.0.3` → `1.0.4`)
2. Retry `publish`
3. Update the version table in this skill

---

## Task C — Add a new entity to the sync system (in consumer project)

1. Add `SyncEntity.MY_ENTITY` to the `SyncEntity` enum in `data:sync`
2. Implement `SyncDelegate`:

```kotlin
@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.MY_ENTITY)
class MyEntitySyncDelegate(private val repo: MyRepository) : SyncDelegate {
    override val entity = SyncEntity.MY_ENTITY
    override suspend fun pullFromServer() = repo.pullFromServer()
        .fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun pushPendingToServer() = repo.pushPendingToServer()
        .fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun handleBackendEvent(entityId: String, eventType: String) = pullFromServer()
}
```

3. In every ViewModel write path: `syncService.markPendingPush(SyncEntity.MY_ENTITY)`
4. In list ViewModel `init {}`: observe status + emit initial pull:

```kotlin
syncService.observeEntity(SyncEntity.MY_ENTITY)
    .onEach { _uiState.update { s -> s.copy(isRefreshing = it?.status is SyncStatus.Syncing) } }
    .launchIn(viewModelScope)
syncService.emit(SyncEvent.TriggerPull(SyncEntity.MY_ENTITY))
```

---

## Published artifact versions (update when bumping)

| Artifact | Current version |
|---|---|
| `com.ampairs:data-common` | 1.0.0 |
| `com.ampairs:auth-api` | 1.0.0 |
| `com.ampairs:auth` | 1.0.0 |
| `com.ampairs:sync` | 1.0.3 |
| `com.ampairs:event` | 1.0.0 |

GitHub Packages URL: `https://maven.pkg.github.com/omprakashsrv/ampairs-app`
