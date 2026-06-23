package com.ampairs.agent.speech

/**
 * Text-to-speech port (FR-009) for reading assistant replies aloud. Platform actuals:
 * Android `android.speech.tts.TextToSpeech` (T013), iOS `AVSpeechSynthesizer` (T014); Desktop binds
 * [NoOpTextToSpeech] (T015). Plain interface (not `expect`) so platforms contribute via Metro DI.
 *
 * Muting is a UI/`ChatUiState` concern (T017) — when muted the ViewModel simply doesn't call [speak].
 */
interface TextToSpeech {
    /** Whether speech synthesis is usable on this platform/device. */
    val isAvailable: Boolean

    /** Speak [text]; [languageTag] is a BCP-47 tag (e.g. "en-IN"), null → device default. Suspends until
     *  the utterance is queued (not necessarily finished). */
    suspend fun speak(text: String, languageTag: String? = null)

    /** Stop any in-progress and queued speech. */
    fun stop()
}
