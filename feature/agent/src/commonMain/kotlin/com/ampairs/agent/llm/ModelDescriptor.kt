package com.ampairs.agent.llm

/** The role a model plays in the pipeline (mirrors the plan's role-split catalog). */
enum class ModelRole { INTENT, CHAT, FALLBACK }

/**
 * A downloadable on-device model the [LlmEngine] can load (the spec's `ModelAsset`). Lives in the
 * `ModelCatalog`; `ModelManager` (T032) downloads it and gates by [estimatedPeakMemoryBytes].
 */
data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val role: ModelRole,
    /** Which [LlmBackend] runs this model, e.g. "litert-lm" or "llamacpp". */
    val backendId: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val estimatedPeakMemoryBytes: Long,
    /** Optional lowercase-hex SHA-256; when set, `ModelManager` verifies the file after download. */
    val sha256: String? = null,
    /** Server-flagged preferred model for its tier (from the manifest); breaks ties in selection. */
    val recommended: Boolean = false,
    /**
     * When non-null the download is an archive that's extracted into a **directory** named [fileName]
     * (the model is then a folder, not a file) — see [ModelManager]. null = a plain single file (the
     * default for LLM/Whisper models). [downloadUrl] points at the archive; [sizeBytes]/[sha256] still
     * validate the downloaded archive before extraction.
     */
    val archiveFormat: ArchiveFormat? = null,
    val defaultParams: LlmParams = LlmParams(),
    /**
     * Cloud transport adapter id for online models (`backendId == "cloud"`) — selects the
     * [com.ampairs.agent.llm.transport.LlmTransport] that owns the wire protocol. `null` for
     * on-device models (which load a local file via their native backend).
     */
    val transportId: String? = null,
    /**
     * Provider-side model id the cloud transport sends (e.g. `"claude-sonnet-4-6"`). `null` for
     * on-device models.
     */
    val remoteModelId: String? = null,
) {
    /**
     * True when this model runs **server-side** via a cloud transport — no local file to download and
     * no device-RAM gate (the inference happens in the cloud). Discriminated by [backendId].
     */
    val isCloud: Boolean get() = backendId == BACKEND_CLOUD

    companion object {
        /** [backendId] value that routes a model through the cloud transport tier. */
        const val BACKEND_CLOUD: String = "cloud"
    }
}
