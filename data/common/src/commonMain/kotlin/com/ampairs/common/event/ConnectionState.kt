package com.ampairs.common.event

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val sessionId: String) : ConnectionState()
    data class Error(val message: String, val exception: Throwable? = null) : ConnectionState()

    fun isConnected() = this is Connected
    fun isError() = this is Error
}
