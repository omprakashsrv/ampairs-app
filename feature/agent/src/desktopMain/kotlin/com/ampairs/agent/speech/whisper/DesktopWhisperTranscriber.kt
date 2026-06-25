package com.ampairs.agent.speech.whisper

import co.touchlab.kermit.Logger
import io.github.givimad.whisperjni.WhisperContext
import io.github.givimad.whisperjni.WhisperFullParams
import io.github.givimad.whisperjni.WhisperJNI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import kotlin.concurrent.Volatile

/**
 * Desktop Whisper inference via **whisper.cpp** (`whisper-jni`, native libs embedded in the jar for
 * Win/Mac/Linux). whisper.cpp does its own mel + tokenization, so this takes the raw 16 kHz PCM floats
 * directly — the shared [WhisperFeatureExtractor]/tokenizer (used by the LiteRT path) are not needed here.
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
                // No-arg params = greedy decode with auto language detection (multilingual model).
                val params = WhisperFullParams()
                val result = jni.full(ctx, params, pcm, pcm.size)
                if (result != 0) {
                    Logger.w(tag = LOG_TAG) { "whisper.cpp full() returned $result" }
                    return@withLock ""
                }
                val segments = jni.fullNSegments(ctx)
                buildString {
                    for (i in 0 until segments) append(jni.fullGetSegmentText(ctx, i))
                }.trim()
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
    }
}
