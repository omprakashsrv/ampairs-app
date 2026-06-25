package com.ampairs.agent.llm

/**
 * The role-split, swappable model catalog (plan §2). Mirrors the Google AI Edge Gallery's
 * `model_allowlist.json`: a tiny tool-caller for intent→action + larger chat models (Gemma 3n,
 * Qwen2.5) — all LiteRT-LM (`.litertlm`). `ModelManager` downloads entries from
 * [ModelDescriptor.downloadUrl]; `ProviderRegistry` + `DeviceCapability` select by [ModelRole]/RAM.
 *
 * Gemma 3n + Qwen2.5 file names, sizes, params and HuggingFace URLs come from the AI Edge LiteRT-LM
 * allowlist (gated repos — the downloader supplies the HF auth token). The intent model's params are
 * kept deterministic (temp 0 / top-k 1) for constrained JSON intent resolution.
 */
object ModelCatalog {

    /** Intent → action tool-calling (default). Tiny + fast; runs on low-end devices. */
    val FUNCTION_GEMMA_270M = ModelDescriptor(
        id = "function-gemma-270m",
        displayName = "FunctionGemma 270M",
        role = ModelRole.INTENT,
        backendId = "litert-lm",
        fileName = "tiny_garden.litertlm",
        downloadUrl = "https://huggingface.co/google/functiongemma-270m-it/resolve/main/tiny_garden.litertlm",
        sizeBytes = 288_440_320L,
        estimatedPeakMemoryBytes = 700_000_000L,
        defaultParams = LlmParams(temperature = 0.0f, topK = 1, maxTokens = 256),
    )

    /** Conversational answers / query phrasing — low-RAM tier. Multimodal (text/vision/audio). */
    val GEMMA_3N_E2B = ModelDescriptor(
        id = "gemma-3n-e2b",
        displayName = "Gemma 3n E2B",
        role = ModelRole.CHAT,
        backendId = "litert-lm",
        fileName = "gemma-3n-E2B-it-int4.litertlm",
        downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm",
        sizeBytes = 3_655_827_456L,
        estimatedPeakMemoryBytes = 4_100_000_000L,
        defaultParams = LlmParams(temperature = 1.0f, topK = 64, topP = 0.95f, maxTokens = 4096),
    )

    /** Conversational answers — higher-RAM tier. Multimodal (text/vision/audio). */
    val GEMMA_3N_E4B = ModelDescriptor(
        id = "gemma-3n-e4b",
        displayName = "Gemma 3n E4B",
        role = ModelRole.CHAT,
        backendId = "litert-lm",
        fileName = "gemma-3n-E4B-it-int4.litertlm",
        downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm/resolve/main/gemma-3n-E4B-it-int4.litertlm",
        sizeBytes = 4_919_541_760L,
        estimatedPeakMemoryBytes = 5_500_000_000L,
        defaultParams = LlmParams(temperature = 1.0f, topK = 64, topP = 0.95f, maxTokens = 4096),
    )

    /**
     * Qwen2.5 1.5B Instruct — a LiteRT-LM chat alternative (text-only), from the AI Edge
     * `litert-community` allowlist. Selectable in the model manager alongside the Gemma chat models.
     */
    val QWEN2_5_1_5B = ModelDescriptor(
        id = "qwen2.5-1.5b-instruct",
        displayName = "Qwen2.5 1.5B Instruct",
        role = ModelRole.FALLBACK,
        backendId = "litert-lm",
        fileName = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
        sizeBytes = 1_597_931_520L,
        estimatedPeakMemoryBytes = 1_900_000_000L,
        defaultParams = LlmParams(temperature = 0.7f, topK = 20, topP = 0.8f, maxTokens = 4096),
    )

    val all: List<ModelDescriptor> = listOf(FUNCTION_GEMMA_270M, GEMMA_3N_E2B, GEMMA_3N_E4B, QWEN2_5_1_5B)

    fun byId(id: String): ModelDescriptor? = all.firstOrNull { it.id == id }

    fun byRole(role: ModelRole): List<ModelDescriptor> = all.filter { it.role == role }

    fun byBackend(backendId: String): List<ModelDescriptor> = all.filter { it.backendId == backendId }
}
