package com.ampairs.agent.speech.whisper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import kotlin.concurrent.Volatile

/**
 * iOS mic capture for the Whisper pipeline via **AVAudioEngine**. Taps the input node at its hardware
 * format and emits normalized 16 kHz mono float chunks.
 *
 * Resampling is a simple ratio **decimation** (pick every Nth sample) of channel 0 — robust and
 * dependency-free; proper anti-aliased conversion (AVAudioConverter) is a runtime quality tune. The
 * [stop]/cancel path removes the tap, stops the engine, and deactivates the audio session.
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioCapture : AudioCapture {

    @Volatile private var engine: AVAudioEngine? = null

    override val isAvailable: Boolean = true

    override fun stream(): Flow<FloatArray> = callbackFlow {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryRecord, null)
        session.setActive(true, null)

        val audioEngine = AVAudioEngine()
        val input = audioEngine.inputNode
        val format = input.inputFormatForBus(0uL)
        val ratio = format.sampleRate / TARGET_RATE // e.g. 48000/16000 = 3.0

        input.installTapOnBus(0uL, BUFFER_SIZE, format) { buffer, _ ->
            val channel = buffer?.floatChannelData?.get(0) ?: return@installTapOnBus
            val frames = buffer.frameLength.toInt()
            if (frames <= 0 || ratio <= 0.0) return@installTapOnBus
            val outCount = (frames / ratio).toInt()
            if (outCount <= 0) return@installTapOnBus
            val out = FloatArray(outCount) { i -> channel[(i * ratio).toInt()] }
            trySend(out)
        }

        engine = audioEngine
        audioEngine.prepare()
        val started = memScoped {
            val err = alloc<ObjCObjectVar<NSError?>>()
            audioEngine.startAndReturnError(err.ptr)
        }
        if (!started) {
            close(IllegalStateException("AVAudioEngine failed to start"))
            return@callbackFlow
        }

        awaitClose {
            input.removeTapOnBus(0uL)
            runCatching { audioEngine.stop() }
            runCatching { session.setActive(false, null) }
            engine = null
        }
    }

    override fun stop() {
        runCatching { engine?.stop() }
    }

    private companion object {
        const val TARGET_RATE = 16_000.0
        const val BUFFER_SIZE = 4096u
    }
}
