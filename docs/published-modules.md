# Ampairs KMP Published Modules — Integration Guide

This guide explains how to consume the published Ampairs KMP modules from another KMP project.

---

## Published Artifacts

All artifacts are published to GitHub Packages under group `com.ampairs`.

| Artifact | Version | What it provides |
|---|---|---|
| `com.ampairs:data-common` | 1.0.0 | `ApiUrlBuilder`, `Response<T>`, `UidGenerator`, `ConfigurationManager`, `DataStoreManager`, Room infra, Ktor client base, `WorkspaceContext` |
| `com.ampairs:auth-api` | 1.0.0 | `TokenRepository`, `UserDataService`, `UserWorkspaceRepository`, `KtorClient`, `DeviceService`, auth domain models |
| `com.ampairs:auth` | 1.0.0 | Full auth feature — OTP/phone UI screens, Room DB, Firebase Auth (Android/iOS), device management |
| `com.ampairs:sync` | 1.0.3 | `CentralSyncService`, `SyncDelegate`, `SyncEntity`, `SyncEvent`, `SyncStatus`, offline push/pull coordination |
| `com.ampairs:event` | 1.0.0 | WebSocket/STOMP real-time event infrastructure (Krossbow) |

**Dependency chain** (transitive — declare only the leaf you need):
```
auth  ──────────────► auth-api ──► data-common
sync  ──────────────────────────► data-common
event ──────────────────────────► data-common
```

---

## Step 1 — Credentials

GitHub Packages requires authentication even for read access.

Create a GitHub Personal Access Token with **`read:packages`** scope at:
`GitHub → Settings → Developer settings → Personal access tokens (classic)`

Add to **`~/.gradle/gradle.properties`** on every machine that will build the consumer project (never commit these values):

```properties
gpr.user=omprakashsrv
gpr.key=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

For CI (GitHub Actions), the built-in `GITHUB_TOKEN` secret has `read:packages` automatically.

---

## Step 2 — Add the Maven repository

In the consumer project's **`settings.gradle.kts`**, inside `dependencyResolutionManagement { repositories { } }`:

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

---

## Step 3 — Declare dependencies

In the consumer module's `build.gradle.kts`, inside `kotlin { sourceSets { commonMain { dependencies { } } } }`:

```kotlin
// Token management, Ktor HTTP client with auto-refresh JWT, user session
implementation("com.ampairs:auth-api:1.0.0")

// Full auth feature — OTP screens, Room DB, Firebase Auth
// (includes auth-api and data-common transitively)
implementation("com.ampairs:auth:1.0.0")

// Shared infrastructure — only if you don't need full auth
implementation("com.ampairs:data-common:1.0.0")

// Offline-first sync coordination
implementation("com.ampairs:sync:1.0.3")

// Real-time WebSocket/STOMP events
implementation("com.ampairs:event:1.0.0")
```

> Do not add `data-common` or `auth-api` explicitly when you already depend on `auth` — they resolve transitively.

---

## Step 4 — Platform-specific Ktor engines

Each platform needs its own Ktor engine. Add to each platform source set:

```kotlin
// androidMain
implementation("io.ktor:ktor-client-okhttp:<ktor_version>")

// iosMain
implementation("io.ktor:ktor-client-darwin:<ktor_version>")

// desktopMain (jvm)
implementation("io.ktor:ktor-client-okhttp:<ktor_version>")
```

Check the Ktor version used in `data-common`'s POM and match it exactly. Current version: **3.3.2**.

---

## API Usage

### Response\<T\> — always null-check, no `.success` property

```kotlin
import com.ampairs.common.model.Response

val response: Response<MyDto> = api.fetchSomething()
if (response.data != null && response.error == null) {
    val data: MyDto = response.data!!
} else {
    val errorMessage = response.error?.message
}
```

### ApiUrlBuilder — building endpoint URLs

```kotlin
import com.ampairs.common.ApiUrlBuilder

// Pattern: ApiUrlBuilder.{domain}Url("v1/path")
val url = ApiUrlBuilder.authUrl("v1/auth/login")
val url = ApiUrlBuilder.customerUrl("v1/groups")
val url = ApiUrlBuilder.productUrl("v1/items")
```

### UidGenerator — client-side UID generation

Always generate UIDs in the **ViewModel**, before calling the repository.

```kotlin
import com.ampairs.common.id_generator.UidGenerator

