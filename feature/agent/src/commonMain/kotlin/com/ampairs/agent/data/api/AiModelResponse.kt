package com.ampairs.agent.data.api

import com.ampairs.agent.data.db.entity.AiModelEntity
import com.ampairs.agent.llm.LlmParams
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelRole
import com.ampairs.common.ApiUrlBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One downloadable on-device model from the backend manifest (`GET /api/agent/v1/models`).
 * Mirrors the backend `AiModelResponse` (snake_case via [SerialName]). The upstream source is
 * deliberately opaque — the app downloads the bytes through the authenticated proxy
 * (`GET /api/agent/v1/models/{id}/download`), so there is no direct download URL in the manifest.
 */
@Serializable
data class AiModelResponse(
    val id: String,
    val name: String,
    val family: String = "",
    @SerialName("parameter_label") val parameterLabel: String = "",
    /** Pipeline slot from the manifest: "INTENT" (tool-caller) / "CHAT" / "FALLBACK". */
    val role: String = "CHAT",
    @SerialName("file_name") val fileName: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    val sha256: String? = null,
    @SerialName("required_ram_mb") val requiredRamMb: Int,
    @SerialName("backend_id") val backendId: String,
    val platforms: Set<String> = emptySet(),
    val recommended: Boolean = false,
)

/** Parse the manifest's role string to [ModelRole]; unknown/blank ⇒ CHAT (a safe, selectable default). */
fun roleFromManifest(role: String): ModelRole =
    when (role.trim().uppercase()) {
        "INTENT" -> ModelRole.INTENT
        "FALLBACK" -> ModelRole.FALLBACK
        else -> ModelRole.CHAT
    }

/**
 * Map a manifest entry onto the engine-facing [ModelDescriptor]. The manifest carries the file, RAM
 * gate, and pipeline [role] (so the tool-caller is wired as INTENT, not auto-picked as a chat model);
 * sampling params are assigned app-side, and [ModelDescriptor.downloadUrl] is the backend proxy URL.
 */
fun AiModelResponse.toDescriptor(): ModelDescriptor = ModelDescriptor(
    id = id,
    displayName = name,
    role = roleFromManifest(role),
    backendId = backendId,
    fileName = fileName,
    downloadUrl = ApiUrlBuilder.agentUrl("v1/models/$id/download"),
    sizeBytes = sizeBytes,
    // Manifest reports the minimum device RAM (MB); the selector gates on this as the peak footprint.
    estimatedPeakMemoryBytes = requiredRamMb.toLong() * 1_000_000L,
    sha256 = sha256,
    recommended = recommended,
    // Manifest models are chat models by default (roleFromManifest), so give them the larger
    // context window; the intent tool-caller keeps the LlmParams default (2048).
    defaultParams = LlmParams(
        temperature = 0.3f,
        topK = 40,
        topP = 0.95f,
        maxTokens = 512,
        contextTokens = if (roleFromManifest(role) == ModelRole.INTENT) 2048 else 4096,
    ),
)

/** Persist a manifest entry to the app-scoped catalog DB (platforms comma-joined). */
fun AiModelResponse.toEntity(): AiModelEntity = AiModelEntity(
    id = id,
    name = name,
    family = family,
    parameterLabel = parameterLabel,
    role = role,
    fileName = fileName,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    requiredRamMb = requiredRamMb,
    backendId = backendId,
    platforms = platforms.joinToString(","),
    recommended = recommended,
)

/** Rebuild the manifest entry from its persisted row, so [toDescriptor] stays the single mapper. */
fun AiModelEntity.toResponse(): AiModelResponse = AiModelResponse(
    id = id,
    name = name,
    family = family,
    parameterLabel = parameterLabel,
    role = role,
    fileName = fileName,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    requiredRamMb = requiredRamMb,
    backendId = backendId,
    platforms = platforms.split(",").filter { it.isNotBlank() }.toSet(),
    recommended = recommended,
)
