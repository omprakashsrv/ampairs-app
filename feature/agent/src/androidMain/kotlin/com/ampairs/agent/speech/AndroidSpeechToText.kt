package com.ampairs.agent.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Android STT actual (T013) over [SpeechRecognizer], preferring on-device recognition
 * (`EXTRA_PREFER_OFFLINE`, FR-008). The recognizer must be created and driven on the main thread, so
 * the whole [callbackFlow] runs on `Dispatchers.Main`; collecting starts the mic and the flow
 * completes on the first `Final`/`Error`.
 *
 * Requires the `RECORD_AUDIO` runtime permission (declared + granted by the host app / T016). Without
 * it the recognizer reports `ERROR_INSUFFICIENT_PERMISSIONS`, surfaced here as an [SttEvent.Error] so
 * the UI falls back to text input (US4-3).
 */
class AndroidSpeechToText(private val context: Context) : SpeechToText {

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    @Volatile
    private var recognizer: SpeechRecognizer? = null

    override fun listen(languageTag: String?): Flow<SttEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(SttEvent.Error("Speech recognition is not available on this device."))
            close()
            return@callbackFlow
        }

        val speechRecognizer = withContext(Dispatchers.Main) {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        recognizer = speechRecognizer

        val listener = object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.firstResult()?.let { trySend(SttEvent.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                val text = results?.firstResult()
                trySend(if (text != null) SttEvent.Final(text) else SttEvent.Error("No speech recognized."))
                close()
            }

            override fun onError(error: Int) {
                trySend(SttEvent.Error(errorMessage(error)))
                close()
            }

            override fun onEndOfSpeech() {
                trySend(SttEvent.EndOfSpeech)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            languageTag?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
        }

        withContext(Dispatchers.Main) {
            speechRecognizer.setRecognitionListener(listener)
            speechRecognizer.startListening(intent)
        }

        awaitClose {
            recognizer = null
            runCatching {
                speechRecognizer.stopListening()
                speechRecognizer.destroy()
            }
        }
    }.flowOn(Dispatchers.Main)

    override fun stop() {
        runCatching { recognizer?.stopListening() }
    }

    private fun Bundle.firstResult(): String? =
        getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during recognition."
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that — please try again."
        SpeechRecognizer.ERROR_AUDIO -> "Couldn't access the microphone."
        else -> "Speech recognition failed."
    }
}
