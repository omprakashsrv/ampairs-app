package com.ampairs.agent.speech.whisper

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
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
        record.startRecording()

        val reader = launch(Dispatchers.IO) {
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

    private companion object {
        const val SAMPLE_RATE = 16_000
        const val MIN_BUFFER_BYTES = 4096
    }
}
