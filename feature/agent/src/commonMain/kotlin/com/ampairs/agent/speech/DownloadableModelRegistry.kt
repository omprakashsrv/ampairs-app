package com.ampairs.agent.speech

import com.ampairs.agent.llm.ModelInstallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** A selectable downloadable model shown in Assistant settings (engine-agnostic). */
data class DownloadableModelOption(val id: String, val label: String, val sizeBytes: Long)

/**
 * Engine-agnostic contract for an STT engine's downloadable-model catalog (selection + per-model
 * install state, layered over the shared [com.ampairs.agent.llm.ModelManager]). Whisper and Vosk both
 * implement it; the chat settings UI drives whichever engine is selected through this interface, so a
 * new downloadable-model STT engine needs **zero** UI changes — just a registry keyed by its adapter id
 * in the DI-provided `Map<String, DownloadableModelRegistry>`.
 */
interface DownloadableModelRegistry {
    /** False when this engine has no models on the current platform (the settings section hides). */
    val hasModels: Boolean

    /** The selectable models (id, label, size) for the settings picker. */
    val options: List<DownloadableModelOption>

    /** Per-model install state keyed by model id (shared model-manager StateFlow). */
    val statuses: StateFlow<Map<String, ModelInstallStatus>>

    /** The persisted selection (null → catalog default). */
    fun selectedId(): Flow<String?>
    suspend fun setSelected(id: String)

    /** The id actually in effect for a persisted value (persisted → recommended → first). */
    fun resolveSelectedId(selectedId: String?): String?

    /** Ids whose model is present on disk (filesystem truth, for the picker). */
    suspend fun installedIds(): Set<String>

    /** Start downloading [id] (default: the active model) via the shared resumable downloader. */
    fun download(id: String? = null)
    suspend fun delete(id: String)

    /** Local path of the active model (file or extracted directory), or null when not downloaded. */
    suspend fun selectedModelPath(): String?

    /** Kick a download of the active model if it isn't installed (used by the engine on first use). */
    suspend fun ensureSelectedDownloaded()
}
