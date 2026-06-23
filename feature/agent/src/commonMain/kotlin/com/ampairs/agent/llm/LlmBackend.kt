package com.ampairs.agent.llm

/**
 * A self-describing engine adapter, contributed to a Metro multibinding (`Set<LlmBackend>`). The
 * `ProviderRegistry` picks one by [id] (from `PlatformDefaults`/`AssistantConfig`) and whether it
 * [supports] the chosen model, then lazily [create]s the engine. Adding a new engine = one adapter.
 */
interface LlmBackend {
    val id: String
    fun supports(model: ModelDescriptor): Boolean
    fun create(): LlmEngine
}
