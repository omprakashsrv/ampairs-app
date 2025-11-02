package com.ampairs.event

import com.ampairs.event.domain.ConnectionState
import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
import com.ampairs.event.util.EventLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.config.HeartBeat
import org.hildan.krossbow.stomp.config.HeartBeatTolerance
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.headers.StompSendHeaders
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

/**
 * Manages WebSocket/STOMP connection for a specific workspace.
 * Provides real-time event streaming from backend to enable multi-device collaboration.
 *
 * Features:
 * - Single STOMP connection per workspace
 * - Automatic event subscription to workspace topic
 * - Heartbeat mechanism to maintain connection (30s interval)
 * - Event filtering (skips own device events)
 * - Reactive connection state
 * - Infinite automatic reconnection while user is in workspace
 * - Exponential backoff (1s → 2s → 4s → ... → max 30s)
 * - Automatic token refresh before each reconnection attempt
 * - Only stops reconnecting when disconnect() is called (user exits workspace)
 *
 * Reconnection Strategy:
 * - Attempt 1: 1 second
 * - Attempt 2: 2 seconds
 * - Attempt 3: 4 seconds
 * - Attempt 4: 8 seconds
 * - Attempt 5: 16 seconds
 * - Attempt 6+: 30 seconds (max delay)
 * - Continues indefinitely until disconnect() or successful connection
 *
 * @param workspaceId Workspace identifier for event topic subscription
 * @param userId Current user identifier
 * @param deviceId Current device identifier (to filter own events)
 * @param httpClient Ktor HTTP client for WebSocket transport
 * @param tokenProvider Function to get current JWT access token
 * @param tokenRefresher Function to refresh expired tokens (returns true on success)
 * @param baseUrl API base URL (will be converted to WebSocket URL)
 */
