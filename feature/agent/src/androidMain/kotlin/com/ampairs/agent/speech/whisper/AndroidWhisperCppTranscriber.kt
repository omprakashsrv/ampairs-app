package com.ampairs.agent.speech.whisper

import co.touchlab.kermit.Logger
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

/**
 * Android Whisper inference via **whisper.cpp** (ggml `.bin`), through the `:whispercpp` JNI module.
 * whisper.cpp computes the log-mel + runs the encoder/decoder + detokenizes natively, so this takes the
 * finished 16 kHz mono PCM floats directly — the shared [WhisperFeatureExtractor]/[WhisperTokenizer]
 * (the old LiteRT `.tflite` front-end) are not used on this path.
 *
 * The native context is loaded once per model-file path (cached) and reused; calls are serialized with
 * a [Mutex] since a whisper.cpp context is single-threaded. Mirrors [DesktopWhisperTranscriber].
 */
class AndroidWhisperCppTranscriber : WhisperTranscriber {

    private val mutex = Mutex()

    @Volatile private var context: WhisperContext? = null
    @Volatile private var contextPath: String? = null

    // The JNI lib loads lazily on first context creation; treat the engine as available and surface any
    // load failure as a transcription error (so the voice loop falls back to text for that turn).
    override val isAvailable: Boolean = true

    override suspend fun transcribe(pcm: FloatArray, modelPath: String, languageTag: String?): String =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                contextFor(modelPath).transcribeData(pcm).trim()
            }
        }

    private suspend fun contextFor(modelPath: String): WhisperContext {
        context?.let { if (contextPath == modelPath) return it }
        context?.let { runCatching { it.release() } }
        return WhisperContext.createContextFromFile(modelPath).also {
            context = it
            contextPath = modelPath
            Logger.i(LOG_TAG) { "Loaded whisper.cpp model: $modelPath" }
        }
    }

    override fun release() {
        val ctx = context ?: return
        context = null
        contextPath = null
        runBlocking { runCatching { ctx.release() } }
    }

    private companion object {
        const val LOG_TAG = "AgentWhisper"
    }
}
