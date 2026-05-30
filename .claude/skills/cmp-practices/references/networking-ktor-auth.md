# Networking — Auth, WebSockets & SSE

> **Project:** JWT bearer tokens via Ktor `Auth` plugin. Auto-refresh on 401. DataStore for token storage.

Reference: [Ktor bearer auth](https://ktor.io/docs/client-bearer-auth.html) | [Ktor WebSockets](https://ktor.io/docs/client-websockets.html)

## Bearer Token Auth

```kotlin
fun createAuthenticatedClient(
    engine: HttpClientEngine,
    baseUrl: String,
    tokenStorage: TokenStorage,
    onSessionExpired: () -> Unit,
): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        defaultRequest { url(baseUrl) }

        install(Auth) {
            bearer {
                loadTokens {
                    val tokens = tokenStorage.getTokens()
                    BearerTokens(tokens.accessToken, tokens.refreshToken)
                }

                refreshTokens {
                    val refreshToken = oldTokens?.refreshToken
                        ?: return@refreshTokens null

                    try {
                        markAsRefreshTokenRequest()   // prevents auth plugin from intercepting refresh
                        val response = client.post("auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(RefreshRequest(refreshToken))
                        }.body<TokenResponse>()

                        tokenStorage.saveTokens(response.accessToken, response.refreshToken)
                        BearerTokens(response.accessToken, response.refreshToken)
                    } catch (e: Exception) {
                        onSessionExpired()
                        null   // null → Ktor won't retry the original request
                    }
                }

                sendWithoutRequest { request ->
                    request.url.pathSegments.none { it in listOf("login", "register", "refresh") }
                }
            }
        }
    }
}
```

**Key points:**
- `markAsRefreshTokenRequest()` — prevents circular auth loops (refresh request won't itself trigger another refresh)
- `oldTokens` — provided by Ktor's `RefreshTokensParams` receiver
- `sendWithoutRequest` — controls which endpoints skip auth entirely
- Return `null` from `refreshTokens` to signal refresh failed

## TokenStorage Interface

```kotlin
interface TokenStorage {
    suspend fun getTokens(): AuthTokens
    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun clearTokens()
}

data class AuthTokens(val accessToken: String, val refreshToken: String)
```

Implement with the project's DataStore (`DataStoreManager`) — reuse the existing instance, never create a new one.

## WebSocket Support

```kotlin
// commonMain — add ktor-client-websockets dependency
val client = HttpClient(engine) {
    install(WebSockets) {
        pingIntervalMillis = 30_000
    }
}

client.webSocket("wss://api.ampairs.com/ws") {
    send(Frame.Text(Json.encodeToString(SubscribeMessage("items"))))

    for (frame in incoming) {
        when (frame) {
            is Frame.Text -> {
                val message = Json.decodeFromString<ServerMessage>(frame.readText())
                // handle message
            }
            is Frame.Close -> break
            else -> Unit
        }
    }
}
```

### Type-safe WebSocket messaging

```kotlin
install(WebSockets) {
    contentConverter = KotlinxWebsocketSerializationConverter(Json)
}

client.webSocket("wss://api.ampairs.com/ws") {
    sendSerialized(SubscribeMessage("items"))
    val message = receiveDeserialized<ServerMessage>()
}
```

## Server-Sent Events (SSE)

SSE is built into `ktor-client-core` — no extra dependency.

```kotlin
install(SSE) { }

client.sse(ApiUrlBuilder.eventUrl("v1/stream")) {
    incoming.collect { event ->
        println("Event: ${event.event}, Data: ${event.data}")
    }
}
```

### SSE vs WebSocket

| Criterion | SSE | WebSocket |
|---|---|---|
| Direction | Server → Client only | Bidirectional |
| Protocol | HTTP | Protocol upgrade |
| Auto-reconnect | Built-in | Manual |
| Use case | Live feeds, notifications, streaming | Chat, real-time collaboration |

## Anti-Patterns

```kotlin
// ❌ No markAsRefreshTokenRequest() — causes infinite auth loop
refreshTokens {
    val response = client.post("auth/refresh") { ... }   // client intercepts this too!
    // ...
}

// ❌ Creating a new HttpClient per request in refreshTokens
// ✅ Use markAsRefreshTokenRequest() instead — same client, no interception
```
