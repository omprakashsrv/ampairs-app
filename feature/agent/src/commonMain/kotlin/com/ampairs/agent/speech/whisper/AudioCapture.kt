package com.ampairs.agent.speech.whisper

import kotlinx.coroutines.flow.Flow

/**
 * Microphone capture for the Whisper STT pipeline — streams **16 kHz mono PCM** as normalized floats
 * (`-1.0f..1.0f`), the exact format Whisper expects. Kept a plain interface (not `expect`) so each
 * platform contributes its implementation through Metro DI, matching [com.ampairs.agent.speech.SpeechToText].
 *
 * Platform actuals: Android `AudioRecord`, iOS `AVAudioEngine`, Desktop `javax.sound.sampled`.
 */
interface AudioCapture {
    /** Whether mic capture is usable on this platform/device (line available, etc.). */
    val isAvailable: Boolean

    /**
     * Cold flow — collecting opens the mic and emits PCM float chunks until [stop] is called (the flow
     * then completes) or the collector is cancelled. Each chunk is contiguous 16 kHz mono samples.
     */
    fun stream(): Flow<FloatArray>

    /** Signal the active [stream] collection to finish (end of utterance / push-to-talk release). */
    fun stop()
}
