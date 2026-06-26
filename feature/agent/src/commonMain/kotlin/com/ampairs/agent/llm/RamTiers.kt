package com.ampairs.agent.llm

/**
 * RAM → model-tier policy (plan Open-Q#4, FR-012). Pure & unit-tested; the actual RAM value comes
 * from [DeviceCapability.totalRamBytes].
 *
 *  - `< 3 GB`  → rule-based only (no on-device LLM)
 *  - `3–6 GB`  → Gemma 3n E2B (low-RAM chat tier)
 *  - `≥ 6 GB`  → Gemma 3n E4B
 *
 * The intent model (FunctionGemma-270m) is tiny and runs whenever the LLM tier is enabled.
 */
object RamTiers {
    const val MIN_LLM_BYTES: Long = 3L * 1024 * 1024 * 1024
    const val E4B_MIN_BYTES: Long = 6L * 1024 * 1024 * 1024

    fun canRunLlm(totalRamBytes: Long): Boolean = totalRamBytes >= MIN_LLM_BYTES

    /** Recommended chat model id for this RAM, or null when only rule-based mode fits. */
    fun recommendedChatModelId(totalRamBytes: Long): String? = when {
        totalRamBytes < MIN_LLM_BYTES -> null
        totalRamBytes < E4B_MIN_BYTES -> ModelCatalog.GEMMA_3N_E2B.id
        else -> ModelCatalog.GEMMA_3N_E4B.id
    }
}
