package com.ampairs.agent.llm

actual object PlatformDefaults {
    // Desktop now ships a LiteRT-LM engine (litertlm-jvm, GPU) — prefer it. llama.cpp stays the
    // declared fallback id (no desktop engine yet, so it resolves to rule-based until added).
    actual val primaryEngineId: String = "litert-lm"
    actual val fallbackEngineId: String = "llamacpp"
}
