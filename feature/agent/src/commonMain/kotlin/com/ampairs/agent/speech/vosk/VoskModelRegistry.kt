package com.ampairs.agent.speech.vosk

import com.ampairs.agent.llm.ModelManager
import com.ampairs.agent.speech.ModelManagerBackedRegistry
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

/**
 * Vosk's downloadable-model registry. All generic behaviour comes from [ModelManagerBackedRegistry];
 * selection is persisted via the generic namespaced preference ([AppPreferencesDataStore.getSelectedModelId]
 * with [NAMESPACE]) — no Vosk-specific prefs key needed. Empty [VoskModelSet] (Android/iOS) ⇒ inert.
 */
@Inject
@SingleIn(AppScope::class)
class VoskModelRegistry(
    modelSet: VoskModelSet,
    modelManager: ModelManager,
    private val prefs: AppPreferencesDataStore,
) : ModelManagerBackedRegistry(modelSet.models, modelManager) {
    override fun selectedId(): Flow<String?> = prefs.getSelectedModelId(NAMESPACE)
    override suspend fun setSelected(id: String) = prefs.setSelectedModelId(NAMESPACE, id)

    private companion object {
        const val NAMESPACE = "vosk"
    }
}
