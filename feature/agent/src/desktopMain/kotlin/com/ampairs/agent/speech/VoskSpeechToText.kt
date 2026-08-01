package com.ampairs.agent.speech

import co.touchlab.kermit.Logger
import com.ampairs.agent.speech.vosk.VoskModelRegistry
import com.ampairs.agent.speech.whisper.AudioCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import kotlin.concurrent.Volatile

/**
 * Desktop offline STT via **Vosk (Kaldi)** — a second engine alongside Whisper. JNA-based; the native
 * libs ship in the `com.alphacephei:vosk` jar for Win/Mac/Linux. The model (a directory) is downloaded
 * on demand through [VoskModelRegistry] over the shared archive-aware `ModelManager` — mirroring how
 * [com.ampairs.agent.speech.whisper.WhisperSttEngine] resolves its model.
 *
 * Differs from Whisper in two ways that map cleanly onto the [SttEvent] contract:
 *  - **Streaming**: `Recognizer.acceptWaveForm` is fed mic chunks live, so we emit [SttEvent.Partial]
 *    from `getPartialResult()` as the user speaks (Whisper is one-shot, partial-less).
 *  - **Self-endpointing**: `acceptWaveForm` returns `true` at a silence boundary; the first non-blank
 *    `getResult()` becomes the [SttEvent.Final] and ends the utterance — no separate VAD needed.
 *
 * Audio comes from the shared [AudioCapture] (16 kHz mono, normalized floats); we convert each chunk to
 * 16-bit PCM and use the `short[]` overload (unambiguous range, unlike the float overload). The Vosk
 * model is language-fixed by its directory, so `languageTag` is informational only.
 */
class VoskSpeechToText(
    private val registry: VoskModelRegistry,
    private val audio: AudioCapture,
) : SpeechToText {

    private val json = Json { ignoreUnknownKeys = true }

    override val isAvailable: Boolean
        get() = registry.hasModels && audio.isAvailable && nativeLibLoads()

    override fun listen(languageTag: String?): Flow<SttEvent> = flow {
        val modelPath = registry.selectedModelPath()
        if (modelPath == null) {
            Logger.i(tag = LOG_TAG) { "Vosk model not downloaded yet — kicking download, staying text this turn" }
            registry.ensureSelectedDownloaded()
            emit(SttEvent.Error("Downloading the Vosk model — please try again in a moment."))
            return@flow
        }
        languageTag?.let { Logger.d(tag = LOG_TAG) { "languageTag '$it' ignored — Vosk model is language-fixed" } }

        val recognizer = try {
            runCatching { LibVosk.setLogLevel(LogLevel.WARNINGS) }
            Recognizer(Model(modelPath), SAMPLE_RATE)
        } catch (t: Throwable) {
            Logger.e(tag = LOG_TAG, throwable = t) { "Vosk init failed" }
            emit(SttEvent.Error("Speech recognition is unavailable on this device."))
            return@flow
        }

        var emittedFinal = false
        var lastPartial = ""
        try {
            audio.stream().collect { chunk ->
                val pcm = chunk.toShorts()
                if (recognizer.acceptWaveForm(pcm, pcm.size)) {
                    val text = recognizer.result.field("text")
                    if (text.isNotBlank()) {
                        emit(SttEvent.Final(text))
                        emit(SttEvent.EndOfSpeech)
                        emittedFinal = true
                        audio.stop() // completes the capture flow → collect returns
                    }
                } else {
                    val partial = recognizer.partialResult.field("partial")
                    if (partial.isNotBlank() && partial != lastPartial) {
                        lastPartial = partial
                        emit(SttEvent.Partial(partial))
                    }
                }
            }
            // Capture ended (early stop() or device EOF) without a silence-boundary final — flush.
            if (!emittedFinal) {
                val text = recognizer.finalResult.field("text")
                if (text.isNotBlank()) emit(SttEvent.Final(text))
                emit(SttEvent.EndOfSpeech)
            }
        } finally {
            runCatching { recognizer.close() }
        }
    }.flowOn(Dispatchers.IO)

    override fun stop() {
        audio.stop()
    }

    /** Normalized floats (-1f..1f) → 16-bit signed PCM for the `acceptWaveForm(short[], …)` overload. */
    private fun FloatArray.toShorts(): ShortArray {
        val out = ShortArray(size)
        for (i in indices) {
            out[i] = (this[i] * 32768f).coerceIn(-32768f, 32767f).toInt().toShort()
        }
        return out
    }

    /** Pull a string field out of a Vosk result JSON (`{"text": "…"}` / `{"partial": "…"}`). */
    private fun String.field(key: String): String =
        runCatching {
            json.parseToJsonElement(this).jsonObject[key]?.jsonPrimitive?.contentOrNull
        }.getOrNull()?.trim().orEmpty()

    private fun nativeLibLoads(): Boolean =
        nativeLoadCache ?: runCatching { LibVosk.setLogLevel(LogLevel.WARNINGS); true }
            .getOrDefault(false).also { nativeLoadCache = it }

    private companion object {
        const val LOG_TAG = "AgentVosk"
        const val SAMPLE_RATE = 16_000f

        @Volatile
        private var nativeLoadCache: Boolean? = null
    }
}
