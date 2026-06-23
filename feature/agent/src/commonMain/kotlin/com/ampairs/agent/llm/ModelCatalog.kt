package com.ampairs.agent.llm

/**
 * The role-split, swappable model catalog (plan §2). Mirrors the Google AI Edge Gallery's
 * `model_allowlist.json`: a tiny tool-caller for intent→action + a larger chat model, plus a
 * llama.cpp fallback. `ModelManager` (T032) downloads entries from [ModelDescriptor.downloadUrl];
 * `ProviderRegistry` (T023) + `DeviceCapability` (T022) select by [ModelRole] and RAM.
 *
 * NOTE: `downloadUrl`/`fileName`/byte sizes are provisional and confirmed at T031 (model availability
 * verification) before the downloader (T032) ships — nothing downloads yet, so they're inert here.
 */
object ModelCatalog {

    /** Intent → action tool-calling (default). Tiny + fast; runs on low-end devices. */
    val FUNCTION_GEMMA_270M = ModelDescriptor(
        id = "function-gemma-270m",
        displayName = "FunctionGemma 270M",
        role = ModelRole.INTENT,
        backendId = "litert-lm",
        fileName = "function-gemma-270m.litertlm",
        downloadUrl = "https://huggingface.co/google/function-gemma-270m", // provisional — confirm T031
        sizeBytes = 300_000_000L,
        estimatedPeakMemoryBytes = 700_000_000L,
        defaultParams = LlmParams(temperature = 0.0f, topK = 1, maxTokens = 256),
    )

    /** Conversational answers / query phrasing — low-RAM tier (3–6 GB). */
    val GEMMA_3N_E2B = ModelDescriptor(
        id = "gemma-3n-e2b",
        displayName = "Gemma 3n E2B",
        role = ModelRole.CHAT,
        backendId = "litert-lm",
        fileName = "gemma-3n-e2b.litertlm",
        downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm", // provisional — confirm T031
        sizeBytes = 3_000_000_000L,
        estimatedPeakMemoryBytes = 3_500_000_000L,
        defaultParams = LlmParams(temperature = 0.3f, topK = 40, topP = 0.95f, maxTokens = 512),
    )

    /** Conversational answers — higher-RAM tier (≥ 6 GB). */
    val GEMMA_3N_E4B = ModelDescriptor(
        id = "gemma-3n-e4b",
        displayName = "Gemma 3n E4B",
        role = ModelRole.CHAT,
        backendId = "litert-lm",
        fileName = "gemma-3n-e4b.litertlm",
        downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm", // provisional — confirm T031
        sizeBytes = 4_400_000_000L,
        estimatedPeakMemoryBytes = 6_000_000_000L,
        defaultParams = LlmParams(temperature = 0.3f, topK = 40, topP = 0.95f, maxTokens = 512),
    )

    /** llama.cpp fallback / parity (Desktop, or when LiteRT-LM is unavailable). Needs ≥ 6 GB. */
    val QWEN2_5_3B = ModelDescriptor(
        id = "qwen2.5-3b-instruct",
        displayName = "Qwen2.5 3B Instruct (GGUF)",
        role = ModelRole.FALLBACK,
        backendId = "llamacpp",
        fileName = "qwen2.5-3b-instruct-q4_k_m.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF", // provisional — confirm T031
        sizeBytes = 2_000_000_000L,
        estimatedPeakMemoryBytes = 4_000_000_000L,
        defaultParams = LlmParams(temperature = 0.2f, topK = 40, topP = 0.9f, maxTokens = 512),
    )

    val all: List<ModelDescriptor> = listOf(FUNCTION_GEMMA_270M, GEMMA_3N_E2B, GEMMA_3N_E4B, QWEN2_5_3B)

    fun byId(id: String): ModelDescriptor? = all.firstOrNull { it.id == id }

    fun byRole(role: ModelRole): List<ModelDescriptor> = all.filter { it.role == role }

    fun byBackend(backendId: String): List<ModelDescriptor> = all.filter { it.backendId == backendId }
}
