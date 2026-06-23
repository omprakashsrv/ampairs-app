package com.ampairs.agent.llm

actual object PlatformDefaults {
    // LiteRT-LM Kotlin/JVM on desktop is unconfirmed (T031) — default to llama.cpp until verified.
    actual val primaryEngineId: String = "llamacpp"
    actual val fallbackEngineId: String = "llamacpp"
}
