package com.ampairs.agent.speech

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.localeWithLocaleIdentifier
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer

/**
 * iOS STT actual (FR-008) over `SFSpeechRecognizer` + `AVAudioEngine`, preferring on-device
 * recognition. Collecting [listen] configures the audio session, taps the mic into a buffer
 * recognition request, and streams [SttEvent]s; the flow closes on the first final result or error.
 * [awaitClose] tears down the tap, engine, request and task. `@OptIn(ExperimentalForeignApi)`.
 *
 * Requires the mic + speech-recognition authorizations (driven by [com.ampairs.agent.permission.
 * IosMicPermissionController]) and the host app's Info.plist `NSMicrophoneUsageDescription` +
 * `NSSpeechRecognitionUsageDescription` keys.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSpeechToText : SpeechToText {

    private fun recognizer(languageTag: String?): SFSpeechRecognizer? =
        if (languageTag != null) {
            SFSpeechRecognizer(locale = NSLocale.localeWithLocaleIdentifier(languageTag))
        } else {
            SFSpeechRecognizer()
        }

    override val isAvailable: Boolean
        get() = SFSpeechRecognizer()?.available ?: false

    override fun listen(languageTag: String?): Flow<SttEvent> = callbackFlow {
        val rec = recognizer(languageTag)
        if (rec == null || !rec.available) {
            trySend(SttEvent.Error("Speech recognition is not available on this device."))
            close()
            return@callbackFlow
        }

        val request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        request.requiresOnDeviceRecognition = true

        val audioEngine = AVAudioEngine()
        val inputNode = audioEngine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)

        val task = rec.recognitionTaskWithRequest(request) { result, error ->
            if (result != null) {
                val text = result.bestTranscription.formattedString
                if (result.isFinal()) {
                    if (text.isNotBlank()) trySend(SttEvent.Final(text))
                    else trySend(SttEvent.Error("No speech recognized."))
                    close()
                } else if (text.isNotEmpty()) {
                    trySend(SttEvent.Partial(text))
                }
            }
            if (error != null) {
                trySend(SttEvent.Error(error.localizedDescription))
                close()
            }
        }

        inputNode.installTapOnBus(bus = 0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
            buffer?.let { request.appendAudioPCMBuffer(it) }
        }

        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryRecord, mode = AVAudioSessionModeMeasurement, options = 0u, error = null)
        session.setActive(true, error = null)
        audioEngine.prepare()
        val started = audioEngine.startAndReturnError(null)
        if (!started) {
            trySend(SttEvent.Error("Couldn't access the microphone."))
            close()
        }

        awaitClose {
            audioEngine.stop()
            inputNode.removeTapOnBus(0u)
            request.endAudio()
            task.cancel()
            runCatching { session.setActive(false, error = null) }
        }
    }

    override fun stop() {}
}
