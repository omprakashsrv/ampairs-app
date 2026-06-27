package com.ampairs.agent.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One assistant turn uploaded as opt-in telemetry (user message + assistant reply). Matches the
 * backend `POST /agent/v1/chat-logs` contract (snake_case). The server stamps the workspace
 * (X-Workspace-ID) and the authenticated user — those are never sent from here.
 */
@Serializable
data class ChatLogRequest(
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("user_message") val userMessage: String,
    @SerialName("assistant_message") val assistantMessage: String,
    @SerialName("model_id") val modelId: String? = null,
    @SerialName("intent") val intent: String? = null,
    @SerialName("module_name") val moduleName: String? = null,
    @SerialName("action_type") val actionType: String? = null,
    /** When the turn happened on-device, epoch millis UTC. */
    @SerialName("client_timestamp") val clientTimestamp: Long? = null,
)

/** Server's acknowledgement of an upload batch. */
@Serializable
data class ChatLogUploadResponse(
    @SerialName("saved") val saved: Int = 0,
)
