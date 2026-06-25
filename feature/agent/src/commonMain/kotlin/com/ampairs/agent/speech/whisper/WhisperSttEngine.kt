package com.ampairs.agent.speech.whisper

import co.touchlab.kermit.Logger
import com.ampairs.agent.speech.SpeechToText
import com.ampairs.agent.speech.SttEvent
import com.ampairs.common.coroutines.DispatcherProvider
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
            Logger.i(LOG_TAG) { "Whisper model not downloaded yet — kicking download, staying text this turn" }
            registry.ensureSelectedDownloaded()
            emit(SttEvent.Error("Downloading the Whisper model — please try again in a moment."))
            return@flow
        }

        // Adaptive voice-activity detection decides when the utterance ends: it self-calibrates to the
        // room/mic, ends ~1.2 s after you stop speaking, and gives up if nothing is said — so capture
        // auto-sends instead of running until a manual stop / the 30 s cap. See [VoiceActivityDetector].
        val chunks = ArrayList<FloatArray>()
        var total = 0
        val vad = VoiceActivityDetector()
        withTimeoutOrNull(MAX_UTTERANCE_MS) {
            audio.stream().collect { chunk ->
                chunks.add(chunk)
                total += chunk.size
                when (vad.accept(chunk)) {
                    VadDecision.ENDPOINT -> {
                        Logger.i(LOG_TAG) { "End of speech (silence after speech) — sending (maxRms=${vad.maxRms})" }
                        audio.stop()
                    }
                    VadDecision.NO_SPEECH_TIMEOUT -> {
                        Logger.i(LOG_TAG) { "No speech detected (maxRms=${vad.maxRms}, floor=${vad.noiseFloor}) — giving up" }
                        audio.stop()
                    }
                    VadDecision.CONTINUE -> {}
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
        if (!vad.speechStarted) {
            // Recorded only silence/background — don't waste an inference pass on it.
            Logger.i(LOG_TAG) { "No speech above floor (maxRms=${vad.maxRms}, floor=${vad.noiseFloor}) — skipping transcription" }
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
        // Bound the one-shot inference so a stuck/extremely-slow native call surfaces as a retryable
        // error instead of freezing the voice UI on the mic screen forever. The first call also pays
        // the model-file load, so the window is generous (the model is cached for later utterances).
        val result = runCatching {
            withTimeoutOrNull(TRANSCRIBE_TIMEOUT_MS) { transcriber.transcribe(pcm, modelPath, languageTag) }
        }.getOrElse {
            Logger.e(LOG_TAG, it) { "Whisper transcription failed: ${it.message}" }
            emit(SttEvent.Error(it.message ?: "Transcription failed."))
            return@flow
        }
        if (result == null) {
            Logger.e(LOG_TAG) { "Whisper transcription timed out after ${TRANSCRIBE_TIMEOUT_MS}ms" }
            emit(SttEvent.Error("Transcription took too long — please try again."))
            return@flow
        }
        val text = result
        Logger.i(LOG_TAG) { "Whisper transcript: '${text.trim()}'" }
        emit(
            if (text.isBlank()) SttEvent.Error("Didn't catch that — please try again.")
            else SttEvent.Final(text.trim()),
        )
    }.flowOn(DispatcherProvider.io)

    override fun stop() = audio.stop()

    private companion object {
        const val LOG_TAG = "AgentWhisper"
        const val MAX_UTTERANCE_MS = 30_000L
        const val MAX_SAMPLES = 16_000 * 30

        /** Hard cap for one-shot inference (incl. first-call model load) so the mic UI can't hang. */
        const val TRANSCRIBE_TIMEOUT_MS = 45_000L
    }
}
