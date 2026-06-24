package com.ampairs.agent.speech

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

/**
 * iOS TTS actual (FR-009) over `AVSpeechSynthesizer`. [speak] suspends until the utterance finishes
 * (or is cut short by [stop] / coroutine cancellation) via the synthesizer delegate, so the hands-free
 * voice loop re-opens the mic only after the reply is fully spoken. `@OptIn(ExperimentalForeignApi)`.
 */
@OptIn(ExperimentalForeignApi::class)
class IosTextToSpeech : TextToSpeech {

    private val synthesizer = AVSpeechSynthesizer()

    @Volatile
    private var current: CompletableDeferred<Unit>? = null

    private val handler = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didFinishSpeechUtterance: AVSpeechUtterance,
        ) {
            current?.complete(Unit)
        }

        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didCancelSpeechUtterance: AVSpeechUtterance,
        ) {
            current?.complete(Unit)
        }
    }

    init {
        synthesizer.delegate = handler
    }

    override val isAvailable: Boolean = true

    override suspend fun speak(text: String, languageTag: String?) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        val lang = languageTag ?: AVSpeechSynthesisVoice.currentLanguageCode()
        AVSpeechSynthesisVoice.voiceWithLanguage(lang)?.let { utterance.setVoice(it) }
        val done = CompletableDeferred<Unit>()
        current = done
        synthesizer.speakUtterance(utterance)
        try {
            done.await()
        } catch (c: CancellationException) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            throw c
        }
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        current?.complete(Unit)
    }
}
