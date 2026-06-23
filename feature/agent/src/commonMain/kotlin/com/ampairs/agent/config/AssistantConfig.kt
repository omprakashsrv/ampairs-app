package com.ampairs.agent.config

import com.ampairs.agent.llm.ModelCatalog

/**
 * Runtime overrides for the assistant pipeline — the user/dev-settable layer on top of
 * `PlatformDefaults`. `ProviderRegistry` (T023) reads it to pick the engine + models; null/default
 * values mean "use the platform default". Persisted in the existing app DataStore (binding lands with
 * T023 — this is the shape + defaults, deliberately free of any DataStore dependency so it stays
 * pure/testable).
 *
 * [safeQueryEnabled] defaults **off** — the read-only SQL fallback (FR-016) is opt-in (gates T038).
 */
data class AssistantConfig(
    /** Engine id override (e.g. "litert-lm" / "llamacpp"); null → PlatformDefaults. */
    val engineId: String? = null,
    val intentModelId: String = ModelCatalog.FUNCTION_GEMMA_270M.id,
    val chatModelId: String = ModelCatalog.GEMMA_3N_E2B.id,
    val sttProviderId: String? = null,
    val ttsProviderId: String? = null,
    /** Master switch for the on-device LLM tier; false → rule-based resolver only. */
    val llmEnabled: Boolean = true,
    /** FR-016 read-only SQL fallback — opt-in, default off. */
    val safeQueryEnabled: Boolean = false,
    /** Read responses aloud (TTS) when available. */
    val ttsEnabled: Boolean = false,
) {
    companion object {
        val Default = AssistantConfig()
    }
}
