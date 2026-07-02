package com.ampairs.agent.llm

/**
 * Best engine id per platform (overridable by `AssistantConfig`). `ProviderRegistry` (T023) reads
 * this when the config doesn't pin an engine. Desktop defaults to llama.cpp until LiteRT-LM's
 * Kotlin/JVM desktop libs are confirmed (T031).
 */
expect object PlatformDefaults {
    val primaryEngineId: String
    val fallbackEngineId: String
}
