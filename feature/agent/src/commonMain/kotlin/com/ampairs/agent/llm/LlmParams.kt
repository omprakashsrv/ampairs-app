package com.ampairs.agent.llm

/** Sampling/generation parameters for an [LlmEngine] (engine-agnostic). */
data class LlmParams(
    val temperature: Float = 0.0f,
    val topK: Int = 1,
    val topP: Float = 1.0f,
    val maxTokens: Int = 512,
    /**
     * Total token budget (prompt + generation) = the engine's KV-cache / context-window size, used
     * by LiteRT-LM's `EngineConfig.maxNumTokens`. Sized per model: a tiny intent model only needs to
     * fit its prompt, while a chat model needs room for the prompt + a longer reply. Engines clamp
     * this to a safe floor so a too-small value can never truncate the built prompt.
     */
    val contextTokens: Int = 2048,
)
