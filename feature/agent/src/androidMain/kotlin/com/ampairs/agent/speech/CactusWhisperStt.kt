package com.ampairs.agent.speech

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import co.touchlab.kermit.Logger
import com.cactus.CactusContextInitializer
import com.cactus.CactusInitParams
import com.cactus.CactusSTT
import com.cactus.CactusTranscriptionParams
import com.cactus.TranscriptionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.ByteArrayOutputStream
import kotlin.concurrent.Volatile

/**
 * Offline Whisper STT adapter (Cactus). Cactus does not capture the mic, so we record 16 kHz mono
 * 16-bit PCM via [AudioRecord] and feed the buffer to `CactusSTT.transcribe`. Endpointing is simple
 * energy-based: stop after a short silence following speech (or a hard cap), emitting interim
 * [SttEvent.Partial]s while recording and a [SttEvent.Final] at the end. The Whisper model is
 * downloaded + initialized lazily on first use. Requires the `RECORD_AUDIO` permission (gated by the
 * mic permission controller before listen()).
 */
class CactusWhisperStt(
    private val context: Context,
    private val model: String = "whisper-tiny",
) : SpeechToText {

    private val stt = CactusSTT()

    @Volatile private var modelReady = false
    @Volatile private var listening = false
    @Volatile private var recorder: AudioRecord? = null

    override val isAvailable: Boolean = true

    @SuppressLint("MissingPermission")
    override fun listen(languageTag: String?): Flow<SttEvent> = callbackFlow {
        if (!ensureModelReady()) {
            trySend(SttEvent.Error("Whisper model is unavailable."))
            close()
            return@callbackFlow
        }

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) {
            trySend(SttEvent.Error("Couldn't access the microphone."))
            close()
            return@callbackFlow
        }
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, SAMPLE_RATE * 2),
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { rec.release() }
            trySend(SttEvent.Error("Couldn't access the microphone."))
            close()
            return@callbackFlow
        }
        recorder = rec
        listening = true

        val acc = ByteArrayOutputStream()
        val buf = ByteArray(CHUNK_BYTES)
        var speechMs = 0
        var silenceMs = 0
        var totalMs = 0
        var lastPartialMs = 0
        runCatching { rec.startRecording() }
        try {
            while (isActive && listening) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                acc.write(buf, 0, n)
                val chunkMs = (n / 2) * 1000 / SAMPLE_RATE
                totalMs += chunkMs
                if (rms(buf, n) > SILENCE_RMS) {
                    speechMs += chunkMs
                    silenceMs = 0
                } else if (speechMs > 0) {
                    silenceMs += chunkMs
                }
                if (acc.size() >= MIN_BYTES && totalMs - lastPartialMs >= PARTIAL_INTERVAL_MS) {
                    lastPartialMs = totalMs
                    transcribe(acc.toByteArray())?.takeIf { it.isNotBlank() }?.let { trySend(SttEvent.Partial(it)) }
                }
                if ((speechMs >= MIN_SPEECH_MS && silenceMs >= END_SILENCE_MS) || totalMs >= MAX_MS) break
            }
            runCatching { rec.stop() }
            val finalText = if (acc.size() >= MIN_BYTES) transcribe(acc.toByteArray()) else null
            trySend(if (!finalText.isNullOrBlank()) SttEvent.Final(finalText) else SttEvent.Error("Didn't catch that — please try again."))
            close()
        } catch (e: Exception) {
            Logger.w(tag = "AgentLlm") { "Whisper STT failed: ${e.message}" }
            trySend(SttEvent.Error(e.message ?: "Speech recognition failed."))
            close()
        }

        awaitClose {
            listening = false
            runCatching { rec.stop() }
            runCatching { rec.release() }
            recorder = null
        }
    }.flowOn(Dispatchers.IO)

    override fun stop() {
        listening = false
        runCatching { recorder?.stop() }
    }

    /** Download + initialize the Whisper model once. Returns true when the engine is ready. */
    private suspend fun ensureModelReady(): Boolean {
        if (modelReady && stt.isReady()) return true
        return runCatching {
            CactusContextInitializer.initialize(context.applicationContext)
            if (!stt.isModelDownloaded(model)) stt.downloadModel(model)
            stt.initializeModel(CactusInitParams(model = model))
            modelReady = stt.isReady()
            modelReady
        }.getOrElse {
            Logger.w(tag = "AgentLlm") { "Whisper model init failed: ${it.message}" }
            false
        }
    }

    /** One-shot transcription of accumulated PCM; null on failure. */
    private suspend fun transcribe(pcm: ByteArray): String? = runCatching {
        stt.transcribe(
            audioBuffer = pcm,
            params = CactusTranscriptionParams(model = model),
            mode = TranscriptionMode.LOCAL,
        )?.takeIf { it.success }?.text
    }.getOrNull()

    /** Root-mean-square amplitude of a 16-bit little-endian PCM chunk (first [len] bytes). */
    private fun rms(bytes: ByteArray, len: Int): Double {
        if (len < 2) return 0.0
        var sum = 0.0
        var i = 0
        var count = 0
        while (i + 1 < len) {
            val sample = (bytes[i].toInt() and 0xFF) or (bytes[i + 1].toInt() shl 8)
            sum += sample.toDouble() * sample.toDouble()
            i += 2
            count++
        }
        return if (count == 0) 0.0 else kotlin.math.sqrt(sum / count)
    }

    private companion object {
        const val SAMPLE_RATE = 16000
        const val CHUNK_BYTES = 3200 // 100 ms of 16 kHz mono PCM16
        const val MIN_BYTES = 32000 // Cactus requires >= 1 s of audio
        const val SILENCE_RMS = 600.0
        const val MIN_SPEECH_MS = 300
        const val END_SILENCE_MS = 1000
        const val MAX_MS = 15000
        const val PARTIAL_INTERVAL_MS = 1500
    }
}
