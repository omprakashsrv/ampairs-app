package com.ampairs.agent.llm

/**
 * Resolves the directory that holds downloaded on-device model files. Platform-provided via Metro
 * (`@ContributesTo(AppScope)`) so the [ModelManager] (commonMain) can do filesystem IO with
 * kotlinx-io without leaking platform APIs.
 *
 * The Android path MUST match [LiteRtLmEngine]'s `modelDir` (`filesDir/agent_models`) so a downloaded
 * file is found by the engine at load time. The shared [DIR_NAME] keeps both sides in sync.
 */
interface ModelStorage {
    /** Absolute path to the directory containing model files (created on demand by the manager). */
    fun modelsDirectoryPath(): String

    companion object {
        const val DIR_NAME: String = "agent_models"
    }
}
