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
    val defaultParams: LlmParams = LlmParams(),
)
