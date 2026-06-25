package com.ampairs.agent.speech.whisper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
 */
class DesktopAudioCapture : AudioCapture {

    private val format = AudioFormat(16_000f, 16, 1, true, false)

    @Volatile private var running = false
    @Volatile private var line: TargetDataLine? = null

    override val isAvailable: Boolean
        get() = runCatching {
            AudioSystem.isLineSupported(DataLine.Info(TargetDataLine::class.java, format))
        }.getOrDefault(false)

    override fun stream(): Flow<FloatArray> = callbackFlow {
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val dataLine = (AudioSystem.getLine(info) as TargetDataLine).apply {
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
