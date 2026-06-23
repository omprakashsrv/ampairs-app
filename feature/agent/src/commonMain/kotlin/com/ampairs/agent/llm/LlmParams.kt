package com.ampairs.agent.llm

/** Sampling/generation parameters for an [LlmEngine] (engine-agnostic). */
data class LlmParams(
    val temperature: Float = 0.0f,
    val topK: Int = 1,
    val topP: Float = 1.0f,
    val maxTokens: Int = 512,
)
