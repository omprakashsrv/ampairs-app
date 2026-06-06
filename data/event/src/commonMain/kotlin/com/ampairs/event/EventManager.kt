package com.ampairs.event

import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.EventType
import com.ampairs.common.workspace.WorkspaceClosable
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

class EventManager(
    private val workspaceId: String,
    private val deviceId: String,
    private val httpClient: HttpClient,
    private val tokenProvider: suspend () -> String,
    private val tokenRefresher: suspend () -> Boolean,
) : IEventManager, WorkspaceClosable {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WorkspaceEvent>(
        replay = 0,
        extraBufferCapacity = 100,
    )
    override val events: SharedFlow<WorkspaceEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var stompSession: StompSession? = null
    private var heartbeatJob: Job? = null
    private var subscriptionJob: Job? = null
    private var reconnectionJob: Job? = null

    private var reconnectionAttempts = 0
    private val baseReconnectionDelay = 1000L
    private val maxReconnectionDelay = 30000L
    private var shouldReconnect = false
    private val heartbeatIntervalMillis = 30_000L

    private val stompClient = StompClient(KtorWebSocketClient(httpClient)) {
        // Server closes at 15s if no STOMP \n heartbeat arrives. Send every 10s to stay well
        // within that window. Setting expectedPeriod=0 tells the server we don't want heartbeats
        // back, so Krossbow won't kill the connection when the server stays silent.
        heartBeat = HeartBeat(
            minSendPeriod = 10.seconds,
            expectedPeriod = 0.seconds,
        )
        heartBeatTolerance = HeartBeatTolerance(
            outgoingMargin = 2.seconds,
            incomingMargin = 0.seconds,
        )
        connectionTimeout = 30.seconds
    }

    // coerceInputValues maps unrecognised enum values (e.g. new backend event types) to the
    // declared default (EventType.UNKNOWN) instead of throwing and dropping the event.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun connect() {
        if (_connectionState.value is ConnectionState.Connected) {
            EventLogger.w("EventManager", "Already connected to workspace: $workspaceId")
            return
        }
        shouldReconnect = true
        reconnectionAttempts = 0
        performConnection()
    }

    private suspend fun performConnection() {
        _connectionState.value = ConnectionState.Connecting
        EventLogger.i("EventManager", "Connecting to workspace: $workspaceId (attempt ${reconnectionAttempts + 1})")

        try {
            val wsUrl = ApiUrlBuilder.wsUrl("ws")

            var token = tokenProvider()
            if (token.isBlank()) {
                EventLogger.w("EventManager", "No access token, attempting refresh")
                val refreshed = tokenRefresher()
                if (refreshed) token = tokenProvider()
                if (token.isBlank()) throw IllegalStateException("No access token available after refresh")
            }

            val urlWithToken = buildString {
                append(wsUrl)
                append("?token=")
                append(token)
                append("&workspaceId=")
                append(workspaceId.encodeURLParameter())
            }
            EventLogger.d("EventManager", "Connecting to: $wsUrl")

            stompSession = stompClient.connect(urlWithToken)
            _connectionState.value = ConnectionState.Connected(stompSession.toString())
            reconnectionAttempts = 0
            EventLogger.i("EventManager", "✅ Connected to workspace: $workspaceId")

            subscribeToWorkspaceEvents()
            startHeartbeat()

        } catch (e: Exception) {
            val authFailure = e.isAuthenticationFailure()
            EventLogger.e("EventManager", "Connection failed for workspace: $workspaceId", e)
            _connectionState.value = ConnectionState.Error(message = "Connection failed: ${e.message}", exception = e)
            cleanup()
            if (shouldReconnect) scheduleReconnection(authFailure)
        }
    }

    private fun scheduleReconnection(refreshToken: Boolean = false) {
        reconnectionAttempts++
        val delay = min(baseReconnectionDelay * (1 shl (reconnectionAttempts - 1)), maxReconnectionDelay)
        EventLogger.i("EventManager", "Scheduling reconnection in ${delay}ms (attempt $reconnectionAttempts)")

        reconnectionJob = scope.launch {
            delay(delay)
            if (shouldReconnect && isActive) {
                if (refreshToken) {
                    EventLogger.i("EventManager", "🔄 Refreshing token before reconnection")
                    val refreshed = tokenRefresher()
                    if (!refreshed) EventLogger.w("EventManager", "⚠️ Token refresh failed, attempting reconnection anyway")
                }
                performConnection()
            }
        }
    }

    private suspend fun subscribeToWorkspaceEvents() {
        val session = stompSession ?: run {
            EventLogger.w("EventManager", "No STOMP session available for subscription")
            return
        }

        try {
            val destination = "/topic/workspace.events.$workspaceId"
            EventLogger.i("EventManager", "Subscribing to: $destination")

            subscriptionJob = scope.launch {
                try {
                    val subscription = session.subscribe(StompSubscribeHeaders(destination))
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
                    _connectionState.value = ConnectionState.Error(message = "Connection dropped: ${e.message}", exception = e)
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
            _connectionState.value = ConnectionState.Error(message = "Subscription failed: ${e.message}", exception = e)
            if (shouldReconnect) {
                cleanup()
                scheduleReconnection(authFailure)
            }
        }
    }

    private suspend fun handleIncomingEvent(event: WorkspaceEvent) {
        if (event.isFromDevice(deviceId)) {
            EventLogger.d("EventManager", "Skipping own event: ${event.eventType} for ${event.entityType}:${event.entityId}")
            return
        }
        EventLogger.i("EventManager", "📨 Received event: ${event.eventType} for ${event.entityType}:${event.entityId} (seq: ${event.sequenceNumber})")
        _events.emit(event)
    }

    private fun startHeartbeat() {
        heartbeatJob = scope.launch {
            while (isActive && stompSession != null) {
                try {
                    stompSession?.send(StompSendHeaders("/app/heartbeat"), FrameBody.Text(""))
                    EventLogger.d("EventManager", "💓 Heartbeat sent")
                } catch (e: Exception) {
                    EventLogger.w("EventManager", "Heartbeat failed", e)
                }
                delay(heartbeatIntervalMillis)
            }
        }
    }

    override suspend fun disconnect() {
        EventLogger.i("EventManager", "Disconnecting from workspace: $workspaceId")
        shouldReconnect = false
        reconnectionJob?.cancel()
        reconnectionJob = null
        cleanup()
        scope.cancel()
        _connectionState.value = ConnectionState.Disconnected
    }

    /** Non-suspending cleanup used by [WorkspaceClosable] on workspace switch. */
    override fun close() {
        shouldReconnect = false
        reconnectionJob?.cancel()
        heartbeatJob?.cancel()
        subscriptionJob?.cancel()
        stompSession = null
        scope.cancel()
        _connectionState.value = ConnectionState.Disconnected
    }

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

    override fun isConnected(): Boolean = _connectionState.value is ConnectionState.Connected

    override fun getEventsForEntityType(entityType: String): SharedFlow<WorkspaceEvent> = events

    override fun getEventsByType(vararg eventTypes: EventType): SharedFlow<WorkspaceEvent> = events

    private fun Throwable.isAuthenticationFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            when (current) {
                is ResponseException -> {
                    val status = current.response.status
                    if (status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden) return true
                }
                else -> if (current.message?.contains("401", ignoreCase = false) == true) return true
            }
            current = current.cause
        }
        return false
    }
}