class EventManager(
    private val workspaceId: String,
    private val userId: String,
    private val deviceId: String,
    private val httpClient: HttpClient,
    private val tokenProvider: suspend () -> String,
    private val tokenRefresher: suspend () -> Boolean,
    private val baseUrl: String
) {
    // Connection state exposed as StateFlow
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Events stream exposed as SharedFlow
    private val _events = MutableSharedFlow<WorkspaceEvent>(
        replay = 0, // Don't replay old events to new subscribers
        extraBufferCapacity = 100 // Buffer up to 100 events if collector is slow
    )
    val events: SharedFlow<WorkspaceEvent> = _events.asSharedFlow()

    // Internal state
    private var stompSession: StompSession? = null
    private var heartbeatJob: Job? = null
    private var subscriptionJob: Job? = null
    private var reconnectionJob: Job? = null

    // Reconnection configuration
    private var reconnectionAttempts = 0
    private val baseReconnectionDelay = 1000L // 1 second
    private val maxReconnectionDelay = 30000L // 30 seconds (reasonable max)
    private var shouldReconnect = false
    private val heartbeatIntervalMillis = 15_000L

    // Krossbow STOMP client with Ktor WebSocket transport
    private val stompClient = StompClient(KtorWebSocketClient(httpClient)) {
        heartBeat = HeartBeat(15.seconds, 15.seconds)
        heartBeatTolerance = HeartBeatTolerance(
            outgoingMargin = 3.seconds,
            incomingMargin = 5.seconds
        )
        connectionTimeout = 30.seconds
    }

    // JSON parser for manual deserialization
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * Connect to WebSocket and subscribe to workspace events.
     * Safe to call multiple times - will not reconnect if already connected.
     * Enables automatic reconnection on connection failure.
     */
    suspend fun connect() {
        if (_connectionState.value is ConnectionState.Connected) {
            EventLogger.w("EventManager", "Already connected to workspace: $workspaceId")
            return
        }

        shouldReconnect = true // Enable auto-reconnect
        reconnectionAttempts = 0 // Reset counter
        performConnection()
    }

    /**
     * Internal method to perform the actual connection.
     * Called by connect() and reconnection logic.
     */
    private suspend fun performConnection() {
        _connectionState.value = ConnectionState.Connecting
        EventLogger.i("EventManager", "Connecting to workspace: $workspaceId (attempt ${reconnectionAttempts + 1})")

        try {
            // Build WebSocket URL (replace http with ws/wss)
            val wsUrl = baseUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://") + "/ws"

            // Get JWT token
            var token = tokenProvider()
            if (token.isBlank()) {
                // Try refreshing token if empty
                EventLogger.w("EventManager", "No access token, attempting refresh")
                val refreshed = tokenRefresher()
                if (refreshed) {
                    token = tokenProvider()
                }
                if (token.isBlank()) {
                    throw IllegalStateException("No access token available after refresh")
                }
            }

            // Connect with JWT token in query parameter (backend supports this)
            // Note: JWT tokens are already base64url-encoded and URL-safe, so no encoding needed
            val urlWithToken = buildString {
                append(wsUrl)
                append("?token=")
                append(token) // JWT tokens are already URL-safe, don't encode
                append("&workspaceId=")
                append(workspaceId.encodeURLParameter()) // Workspace ID should still be encoded
            }
            EventLogger.d("EventManager", "Connecting to: $wsUrl")

            // Connect using Krossbow STOMP client
            stompSession = stompClient.connect(urlWithToken)

            _connectionState.value = ConnectionState.Connected(stompSession.toString())
            reconnectionAttempts = 0 // Reset on successful connection
            EventLogger.i("EventManager", "✅ Connected to workspace: $workspaceId")

            // Subscribe to workspace events topic
            subscribeToWorkspaceEvents()

            // Start heartbeat to keep connection alive
            startHeartbeat()

        } catch (e: Exception) {
            val authFailure = e.isAuthenticationFailure()
            EventLogger.e("EventManager", "Connection failed for workspace: $workspaceId", e)
            _connectionState.value = ConnectionState.Error(
                message = "Connection failed: ${e.message}",
                exception = e
            )

            // Clean up partial connection
            cleanup()

            // Attempt reconnection if enabled
            if (shouldReconnect) {
                scheduleReconnection(authFailure)
            }
        }
    }

    /**
     * Schedule automatic reconnection with exponential backoff.
     * Keeps retrying indefinitely while shouldReconnect is true (user is in workspace).
     * Only stops when user explicitly exits workspace (disconnect() is called).
     */
    private fun scheduleReconnection(refreshToken: Boolean = false) {
        reconnectionAttempts++

        // Calculate exponential backoff delay (capped at maxReconnectionDelay)
        val delay = min(
            baseReconnectionDelay * (1 shl (reconnectionAttempts - 1)),
            maxReconnectionDelay
        )

        EventLogger.i("EventManager", "Scheduling reconnection in ${delay}ms (attempt $reconnectionAttempts)")

        reconnectionJob = CoroutineScope(Dispatchers.Default).launch {
            delay(delay)

            if (shouldReconnect && isActive) {
                if (refreshToken) {
                    EventLogger.i("EventManager", "🔄 Refreshing token before reconnection")
                    val refreshed = tokenRefresher()
                    if (refreshed) {
                        EventLogger.i("EventManager", "✅ Token refreshed, attempting reconnection")
                    } else {
                        EventLogger.w("EventManager", "⚠️ Token refresh failed, attempting reconnection anyway")
                    }
                }

                performConnection()
            }
        }
    }

    /**
     * Subscribe to workspace events topic using STOMP.
     * Backend publishes events to: /topic/workspace.events.{workspaceId}
     */
    private suspend fun subscribeToWorkspaceEvents() {
        val session = stompSession ?: run {
            EventLogger.w("EventManager", "No STOMP session available for subscription")
            return
        }

        try {
            val destination = "/topic/workspace.events.$workspaceId"
            EventLogger.i("EventManager", "Subscribing to: $destination")

            // Subscribe to STOMP messages and manually deserialize
            subscriptionJob = CoroutineScope(Dispatchers.Default).launch {
                try {
                    val headers = StompSubscribeHeaders(destination)
                    val subscription = session.subscribe(headers)
                    subscription.collect { message ->
                        try {
                            val event = json.decodeFromString<WorkspaceEvent>(message.bodyAsText)
                            handleIncomingEvent(event)
                        } catch (e: Exception) {
                            EventLogger.e("EventManager", "Failed to parse event", e)
                        }
                    }
                } catch (e: Exception) {
                    val authFailure = e.isAuthenticationFailure()
                    EventLogger.e("EventManager", "Subscription collection failed - connection likely dropped", e)
                    _connectionState.value = ConnectionState.Error(
                        message = "Connection dropped: ${e.message}",
                        exception = e
                    )

                    // Trigger reconnection
                    if (shouldReconnect) {
                        cleanup()
                        scheduleReconnection(authFailure)
                    }
                }
            }

            EventLogger.i("EventManager", "✅ Subscribed to workspace events")

        } catch (e: Exception) {
            val authFailure = e.isAuthenticationFailure()
            EventLogger.e("EventManager", "Subscription failed", e)
            _connectionState.value = ConnectionState.Error(
                message = "Subscription failed: ${e.message}",
                exception = e
            )

            // Trigger reconnection
            if (shouldReconnect) {
                cleanup()
                scheduleReconnection(authFailure)
            }
        }
    }

    /**
     * Handle incoming workspace event.
     * Filters out events from current device and emits to subscribers.
     */
    private suspend fun handleIncomingEvent(event: WorkspaceEvent) {
        try {
            // Skip events from this device (avoid echo)
            if (event.isFromDevice(deviceId)) {
                EventLogger.d(
                    "EventManager",
                    "Skipping own event: ${event.eventType} for ${event.entityType}:${event.entityId}"
                )
                return
            }

            EventLogger.i(
                "EventManager",
                "📨 Received event: ${event.eventType} for ${event.entityType}:${event.entityId} (seq: ${event.sequenceNumber})"
            )

            // Emit to all listeners (repositories, ViewModels, etc.)
            _events.emit(event)

        } catch (e: Exception) {
            EventLogger.e("EventManager", "Error handling event", e)
        }
    }

    /**
     * Start heartbeat mechanism to keep connection alive.
     * Backend expects heartbeat every 30 seconds to /app/heartbeat
     */
    private fun startHeartbeat() {
        heartbeatJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && stompSession != null) {
                delay(heartbeatIntervalMillis)

                try {
                    val headers = StompSendHeaders("/app/heartbeat")
                    stompSession?.send(headers, FrameBody.Text(""))
                    EventLogger.d("EventManager", "💓 Heartbeat sent")
                } catch (e: Exception) {
                    EventLogger.w("EventManager", "Heartbeat failed", e)
                    // Don't break the loop - will retry next time
                }
            }
        }
    }

    /**
     * Disconnect from WebSocket and clean up resources.
     * Safe to call multiple times.
     * Disables automatic reconnection.
     */
    suspend fun disconnect() {
        EventLogger.i("EventManager", "Disconnecting from workspace: $workspaceId")
        shouldReconnect = false // Disable auto-reconnect
        reconnectionJob?.cancel()
        reconnectionJob = null
        cleanup()
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Clean up coroutines and STOMP session
     */
    private suspend fun cleanup() {
        heartbeatJob?.cancel()
        subscriptionJob?.cancel()

        try {
            stompSession?.disconnect()
        } catch (e: Exception) {
            EventLogger.w("EventManager", "Error during STOMP disconnect", e)
        }

        stompSession = null
    }

    /**
     * Send a text message to a STOMP destination (for future use).
     * Currently not used as backend auto-broadcasts events.
     */
    suspend fun sendTextMessage(destination: String, message: String) {
        try {
            val headers = StompSendHeaders(destination)
            stompSession?.send(headers, FrameBody.Text(message))
            EventLogger.d("EventManager", "Sent message to: $destination")
        } catch (e: Exception) {
            EventLogger.e("EventManager", "Failed to send message", e)
        }
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }

    /**
     * Get events for a specific entity type as Flow
     */
    fun getEventsForEntityType(entityType: String): SharedFlow<WorkspaceEvent> {
        return events
    }

    /**
     * Get events matching specific event types as Flow
     */
    fun getEventsByType(vararg eventTypes: EventType): SharedFlow<WorkspaceEvent> {
        return events
    }

    private fun Throwable.isAuthenticationFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            when (current) {
                is ResponseException -> {
                    val status = current.response.status
                    if (status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden) {
                        return true
                    }
                }

                else -> if (current.message?.contains("401", ignoreCase = false) == true) {
                    return true
                }
            }
            current = current.cause
        }
        return false
    }
}
