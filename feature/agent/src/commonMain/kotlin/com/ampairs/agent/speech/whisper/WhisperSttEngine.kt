package com.ampairs.agent.speech.whisper

import com.ampairs.agent.speech.SpeechToText
import com.ampairs.agent.speech.SttEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull

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
            registry.ensureSelectedDownloaded()
            emit(SttEvent.Error("Downloading the Whisper model — please try again in a moment."))
            return@flow
        }

        val chunks = ArrayList<FloatArray>()
        var total = 0
        withTimeoutOrNull(MAX_UTTERANCE_MS) {
            audio.stream().collect { chunk ->
                chunks.add(chunk)
                total += chunk.size
                if (total >= MAX_SAMPLES) audio.stop() // hit the 30 s window — end the stream
            }
        }
        audio.stop()
        emit(SttEvent.EndOfSpeech)

        if (total == 0) {
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

        val text = runCatching { transcriber.transcribe(pcm, modelPath, languageTag) }
            .getOrElse {
                emit(SttEvent.Error(it.message ?: "Transcription failed."))
                return@flow
            }
        emit(
            if (text.isBlank()) SttEvent.Error("Didn't catch that — please try again.")
            else SttEvent.Final(text.trim()),
        )
    }.flowOn(Dispatchers.IO)

    override fun stop() = audio.stop()

    private companion object {
        const val MAX_UTTERANCE_MS = 30_000L
        const val MAX_SAMPLES = 16_000 * 30
    }
}
