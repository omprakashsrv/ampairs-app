package com.ampairs.agent.data.api

import com.ampairs.agent.llm.LlmParams
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server-seeded LLM model catalog entry, pulled from `GET /api/workspace/v1/llm-models/catalog`.
 * Mirrors the backend `LlmModelResponse` (snake_case). The catalog is server-driven (like the module
 * catalog) — the app never hardcodes model URLs; [toDescriptor] maps this onto the engine-facing
 * [ModelDescriptor].
 */
@Serializable
data class RemoteModelDescriptor(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val role: ModelRole,
    @SerialName("backend_id") val backendId: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("download_url") val downloadUrl: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("estimated_peak_memory_bytes") val estimatedPeakMemoryBytes: Long,
    val sha256: String? = null,
    val temperature: Float = 0.0f,
    @SerialName("top_k") val topK: Int = 1,
    @SerialName("top_p") val topP: Float = 1.0f,
    @SerialName("max_tokens") val maxTokens: Int = 512,
)

fun RemoteModelDescriptor.toDescriptor(): ModelDescriptor = ModelDescriptor(
    id = id,
    displayName = displayName,
    role = role,
    backendId = backendId,
    fileName = fileName,
    downloadUrl = downloadUrl,
    sizeBytes = sizeBytes,
    estimatedPeakMemoryBytes = estimatedPeakMemoryBytes,
    sha256 = sha256,
    defaultParams = LlmParams(
        temperature = temperature,
        topK = topK,
        topP = topP,
        maxTokens = maxTokens,
    ),
)
