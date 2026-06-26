package com.ampairs.agent.speech.whisper

import co.touchlab.kermit.Logger
import io.github.givimad.whisperjni.WhisperContext
import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import io.github.givimad.whisperjni.WhisperSamplingStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import kotlin.concurrent.Volatile
import kotlin.time.measureTimedValue

/**
 * Desktop Whisper inference via **whisper.cpp** (`whisper-jni`, native libs embedded in the jar for
 * Win/Mac/Linux). whisper.cpp does its own mel + tokenization, so this takes the raw 16 kHz PCM floats
 * directly.
 *
 * The model context is loaded once per model file path (cached) and reused; transcription is serialized
 * with a [Mutex] since a `WhisperContext` is single-threaded. Library load failures (unsupported OS)
 * surface via [isAvailable] = false so the registry reports the adapter unavailable.
 */
class DesktopWhisperTranscriber : WhisperTranscriber {

    private val mutex = Mutex()

    @Volatile private var whisper: WhisperJNI? = null
    @Volatile private var context: WhisperContext? = null
    @Volatile private var contextPath: String? = null

    override val isAvailable: Boolean
        get() = runCatching { ensureLibrary() }.isSuccess

    override suspend fun transcribe(pcm: FloatArray, modelPath: String, languageTag: String?): String =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val jni = ensureLibrary()
                val ctx = contextFor(jni, modelPath)
                // BCP-47 (e.g. "hi-IN") → whisper ISO 639-1 ("hi"); null/blank → "auto" so whisper.cpp
                // detects the spoken language and transcribes in it (Hindi stays Hindi), never
                // translating. Mirrors AndroidWhisperCppTranscriber.
                val lang = languageTag?.substringBefore('-')?.lowercase()?.takeIf { it.isNotBlank() } ?: "auto"
                // IMPORTANT: WhisperFullParams()'s no-arg constructor defaults to BEAM SEARCH and
                // language="en" with progress/timestamp printing on. Construct with GREEDY (much faster
                // for one-shot short utterances) and mirror the Android/whisper.cpp param block exactly.
                val params = WhisperFullParams(WhisperSamplingStrategy.GREEDY).apply {
                    nThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, MAX_THREADS)
                    language = lang          // override the lib's hardcoded "en" default
                    translate = false        // transcribe in-language, never to English
                    noContext = true
                    singleSegment = false
                    offsetMs = 0
                    printRealtime = false
                    printProgress = false    // lib default is true — silence it + skip the work
                    printTimestamps = false  // lib default is true
                    printSpecial = false
                    noTimestamps = true      // we only use segment text
                    suppressNonSpeechTokens = true
                    // best_of defaults to 5/-1; at temperature 0 greedy decoding is deterministic, so
                    // extra candidates are wasted compute — 1 gives the same output for ~one decode.
                    greedyBestOf = 1
                }
                val (text, dur) = measureTimedValue {
                    val result = jni.full(ctx, params, pcm, pcm.size)
                    if (result != 0) {
                        Logger.w(tag = LOG_TAG) { "whisper.cpp full() returned $result" }
                        ""
                    } else {
                        val segments = jni.fullNSegments(ctx)
                        buildString {
                            for (i in 0 until segments) append(jni.fullGetSegmentText(ctx, i))
                        }.trim()
                    }
                }
                Logger.i(tag = LOG_TAG) {
                    "Inference took $dur for ${pcm.size} samples (~${pcm.size / 16_000}s audio, threads=${params.nThreads})"
                }
                text
            }
        }

    private fun ensureLibrary(): WhisperJNI {
        whisper?.let { return it }
        WhisperJNI.loadLibrary()
        WhisperJNI.setLibraryLogger(null)
        return WhisperJNI().also { whisper = it }
    }

    private fun contextFor(jni: WhisperJNI, modelPath: String): WhisperContext {
        context?.let { if (contextPath == modelPath) return it }
        runCatching { context?.close() }
        return jni.init(Paths.get(modelPath)).also {
            context = it
            contextPath = modelPath
        }
    }

    override fun release() {
        runCatching { context?.close() }
        context = null
        contextPath = null
    }

    private companion object {
        const val LOG_TAG = "AgentWhisper"
        const val MAX_THREADS = 8
    }
}
