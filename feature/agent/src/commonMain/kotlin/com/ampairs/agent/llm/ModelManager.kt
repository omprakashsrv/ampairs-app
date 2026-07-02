package com.ampairs.agent.llm

import kotlinx.coroutines.flow.StateFlow

/**
 * Downloads, tracks, and removes on-device model files (T032). Install state is **filesystem truth**:
 * a model is [ModelInstallStatus.Installed] iff its file exists at the expected size (+ optional
 * checksum), so state is self-healing across restarts with no separate persistence.
 *
 * Engine-agnostic: [LiteRtLmEngine] finds a downloaded file by the [ModelStorage.DIR_NAME] convention,
 * so no `LlmEngine.load` signature change is needed. The generic downloader supports an optional
 * Bearer token for gated/self-hosted URLs.
 */
interface ModelManager {
    /** Per-model install state keyed by [ModelDescriptor.id]. Missing key ⇒ [ModelInstallStatus.NotInstalled]. */
    val statuses: StateFlow<Map<String, ModelInstallStatus>>

    /** Current status for [modelId] (defaults to [ModelInstallStatus.NotInstalled]). */
    fun statusOf(modelId: String): ModelInstallStatus

    /** Rescan the models directory and reconcile [statuses] with what's actually on disk. */
    suspend fun refresh()

    /** Start (or resume) downloading [model]. No-op if already downloading. [authToken] → `Bearer` header. */
    fun download(model: ModelDescriptor, authToken: String? = null)

    /** Cancel an in-progress download (keeps the partial file for later resume). */
    fun cancel(modelId: String)

    /** Delete the installed (and any partial) file for [model]. */
    suspend fun delete(model: ModelDescriptor)

    /** Absolute path to the verified local file, or null when not installed. */
    fun localPathOrNull(model: ModelDescriptor): String?
}
