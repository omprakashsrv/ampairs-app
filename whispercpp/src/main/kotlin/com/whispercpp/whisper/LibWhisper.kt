package com.whispercpp.whisper

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private const val LOG_TAG = "LibWhisper"

/**
 * Thin Kotlin wrapper over the whisper.cpp JNI bridge (`libwhisper_jni.so`, built by CMake/FetchContent
 * — see `src/main/cpp/CMakeLists.txt`). Adapted from whisper.cpp's `examples/whisper.android` `lib`
 * module (package kept as `com.whispercpp.whisper` so the JNI symbol names line up), trimmed to the
 * file-based context + one-shot transcription the Ampairs offline STT adapter uses.
 *
 * A `whisper_context` must not be touched from more than one thread at a time, so every native call is
 * confined to a single-threaded dispatcher.
 */
class WhisperContext private constructor(private var ptr: Long) {
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
    )

    /**
     * Transcribe a finished 16 kHz mono PCM buffer; returns the concatenated segment text.
     * [language] is an ISO 639-1 code (e.g. "en", "hi"); null/blank → auto-detect (transcribe in the
     * spoken language, never translate).
     */
    suspend fun transcribeData(data: FloatArray, language: String? = null): String = withContext(scope.coroutineContext) {
        require(ptr != 0L) { "WhisperContext already released" }
        val numThreads = WhisperCpuConfig.preferredThreadCount
        Log.d(LOG_TAG, "Transcribing with $numThreads threads, language=${language ?: "auto"}")
        WhisperLib.fullTranscribe(ptr, numThreads, data, language)
        val count = WhisperLib.getTextSegmentCount(ptr)
        buildString {
            for (i in 0 until count) append(WhisperLib.getTextSegment(ptr, i))
        }
    }

    suspend fun release() = withContext(scope.coroutineContext) {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0
        }
    }

    protected fun finalize() {
        runBlocking { release() }
    }

    companion object {
        fun createContextFromFile(filePath: String): WhisperContext {
            val ptr = WhisperLib.initContext(filePath)
            if (ptr == 0L) throw RuntimeException("Couldn't create Whisper context from $filePath")
            return WhisperContext(ptr)
        }

        fun getSystemInfo(): String = WhisperLib.getSystemInfo()
    }
}

private class WhisperLib {
    companion object {
        init {
            Log.d(LOG_TAG, "Primary ABI: ${android.os.Build.SUPPORTED_ABIS.firstOrNull()}")
            System.loadLibrary("whisper_jni")
        }

        external fun initContext(modelPath: String): Long
        external fun freeContext(contextPtr: Long)
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray, language: String?)
        external fun getTextSegmentCount(contextPtr: Long): Int
        external fun getTextSegment(contextPtr: Long, index: Int): String
        external fun getSystemInfo(): String
    }
}
