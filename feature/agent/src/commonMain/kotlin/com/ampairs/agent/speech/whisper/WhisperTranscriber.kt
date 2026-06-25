package com.ampairs.agent.speech.whisper

/**
 * Platform Whisper inference backend. Takes a finished **16 kHz mono PCM** buffer and returns the
 * transcript. Plain interface (DI-provided per platform) so [WhisperSttEngine] in commonMain stays
 * engine-agnostic. All platforms run **whisper.cpp / ggml**, which computes the log-mel + runs the
 * encoder/decoder + detokenizes natively — so every actual takes the raw PCM floats directly:
 * - **Android:** the `:whispercpp` JNI module ([AndroidWhisperCppTranscriber]).
 * - **Desktop:** `whisper-jni` ([DesktopWhisperTranscriber]).
 * - **iOS:** Kotlin/Native cinterop to `whisper.xcframework` ([IosWhisperTranscriber]).
 *
 * One-shot per utterance (Whisper transcribes a whole ≤30 s window at once — no streaming partials).
 */
interface WhisperTranscriber {
    /** Whether the native runtime is loadable on this platform/device. */
    val isAvailable: Boolean

    /**
     * Transcribe [pcm] (16 kHz mono, normalized floats) using the model file at [modelPath].
     * [languageTag] is a BCP-47 tag (e.g. "en-IN"); null → auto/default. Returns "" when nothing was
     * recognized. Implementations cache the loaded model by path.
     */
    suspend fun transcribe(pcm: FloatArray, modelPath: String, languageTag: String?): String

    /** Release any loaded native model/context. */
    fun release()
}
