package com.ampairs.agent.data.api

import com.ampairs.common.model.Response

/** Uploads opt-in chat telemetry to the backend (`POST /agent/v1/chat-logs`). */
interface ChatLogApi {
    suspend fun upload(logs: List<ChatLogRequest>): Response<ChatLogUploadResponse>
}
