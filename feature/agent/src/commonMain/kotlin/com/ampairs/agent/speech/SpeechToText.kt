package com.ampairs.agent.speech

import kotlinx.coroutines.flow.Flow

/**
 * What the recognizer emits during a [SpeechToText.listen] session (FR-008). Partials let the UI show
 * a live transcript; a single [Final] ends the utterance and is what gets submitted to the assistant.
 */
sealed interface SttEvent {
    /** Interim hypothesis — may change as more audio arrives. */
    data class Partial(val text: String) : SttEvent

    /** Committed recognition result for the utterance. */
    data class Final(val text: String) : SttEvent

    /** Recognizer or permission failure — the UI falls back to text input (US4-3). */
    data class Error(val message: String) : SttEvent

    /** The recognizer detected the end of speech (mic can be released). */
    data object EndOfSpeech : SttEvent
}

/**
 * On-device speech-to-text port (FR-008). Platform actuals: Android `SpeechRecognizer`
 * (`EXTRA_PREFER_OFFLINE`, T013), iOS `SFSpeechRecognizer` (`requiresOnDeviceRecognition = true`,
 * T014); Desktop has no recognizer and binds [UnsupportedSpeechToText] (T015). Kept a plain interface
 * (not `expect`) so each platform contributes its implementation through Metro DI, matching the rest
 * of the codebase.
 */
interface SpeechToText {
    /** Whether on-device recognition is usable on this platform/device. */
    val isAvailable: Boolean

    /**
     * Cold flow — collecting starts the microphone and streams [SttEvent]s until a [SttEvent.Final]
     * (or [SttEvent.Error]). [languageTag] is a BCP-47 tag (e.g. "en-IN"); null → device default.
     */
    fun listen(languageTag: String? = null): Flow<SttEvent>

    /** Stop an in-progress recognition early, committing whatever was heard so far. */
    fun stop()
}
