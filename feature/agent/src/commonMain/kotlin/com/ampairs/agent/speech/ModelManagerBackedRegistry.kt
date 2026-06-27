package com.ampairs.agent.speech

import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelInstallStatus
import com.ampairs.agent.llm.ModelManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Shared [DownloadableModelRegistry] behaviour for any STT engine whose models are downloaded through
 * the common [ModelManager] (resumable download, checksum, progress, and — for directory models —
 * archive extraction). Subclasses supply only the model list and where the selection is persisted
 * ([selectedId]/[setSelected]); everything else (resolution, install state, download/delete) is generic.
 *
 * This is what keeps the system pluggable: Whisper and Vosk are ~10-line subclasses, and a new engine
 * is the same.
 */
abstract class ModelManagerBackedRegistry(
    private val models: List<ModelDescriptor>,
    private val modelManager: ModelManager,
) : DownloadableModelRegistry {

    override val hasModels: Boolean = models.isNotEmpty()

    override val options: List<DownloadableModelOption> =
        models.map { DownloadableModelOption(it.id, it.displayName, it.sizeBytes) }

    override val statuses: StateFlow<Map<String, ModelInstallStatus>> = modelManager.statuses

    override fun resolveSelectedId(selectedId: String?): String? = resolve(selectedId)?.id

    override suspend fun installedIds(): Set<String> =
        models.filter { modelManager.localPathOrNull(it) != null }.map { it.id }.toSet()

    override suspend fun selectedModelPath(): String? {
        val model = resolve(selectedId().first()) ?: return null
        return modelManager.localPathOrNull(model)
    }

    override fun download(id: String?) {
        val model = if (id != null) models.firstOrNull { it.id == id }
        else models.firstOrNull { it.recommended } ?: models.firstOrNull()
        model?.let { modelManager.download(it) }
    }

    override suspend fun delete(id: String) {
        models.firstOrNull { it.id == id }?.let { modelManager.delete(it) }
    }

    override suspend fun ensureSelectedDownloaded() {
        val model = resolve(selectedId().first()) ?: return
        if (modelManager.localPathOrNull(model) == null) modelManager.download(model)
    }

    /** persisted id → recommended → first; null when there are no models. */
    protected fun resolve(selectedId: String?): ModelDescriptor? =
        models.firstOrNull { it.id == selectedId }
            ?: models.firstOrNull { it.recommended }
            ?: models.firstOrNull()
}
