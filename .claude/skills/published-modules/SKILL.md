---
name: published-modules
description: "How to consume published Ampairs KMP modules (auth-api, auth, data-common, data-sync, data-event) from another KMP project via GitHub Packages."
trigger: /published-modules
---

# Published Ampairs KMP Modules — Consumer Guide

## Published Artifacts

All artifacts are at group `com.ampairs`, published to GitHub Packages:
`https://maven.pkg.github.com/omprakashsrv/ampairs-app`

| Module | Artifact | Version | Contents |
|---|---|---|---|
| `feature/auth-api` | `com.ampairs:auth-api:1.0.0` | 1.0.0 | `TokenRepository`, `UserDataService`, `UserWorkspaceRepository`, `KtorClient`, `DeviceService`, domain models |
| `feature/auth` | `com.ampairs:auth:1.0.0` | 1.0.0 | Full auth UI (OTP, phone, device management), Room DB, Firebase Auth |
| `data/common` | `com.ampairs:data-common:1.0.0` | 1.0.0 | `ApiUrlBuilder`, `ConfigurationManager`, `UidGenerator`, `DataStoreManager`, Room infrastructure, Ktor setup, `Response<T>` model |
| `data/sync` | `com.ampairs:sync:1.0.3` | 1.0.3 | `CentralSyncService`, `SyncDelegate`, `SyncEntity`, `SyncEvent`, `SyncStatus`, `SyncStateDatabase` |
| `data/event` | `com.ampairs:event:1.0.0` | 1.0.0 | WebSocket/STOMP event infrastructure (Krossbow) |

**Dependency chain**: `auth` → `auth-api` → `data-common`; `sync` → `data-common`; `event` → `data-common`.
Declare only the leaf artifact you need — transitive deps resolve automatically.

---

## Consumer Project Setup

### 1. GitHub Packages credentials

Add to `~/.gradle/gradle.properties` (never commit):
```properties
gpr.user=omprakashsrv
gpr.key=<PAT with read:packages scope>
```

Or via env vars: `GITHUB_ACTOR` / `GITHUB_TOKEN`.

### 2. Add the Maven repository

In the consumer project's `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "AmpairsGitHubPackages"
            url = uri("https://maven.pkg.github.com/omprakashsrv/ampairs-app")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### 3. Declare dependencies

In the consumer module's `build.gradle.kts` inside the `kotlin { sourceSets { commonMain { dependencies { } } } }` block:

```kotlin
// Auth interfaces + token/user services only (no UI, no Room DB)
implementation("com.ampairs:auth-api:1.0.0")

// Full auth feature (UI screens, Room DB, OTP, Firebase Auth)
implementation("com.ampairs:auth:1.0.0")

// Shared infrastructure (ApiUrlBuilder, Response<T>, UidGenerator, DataStore, Ktor)
implementation("com.ampairs:data-common:1.0.0")

// Offline-first sync coordination (CentralSyncService, SyncDelegate, SyncEntity)
implementation("com.ampairs:sync:1.0.3")

// Real-time WebSocket/STOMP event bus
implementation("com.ampairs:event:1.0.0")
```

---

## Key API Usage

### TokenRepository (from `auth-api`)

```kotlin
// Inject via Koin or constructor
class MyRepository(private val tokenRepo: TokenRepository) {
    suspend fun doAuthenticatedCall() {
        val token = tokenRepo.getAccessToken() // returns null if not logged in
        // token is auto-refreshed by Ktor plugin — just read it for inspection
    }
}
```

### UserDataService (from `auth-api`)

```kotlin
class ProfileViewModel(private val userDataService: UserDataService) : ViewModel() {
    init {
        viewModelScope.launch {
            val user = userDataService.getCurrentUser()
            // user: UserData? — null if no session
        }
    }
}
```

### KtorClient (from `auth-api`)

```kotlin
// Returns a pre-configured HttpClient with JWT bearer auth + auto-refresh
// Use ApiUrlBuilder from data-common to build URLs
val client: HttpClient = KtorClient.create(tokenRepository)

val response: Response<MyDto> = client.get(ApiUrlBuilder.myUrl("v1/items"))
if (response.data != null && response.error == null) {
    // success
}
```

### Response\<T\> (from `data-common`)

```kotlin
import com.ampairs.common.model.Response

// Always null-check data — there is no .success property
fun handleResponse(response: Response<MyDto>) {
    if (response.data != null && response.error == null) {
        val data: MyDto = response.data!!
    } else {
        val errorMsg = response.error?.message
    }
}
```

### ApiUrlBuilder (from `data-common`)

```kotlin
import com.ampairs.common.ApiUrlBuilder

// Builds the full URL using the current workspace + backend base URL
val url = ApiUrlBuilder.customerUrl("v1/groups")      // customer domain
val url = ApiUrlBuilder.productUrl("v1/items")        // product domain
val url = ApiUrlBuilder.authUrl("v1/auth/login")      // auth domain
```

### UidGenerator (from `data-common`)

```kotlin
import com.ampairs.common.id_generator.UidGenerator

// Always generate UIDs in the ViewModel, before calling the repository
val uid = UidGenerator.generateUid("CUS")  // → "CUS20250601143012ABCD1234EFGH5678"
```

### CentralSyncService (from `sync`)

```kotlin
// After any local write, mark the entity as pending push:
syncService.markPendingPush(SyncEntity.MY_ENTITY)

// Manual full sync (pull + push):
syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.MY_ENTITY))

// Observe sync status for a spinner:
syncService.observeEntity(SyncEntity.MY_ENTITY)
    .onEach { state -> isRefreshing = state?.status is SyncStatus.Syncing }
    .launchIn(viewModelScope)
```

### SyncDelegate (from `sync`) — implementing your own syncable entity

```kotlin
@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.MY_ENTITY)
class MyEntitySyncDelegate(
    private val repository: MyEntityRepository,
) : SyncDelegate {
    override val entity = SyncEntity.MY_ENTITY

    override suspend fun pullFromServer(): SyncResult =
        repository.pullFromServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        repository.pushPendingToServer().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()
}
```

---

## Publishing New Versions

Run from macOS (required for iOS targets):
```bash
./gradlew :data:common:publish
./gradlew :feature:auth-api:publish
./gradlew :feature:auth:publish
./gradlew :data:sync:publish
./gradlew :data:event:publish
```

**Version bump before republishing**: GitHub Packages rejects 409 Conflict on existing versions.
Update `version = "x.y.z"` in each module's `build.gradle.kts` before running `publish`.

**If publish partially fails** (Android publishes, then iOS compilation fails):
The Android artifact at that version is already on GitHub Packages and can't be overwritten.
Bump the version by one patch and retry.

---

## Notes for Room KMP Modules (data:sync)

`data:sync` omits `kspCommonMainMetadata` from its Room compiler deps to avoid a Kotlin 2.x
expect/actual same-module conflict during metadata compilation. The `expect object` in
`SyncStateDatabase.kt` declares `override fun initialize()` explicitly so K2 accepts the
declaration without a KSP-generated actual in the metadata context.

If adding a new Room `@Database` to a published module, apply the same pattern:
1. Do NOT add `kspCommonMainMetadata` to that module's Room compiler deps
2. Declare `override fun initialize(): MyDatabase` in the `expect object` body
