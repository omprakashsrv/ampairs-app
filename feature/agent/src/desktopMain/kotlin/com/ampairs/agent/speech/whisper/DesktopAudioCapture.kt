package com.ampairs.agent.speech.whisper

import com.ampairs.common.config.AppPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Desktop mic capture over `javax.sound.sampled` — opens a [TargetDataLine] at **16 kHz, 16-bit, mono,
 * signed little-endian**, reads on [Dispatchers.IO], and emits normalized float chunks. [stop] ends the
 * read loop, which completes the [callbackFlow]; `awaitClose` releases the line.
 *
 * The input device is the user's persisted choice (Assistant settings → Microphone, stored as the
 * mixer name); null/unknown falls back to the system default line.
 */
class DesktopAudioCapture(
    private val prefs: AppPreferencesDataStore,
) : AudioCapture {

    private val format = AudioFormat(16_000f, 16, 1, true, false)

    @Volatile private var running = false
    @Volatile private var line: TargetDataLine? = null

    override val isAvailable: Boolean
        get() = runCatching {
            AudioSystem.isLineSupported(DataLine.Info(TargetDataLine::class.java, format))
        }.getOrDefault(false)

    override fun stream(): Flow<FloatArray> = callbackFlow {
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val dataLine = openSelectedLine(info).apply {
            open(format)
            start()
        }
        line = dataLine
        running = true

        val reader = launch(Dispatchers.IO) {
            val buffer = ByteArray(BUFFER_BYTES)
            try {
                while (running && isActive) {
                    val read = dataLine.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    trySend(buffer.toFloats(read))
                }
            } finally {
                close() // completes the flow so the engine stops collecting
            }
        }

        awaitClose {
            running = false
            reader.cancel()
            runCatching {
                dataLine.stop()
                dataLine.flush()
                dataLine.close()
            }
            line = null
        }
    }

    /**
     * Open the [TargetDataLine] for the user-selected mixer (by name), falling back to the system
     * default line when nothing is selected or the saved device is gone (unplugged).
     */
    private suspend fun openSelectedLine(info: DataLine.Info): TargetDataLine {
        val selectedName = runCatching { prefs.getSelectedAudioInputDeviceId().first() }.getOrNull()
        if (!selectedName.isNullOrBlank()) {
            val mixerInfo = AudioSystem.getMixerInfo().firstOrNull { it.name == selectedName }
            if (mixerInfo != null) {
                runCatching { AudioSystem.getTargetDataLine(format, mixerInfo) }
                    .getOrNull()?.let { return it }
            }
        }
        return AudioSystem.getLine(info) as TargetDataLine
    }

    override fun stop() {
        running = false
        runCatching { line?.stop() }
    }

    /** 16-bit LE PCM → normalized floats (-1f..1f). [validBytes] is what the line actually filled. */
    private fun ByteArray.toFloats(validBytes: Int): FloatArray {
        val samples = validBytes / 2
        val out = FloatArray(samples)
        var b = 0
        for (i in 0 until samples) {
            val lo = this[b].toInt() and 0xFF
            val hi = this[b + 1].toInt() // sign-extended high byte
            out[i] = ((hi shl 8) or lo) / 32768f
            b += 2
        }
        return out
    }

    private companion object {
        const val BUFFER_BYTES = 4096
    }
}
