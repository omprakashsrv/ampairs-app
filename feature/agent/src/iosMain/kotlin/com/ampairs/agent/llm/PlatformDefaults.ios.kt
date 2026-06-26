package com.ampairs.agent.llm

actual object PlatformDefaults {
    actual val primaryEngineId: String = "litert-lm"
    actual val fallbackEngineId: String = "llamacpp"
}
