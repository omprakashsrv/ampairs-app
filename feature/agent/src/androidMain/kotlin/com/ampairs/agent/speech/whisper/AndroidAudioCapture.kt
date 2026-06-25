package com.ampairs.agent.speech.whisper

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

/**
 * Android mic capture via [AudioRecord] at **16 kHz mono PCM16**, emitting normalized float chunks.
 * [stop] ends the read loop, which completes the [callbackFlow]; `awaitClose` releases the recorder.
 * RECORD_AUDIO is declared in the agent manifest and gated by `MicPermissionController` before [stream].
 */
class AndroidAudioCapture : AudioCapture {

    private val minBuffer = AudioRecord.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )

    @Volatile private var running = false
    @Volatile private var recorder: AudioRecord? = null

    override val isAvailable: Boolean
        get() = minBuffer > 0

    @SuppressLint("MissingPermission") // RECORD_AUDIO gated by MicPermissionController before listen()
    override fun stream(): Flow<FloatArray> = callbackFlow {
        val bufferBytes = maxOf(minBuffer, MIN_BUFFER_BYTES)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            close(IllegalStateException("Microphone unavailable"))
            return@callbackFlow
        }
        recorder = record
        running = true

        val reader = launch(Dispatchers.IO) {
            // Audible "start listening" cue (the native recognizer beeps; the raw AudioRecord path
            // doesn't). Play it, then wait out the tone before recording so the mic doesn't catch it.
            playStartTone()
            delay(START_TONE_GAP_MS)
            if (!running || !isActive) {
                close()
                return@launch
            }
            val started = runCatching { record.startRecording() }.isSuccess
            if (!started || record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                close(IllegalStateException("Microphone failed to start"))
                return@launch
            }
            val buf = ShortArray(bufferBytes / 2)
            try {
                while (running && isActive) {
                    val n = record.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    val floats = FloatArray(n) { buf[it] / 32768f }
                    trySend(floats)
                }
            } finally {
                close()
            }
        }

        awaitClose {
            running = false
            reader.cancel()
            runCatching {
                record.stop()
                record.release()
            }
            recorder = null
        }
    }

    override fun stop() {
        running = false
        runCatching { recorder?.stop() }
    }

    /** Short beep so the user knows the mic just opened. Best-effort — never fails capture. */
    private suspend fun playStartTone() {
        val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME) }.getOrNull() ?: return
        runCatching { tone.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_MS) }
        delay(TONE_MS.toLong())
        runCatching { tone.release() }
    }

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val MIN_BUFFER_BYTES = 4096
        const val TONE_VOLUME = 80 // 0..100
        const val TONE_MS = 150
        const val START_TONE_GAP_MS = 250L // let the beep finish before the mic opens
    }
}
