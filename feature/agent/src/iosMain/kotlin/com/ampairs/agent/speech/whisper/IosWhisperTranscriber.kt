package com.ampairs.agent.speech.whisper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile
import whisper.cpp.whisper_context
import whisper.cpp.whisper_context_default_params
import whisper.cpp.whisper_free
import whisper.cpp.whisper_full
import whisper.cpp.whisper_full_default_params
import whisper.cpp.whisper_full_get_segment_text
import whisper.cpp.whisper_full_n_segments
import whisper.cpp.whisper_init_from_file_with_params
import whisper.cpp.whisper_sampling_strategy

/**
 * iOS Whisper inference via **whisper.cpp** (ggml) through Kotlin/Native **cinterop** to its C API
 * (`whisper.h`) — no Swift wrapper. whisper.cpp computes the log-mel and decodes internally, so this
 * takes the raw 16 kHz mono PCM directly (like the Desktop `whisper-jni` path); the commonMain
 * mel/tokenizer helpers are not used here.
 *
 * The loaded model context is cached by path. Inference runs on [Dispatchers.Default] (the iOS rule:
 * `Dispatchers.IO`'s actual is internal on Kotlin/Native). The static library is linked into the iOS
 * app at the Xcode build step (the prebuilt whisper.xcframework); this Kotlin side only needs the
 * cinterop bindings. Language/threads are left at whisper defaults for now (runtime-tuned on device).
 */
@OptIn(ExperimentalForeignApi::class)
class IosWhisperTranscriber : WhisperTranscriber {

    private val mutex = Mutex()

    @Volatile private var ctx: CPointer<whisper_context>? = null
    @Volatile private var loadedPath: String? = null

    override val isAvailable: Boolean = true

    override suspend fun transcribe(pcm: FloatArray, modelPath: String, languageTag: String?): String {
        if (pcm.isEmpty()) return ""
        return withContext(Dispatchers.Default) {
            mutex.withLock {
                val context = ensureContext(modelPath)
                val rc = pcm.usePinned { pinned ->
                    whisper_full(
                        context,
                        whisper_full_default_params(whisper_sampling_strategy.WHISPER_SAMPLING_GREEDY),
                        pinned.addressOf(0),
                        pcm.size,
                    )
                }
                if (rc != 0) throw IllegalStateException("whisper_full failed (code $rc)")
                buildString {
                    val segments = whisper_full_n_segments(context)
                    for (i in 0 until segments) {
                        append(whisper_full_get_segment_text(context, i)?.toKString() ?: "")
                    }
                }.trim()
            }
        }
    }

    /** Load (and cache) the ggml model context for [modelPath]; reused across calls for the same path. */
    private fun ensureContext(modelPath: String): CPointer<whisper_context> {
        ctx?.let { if (loadedPath == modelPath) return it }
        ctx?.let { whisper_free(it) }
        ctx = null
        val created = memScoped {
            whisper_init_from_file_with_params(modelPath.cstr.ptr, whisper_context_default_params())
        } ?: throw IllegalStateException("Failed to load Whisper model: $modelPath")
        ctx = created
        loadedPath = modelPath
        return created
    }

    override fun release() {
        ctx?.let { whisper_free(it) }
        ctx = null
        loadedPath = null
    }
}
