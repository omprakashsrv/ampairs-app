package com.ampairs.agent.speech.whisper

import com.ampairs.agent.llm.ModelManager
import com.ampairs.agent.speech.ModelManagerBackedRegistry
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

/**
 * Whisper's downloadable-model registry (selection + install state over the shared [ModelManager]).
 * App-scoped so [WhisperSttEngine] and the settings UI share one source of truth. Empty
 * [WhisperModelSet] (a platform with no Whisper engine) ⇒ [hasModels] is false and it's inert.
 *
 * All generic behaviour lives in [ModelManagerBackedRegistry]; only persistence is Whisper-specific
 * (legacy dedicated prefs key, kept for back-compat with already-installed apps).
 */
@Inject
@SingleIn(AppScope::class)
class WhisperModelRegistry(
    modelSet: WhisperModelSet,
    modelManager: ModelManager,
    private val prefs: AppPreferencesDataStore,
) : ModelManagerBackedRegistry(modelSet.models, modelManager) {
    override fun selectedId(): Flow<String?> = prefs.getSelectedWhisperModelId()
    override suspend fun setSelected(id: String) = prefs.setSelectedWhisperModelId(id)
}
