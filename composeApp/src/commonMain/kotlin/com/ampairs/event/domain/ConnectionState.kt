package com.ampairs.event.domain

/**
 * Represents the current state of WebSocket/STOMP connection.
 * Used for UI indicators and connection management.
 */
sealed class ConnectionState {
    /**
     * Not connected to WebSocket
     */
    data object Disconnected : ConnectionState()

    /**
     * Attempting to establish WebSocket connection
     */
    data object Connecting : ConnectionState()

    /**
     * Successfully connected and subscribed to workspace events
     * @param sessionId STOMP session identifier
     */
    data class Connected(val sessionId: String) : ConnectionState()

    /**
     * Connection or subscription error occurred
     * @param message Error description
     * @param exception Optional exception that caused the error
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ConnectionState()

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean = this is Connected

    /**
     * Check if in error state
     */
    fun isError(): Boolean = this is Error

    /**
     * Get connection status as string for logging
     */
    fun getStatusString(): String = when (this) {
        is Disconnected -> "Disconnected"
        is Connecting -> "Connecting"
        is Connected -> "Connected (session: $sessionId)"
        is Error -> "Error: $message"
    }
}