// Format: {PREFIX}{YYYYMMDDHHMMSS}{RANDOM} — 32 chars total
val uid = UidGenerator.generateUid("ORD")  // e.g. "ORD20250601143012ABCD1234EFGH5678"
```

### KtorClient — pre-configured HTTP client with JWT

```kotlin
import com.ampairs.common.KtorClient
import com.ampairs.auth.api.TokenRepository

// Create once and share as a singleton
val httpClient = KtorClient.create(tokenRepository)

// Use with ApiUrlBuilder
val response: Response<OrderDto> = httpClient.get(ApiUrlBuilder.orderUrl("v1/orders"))
```

### TokenRepository — token access and session state

```kotlin
import com.ampairs.auth.api.TokenRepository

class MyRepository(private val tokenRepo: TokenRepository) {
    suspend fun isLoggedIn(): Boolean = tokenRepo.getAccessToken() != null

    suspend fun getAuthHeader(): String? =
        tokenRepo.getAccessToken()?.let { "Bearer $it" }
}
```

### UserDataService — current user info

```kotlin
import com.ampairs.auth.api.UserDataService

class ProfileViewModel(private val userDataService: UserDataService) : ViewModel() {
    init {
        viewModelScope.launch {
            val user = userDataService.getCurrentUser()  // null if no session
            _uiState.update { it.copy(userName = user?.name.orEmpty()) }
        }
    }
}
```

### CentralSyncService — triggering and observing sync

```kotlin
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus

class OrderListViewModel(
    private val repository: OrderRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    init {
        // Observe sync state → drive loading spinner
        syncService.observeEntity(SyncEntity.ORDER)
            .onEach { state ->
                _uiState.update { it.copy(isRefreshing = state?.status is SyncStatus.Syncing) }
            }
            .launchIn(viewModelScope)

        // Pull latest data on screen open
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ORDER))
    }

    fun onCreateOrder(form: OrderForm) {
        viewModelScope.launch {
            val uid = UidGenerator.generateUid("ORD")
            repository.createOrder(form.toOrder(uid))
            // Every local write must mark entity as pending push
            syncService.markPendingPush(SyncEntity.ORDER)
        }
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.ORDER))
    }
}
```

### SyncDelegate — wiring your own entity into the sync pipeline

Implement and register a `SyncDelegate` for each entity you want to sync:

```kotlin
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncResult

@Inject
@ContributesIntoMap(AppScope::class)
@SyncEntityKey(SyncEntity.ORDER)
class OrderSyncDelegate(
    private val repository: OrderRepository,
) : SyncDelegate {

    override val entity = SyncEntity.ORDER

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

### ConfigurationManager — backend base URL and workspace

```kotlin
import com.ampairs.common.config.ConfigurationManager

// Read the active workspace slug (set during login)
val workspaceSlug = ConfigurationManager.getWorkspaceSlug()

// Read the backend base URL
val baseUrl = ConfigurationManager.getBaseUrl()
```

---

## Offline-First Write Pattern

Every local write in your feature must follow this sequence:

```kotlin
// 1. Generate UID in ViewModel before touching the repository
val uid = UidGenerator.generateUid("ORD")

// 2. Write to local Room DB with synced = false
repository.createLocally(entity.copy(uid = uid, synced = false))

// 3. Trigger background push via CentralSyncService
syncService.markPendingPush(SyncEntity.ORDER)

// Never call repository.syncToServer() directly from a ViewModel
```

---

## Verify the integration

Run a compile check on all target platforms after adding the dependencies:

```bash
./gradlew :{your-module}:compileDebugKotlinAndroid
./gradlew :{your-module}:compileKotlinDesktop
./gradlew shared:compileKotlinIosSimulatorArm64
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Could not resolve com.ampairs:*` | Check `settings.gradle.kts` has the GitHub Packages repo; verify `gpr.key` is set in `~/.gradle/gradle.properties` |
| `401 Unauthorized` on dependency resolution | PAT expired or missing `read:packages` scope — regenerate at GitHub → Settings → Developer settings |
| `Response<T>.data` NPE | Always null-check: `if (response.data != null && response.error == null)` — there is no `.success` property |
| `Unresolved reference: Dispatchers.IO` in iosMain | Use `Dispatchers.Default` in `iosMain` source sets; `Dispatchers.IO` is only safe in `commonMain` |
| Stale data after workspace switch | Ensure your DI chain uses `factory {}` not `single {}` for all workspace-aware layers |
