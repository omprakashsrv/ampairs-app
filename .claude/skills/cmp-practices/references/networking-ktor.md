# Networking with Ktor Client

> **Project setup:** Ktor 3.3.2. OkHttp on Android, Darwin on iOS, CIO on Desktop. JWT bearer auth. API URL builder: `ApiUrlBuilder.{domain}Url("v1/path")`.

References: [Ktor client docs](https://ktor.io/docs/client.html) | [Auth, WebSocket, SSE](networking-ktor-auth.md)

## HttpClient Configuration

Create a single reusable `HttpClient` instance — never create one per request.

```kotlin
fun createHttpClient(engine: HttpClientEngine, baseUrl: String): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true    // ignore unknown JSON fields
                coerceInputValues = true     // null → defaults for non-null props
                encodeDefaults = true        // include defaults when serializing
            })
        }

        defaultRequest {
            url(baseUrl)
            headers.append("Accept", "application/json")
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 15_000
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
            sanitizeHeader { it == "Authorization" }
        }
    }
}
```

## Platform Engines (per source set)

```kotlin
// androidMain
actual fun createPlatformHttpClient(baseUrl: String) = createHttpClient(OkHttp.create(), baseUrl)

// iosMain
actual fun createPlatformHttpClient(baseUrl: String) = createHttpClient(Darwin.create(), baseUrl)

// desktopMain
actual fun createPlatformHttpClient(baseUrl: String) = createHttpClient(CIO.create(), baseUrl)
```

## DTO Models

```kotlin
@Serializable
data class CustomerDto(
    val uid: String,
    val name: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("workspace_id") val workspaceId: String,
)

@Serializable
data class CustomerListDto(
    val customers: List<CustomerDto>,
    val total: Int,
    @SerialName("has_next") val hasNext: Boolean = false,
)
```

Always use `@SerialName("snake_case")` for backend field compatibility. DTOs mirror the API contract — no business logic.

## DTO → Domain Mappers

Map at the repository boundary. Domain models have no serialization annotations.

```kotlin
fun CustomerDto.toDomain() = Customer(
    uid = uid,
    name = name,
    phoneNumber = phoneNumber,
    workspaceId = workspaceId,
)

fun CustomerDto.toEntity() = CustomerEntity(
    uid = uid,
    name = name,
    phoneNumber = phoneNumber,
    workspaceId = workspaceId,
    synced = true,
)
```

## API Service Layer

```kotlin
class CustomerApiImpl(private val client: HttpClient) : CustomerApi {

    override suspend fun getCustomers(
        workspaceId: String,
        page: Int,
        limit: Int,
    ): CustomerListDto {
        return client.get(ApiUrlBuilder.customerUrl("v1/customers")) {
            parameter("workspaceId", workspaceId)
            parameter("page", page)
            parameter("limit", limit)
        }.body()
    }

    override suspend fun createCustomer(customer: Customer): CustomerDto {
        return client.post(ApiUrlBuilder.customerUrl("v1/customers")) {
            contentType(ContentType.Application.Json)
            setBody(customer.toDto())
        }.body()
    }

    override suspend fun deleteCustomer(uid: String) {
        client.delete(ApiUrlBuilder.customerUrl("v1/customers/$uid"))
    }
}
```

## Response Handling — Project Pattern

```kotlin
// Project's Response<T> wrapper — data is nullable, no .success property
if (response.data != null && response.error == null) {
    // success
} else {
    // handle error via response.error
}
```

## Repository — Offline-First Pattern

```kotlin
class CustomerRepositoryImpl(
    private val api: CustomerApi,
    private val dao: CustomerDao,
) : CustomerRepository {

    override fun observeCustomers(): Flow<List<Customer>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun syncCustomers(workspaceId: String) {
        var page = 1
        var hasNext = true
        while (hasNext) {
            val result = api.getCustomers(workspaceId, page, limit = 100)
            dao.insertAll(result.customers.map { it.toEntity() })
            hasNext = result.hasNext
            page++
        }
    }

    override suspend fun createCustomer(customer: Customer): Result<Customer> {
        require(customer.uid.isNotBlank()) { "UID must be set by ViewModel" }
        dao.insert(customer.toEntity().copy(synced = false))
        return try {
            val serverCustomer = api.createCustomer(customer)
            val resolved = if (serverCustomer.uid != customer.uid)
                serverCustomer.copy(uid = customer.uid) else serverCustomer
            dao.insert(resolved.toEntity().copy(synced = true))
            Result.success(resolved.toDomain())
        } catch (e: Exception) {
            Result.success(customer)   // graceful fallback — already saved locally
        }
    }
}
```

## Anti-Patterns

```kotlin
// ❌ New client per request — leaks connections, no connection pooling
suspend fun getCustomers() = HttpClient(OkHttp).get(...).body<CustomerListDto>()

// ❌ Hardcoded URL strings
client.get("https://api.ampairs.com/v1/customers")
// ✅
client.get(ApiUrlBuilder.customerUrl("v1/customers"))

// ❌ Generating UID in repository
val uid = UidGenerator.generateUid("CUS")   // should be in ViewModel
// ✅ Assert it was set by ViewModel
require(customer.uid.isNotBlank()) { "UID must be set by ViewModel" }

// ❌ Response.success property
if (response.success) { ... }   // does not exist
// ✅
if (response.data != null && response.error == null) { ... }
```
