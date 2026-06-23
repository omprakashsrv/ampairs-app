package com.ampairs.agent.speech

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * STT fallback for platforms without on-device recognition (Desktop, T015) — emits a single
 * [SttEvent.Error] so the UI degrades to text-only input (US4-3) rather than appearing to listen.
 */
object UnsupportedSpeechToText : SpeechToText {
    override val isAvailable: Boolean = false

    override fun listen(languageTag: String?): Flow<SttEvent> =
        flowOf(SttEvent.Error("Speech recognition is not available on this platform."))

    override fun stop() {}
}

/** TTS fallback for platforms without speech synthesis (Desktop, T015) — silently does nothing. */
object NoOpTextToSpeech : TextToSpeech {
    override val isAvailable: Boolean = false

    override suspend fun speak(text: String, languageTag: String?) {}

    override fun stop() {}
}
