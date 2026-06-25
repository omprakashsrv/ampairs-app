package com.ampairs.agent.speech

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** A selectable speech adapter shown in the assistant settings (id is persisted). */
data class SpeechAdapterOption(val id: String, val label: String)

/** A registered speech-to-text engine option (one per available STT backend on this platform). */
class SttAdapterEntry(val id: String, val label: String, val engine: SpeechToText)

/** A registered text-to-speech engine option (one per available TTS backend on this platform). */
class TtsAdapterEntry(val id: String, val label: String, val engine: TextToSpeech)

/**
 * Routes STT/TTS to the user-selected adapter (settings, persisted in DataStore), falling back to the
 * first registered entry when nothing is chosen or the chosen id is gone. Each platform contributes
 * its available adapters as `List<SttAdapterEntry>` / `List<TtsAdapterEntry>` (Desktop may contribute
 * none today). App-scoped: one registry, engines created once; heavy engine init stays lazy in the
 * engine itself.
 */
@Inject
@SingleIn(AppScope::class)
class SpeechAdapterRegistry(
    private val sttEntries: List<SttAdapterEntry>,
    private val ttsEntries: List<TtsAdapterEntry>,
    private val prefs: AppPreferencesDataStore,
) {
    val sttOptions: List<SpeechAdapterOption> = sttEntries.map { SpeechAdapterOption(it.id, it.label) }
    val ttsOptions: List<SpeechAdapterOption> = ttsEntries.map { SpeechAdapterOption(it.id, it.label) }

    fun selectedSttId(): Flow<String?> = prefs.getSelectedSttAdapterId()
    fun selectedTtsId(): Flow<String?> = prefs.getSelectedTtsAdapterId()

    suspend fun setSttId(id: String) = prefs.setSelectedSttAdapterId(id)
    suspend fun setTtsId(id: String) = prefs.setSelectedTtsAdapterId(id)

    /** The active STT engine (selected id → first registered → null when the platform has none). */
    suspend fun stt(): SpeechToText? {
        if (sttEntries.isEmpty()) return null
        val id = prefs.getSelectedSttAdapterId().first()
        return (sttEntries.firstOrNull { it.id == id } ?: sttEntries.first()).engine
    }

    /** The active TTS engine (selected id → first registered → null when the platform has none). */
    suspend fun tts(): TextToSpeech? {
        if (ttsEntries.isEmpty()) return null
        val id = prefs.getSelectedTtsAdapterId().first()
        return (ttsEntries.firstOrNull { it.id == id } ?: ttsEntries.first()).engine
    }
}
