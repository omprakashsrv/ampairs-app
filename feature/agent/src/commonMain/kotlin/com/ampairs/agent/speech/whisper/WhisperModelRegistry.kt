package com.ampairs.agent.speech.whisper

import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelInstallStatus
import com.ampairs.agent.llm.ModelManager
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/** A selectable Whisper model shown in Assistant settings (id persisted via [AppPreferencesDataStore]). */
data class WhisperModelOption(val id: String, val label: String, val sizeBytes: Long)

/**
 * Owns the platform's Whisper model list (from the DI-provided [WhisperModelSet]) plus selection and
 * download state, layered over the existing [ModelManager]. App-scoped, so [WhisperSttEngine] and the
 * settings UI share one source of truth. Empty [WhisperModelSet] (a platform with no Whisper engine
 * yet) ⇒ [hasModels] is false and the registry is inert.
 */
@Inject
@SingleIn(AppScope::class)
class WhisperModelRegistry(
    modelSet: WhisperModelSet,
    private val modelManager: ModelManager,
    private val prefs: AppPreferencesDataStore,
) {
    private val models: List<ModelDescriptor> = modelSet.models

    val hasModels: Boolean = models.isNotEmpty()
    val options: List<WhisperModelOption> =
        models.map { WhisperModelOption(it.id, it.displayName, it.sizeBytes) }

    /** Per-model install state keyed by model id (reuses the shared model-manager StateFlow). */
    val statuses: StateFlow<Map<String, ModelInstallStatus>> = modelManager.statuses

    fun selectedId(): Flow<String?> = prefs.getSelectedWhisperModelId()
    suspend fun setSelected(id: String) = prefs.setSelectedWhisperModelId(id)

    /** The chosen model (persisted id → recommended → first), or null when the platform has none. */
    private fun resolve(selectedId: String?): ModelDescriptor? =
        models.firstOrNull { it.id == selectedId }
            ?: models.firstOrNull { it.recommended }
            ?: models.firstOrNull()

    /** Local file path of the active model, or null when it isn't downloaded yet. */
    suspend fun selectedModelPath(): String? {
        val model = resolve(prefs.getSelectedWhisperModelId().first()) ?: return null
        return modelManager.localPathOrNull(model)
    }

    /** Start downloading [id] (default: the active model) via the shared resumable downloader. */
    fun download(id: String? = null) {
        val model = if (id != null) models.firstOrNull { it.id == id } else models.firstOrNull { it.recommended } ?: models.firstOrNull()
        model?.let { modelManager.download(it) }
    }

    suspend fun delete(id: String) {
        models.firstOrNull { it.id == id }?.let { modelManager.delete(it) }
    }

    /** Kick a download of the active model if it isn't installed (used by the engine on first use). */
    suspend fun ensureSelectedDownloaded() {
        val model = resolve(prefs.getSelectedWhisperModelId().first()) ?: return
        if (modelManager.localPathOrNull(model) == null) modelManager.download(model)
    }
}
