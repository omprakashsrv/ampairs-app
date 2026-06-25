package com.ampairs.agent.speech.whisper

import co.touchlab.kermit.Logger
import com.ampairs.agent.speech.SpeechToText
import com.ampairs.agent.speech.SttEvent
import com.ampairs.common.coroutines.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.sqrt

/**
 * Offline Whisper [SpeechToText] adapter (selectable in Assistant settings). Orchestrates the shared
 * pipeline — capture → transcribe — delegating the model file to [WhisperModelRegistry] and the
 * platform inference to [WhisperTranscriber]:
 *
 * 1. Resolve the selected model's local path; if it isn't downloaded, kick the download and surface a
 *    one-time [SttEvent.Error] so the UI falls back to text (US4-3) until it's ready.
 * 2. Stream 16 kHz mono PCM from [AudioCapture] until [stop] (or the ≤30 s Whisper window cap), then
 *    transcribe the whole buffer in one shot.
 *
 * **One-shot — no [SttEvent.Partial]s** (Whisper transcribes a full window at once). The
 * [SttEvent] contract allows this and the voice-loop `listenOnce` path only needs the [SttEvent.Final].
 */
class WhisperSttEngine(
    private val registry: WhisperModelRegistry,
    private val transcriber: WhisperTranscriber,
    private val audio: AudioCapture,
) : SpeechToText {

    override val isAvailable: Boolean
        get() = registry.hasModels && transcriber.isAvailable && audio.isAvailable

    override fun listen(languageTag: String?): Flow<SttEvent> = flow {
        val modelPath = registry.selectedModelPath()
        if (modelPath == null) {
            Logger.i(LOG_TAG) { "Whisper model not downloaded yet — kicking download, staying text this turn" }
            registry.ensureSelectedDownloaded()
            emit(SttEvent.Error("Downloading the Whisper model — please try again in a moment."))
            return@flow
        }

        // Endpointing: stream until the user stops speaking (energy drops for SILENCE_HANGOVER), or
        // there's no speech at all for NO_SPEECH_TIMEOUT, or we hit the 30 s window. Without this the
        // capture only ends on a manual stop / the 30 s cap, so it never auto-sends the utterance.
        val chunks = ArrayList<FloatArray>()
        var total = 0
        var speechStarted = false
        var trailingSilence = 0
        var maxLevel = 0f
        withTimeoutOrNull(MAX_UTTERANCE_MS) {
            audio.stream().collect { chunk ->
                chunks.add(chunk)
                total += chunk.size
                val level = rms(chunk)
                if (level > maxLevel) maxLevel = level
                when {
                    level >= SPEECH_RMS_THRESHOLD -> {
                        if (!speechStarted) Logger.i(LOG_TAG) { "Speech detected (rms=$level) — endpointing armed" }
                        speechStarted = true
                        trailingSilence = 0
                    }
                    speechStarted -> {
                        trailingSilence += chunk.size
                        if (trailingSilence >= SILENCE_HANGOVER_SAMPLES) {
                            Logger.i(LOG_TAG) { "End of speech (≥${SILENCE_HANGOVER_SAMPLES / 16_000f}s silence) — sending" }
                            audio.stop()
                        }
                    }
                    total >= NO_SPEECH_SAMPLES -> {
                        Logger.i(LOG_TAG) { "No speech in ${NO_SPEECH_SAMPLES / 16_000}s (maxRms=$maxLevel) — giving up" }
                        audio.stop()
                    }
                }
                if (total >= MAX_SAMPLES) audio.stop() // hit the 30 s window — end the stream
            }
        }
        audio.stop()
        emit(SttEvent.EndOfSpeech)

        if (total == 0) {
            Logger.w(LOG_TAG) { "No audio captured (mic produced 0 samples)" }
            emit(SttEvent.Error("Didn't catch that — please try again."))
            return@flow
        }
        if (!speechStarted) {
            // Recorded only silence/background — don't waste an inference pass on it.
            Logger.i(LOG_TAG) { "No speech above threshold (maxRms=$maxLevel, thr=$SPEECH_RMS_THRESHOLD) — skipping transcription" }
            emit(SttEvent.Error("Didn't catch that — please try again."))
            return@flow
        }

        val pcm = FloatArray(minOf(total, MAX_SAMPLES))
        var offset = 0
        for (chunk in chunks) {
            if (offset >= pcm.size) break
            val n = minOf(chunk.size, pcm.size - offset)
            chunk.copyInto(pcm, offset, 0, n)
            offset += n
        }

        Logger.i(LOG_TAG) { "Transcribing ${pcm.size} samples (~${pcm.size / 16_000}s) with $modelPath" }
        val text = runCatching { transcriber.transcribe(pcm, modelPath, languageTag) }
            .getOrElse {
                Logger.e(LOG_TAG, it) { "Whisper transcription failed: ${it.message}" }
                emit(SttEvent.Error(it.message ?: "Transcription failed."))
                return@flow
            }
        Logger.i(LOG_TAG) { "Whisper transcript: '${text.trim()}'" }
        emit(
            if (text.isBlank()) SttEvent.Error("Didn't catch that — please try again.")
            else SttEvent.Final(text.trim()),
        )
    }.flowOn(DispatcherProvider.io)

    override fun stop() = audio.stop()

    /** Root-mean-square level of a normalized [-1,1] PCM chunk; 0 for an empty chunk. */
    private fun rms(chunk: FloatArray): Float {
        if (chunk.isEmpty()) return 0f
        var sum = 0.0
        for (s in chunk) sum += s.toDouble() * s
        return sqrt(sum / chunk.size).toFloat()
    }

    private companion object {
        const val LOG_TAG = "AgentWhisper"
        const val MAX_UTTERANCE_MS = 30_000L
        const val MAX_SAMPLES = 16_000 * 30
        /** Energy gate for "is this speech". Tunable per the maxRms logged each utterance. */
        const val SPEECH_RMS_THRESHOLD = 0.02f
        /** Trailing silence (after speech) that ends the utterance: 1.2 s. */
        const val SILENCE_HANGOVER_SAMPLES = (16_000 * 1.2f).toInt()
        /** If no speech is detected at all, give up after 10 s. */
        const val NO_SPEECH_SAMPLES = 16_000 * 10
    }
}
