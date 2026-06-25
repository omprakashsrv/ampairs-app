package com.ampairs.agent.speech.whisper

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin

/**
 * Computes the **log-mel spectrogram** Whisper's encoder expects, from 16 kHz mono PCM. Mirrors
 * OpenAI Whisper's `log_mel_spectrogram` (n_fft=400, hop=160, 80 mels, 30 s → 3000 frames) using the
 * mel filterbank shipped in [WhisperAssets].
 *
 * Uses a precomputed DFT (n_fft=400 is not a power of two) over the 201 retained frequency bins —
 * heavier than an FFT but simple and correct; acceptable for a one-shot ≤30 s utterance. Output is a
 * flat `[nMel * N_FRAMES]` row-major array, ready to reshape to the `[1, 80, 3000]` model input.
 *
 * Runtime-tuned on device (tensor layout / normalization may need adjustment per the exact tflite graph).
 */
class WhisperFeatureExtractor(private val assets: WhisperAssets) {

    private val nMel = assets.nMel
    private val nFreq = assets.nFft // retained bins = n_fft/2 + 1 = 201

    private val hann = FloatArray(N_FFT) { (0.5 - 0.5 * cos(2.0 * PI * it / N_FFT)).toFloat() }

    // Precomputed DFT basis: cos/sin[k * N_FFT + n] for k in 0..nFreq-1, n in 0..N_FFT-1.
    private val cosT = FloatArray(nFreq * N_FFT)
    private val sinT = FloatArray(nFreq * N_FFT)

    init {
        for (k in 0 until nFreq) {
            val base = k * N_FFT
            for (n in 0 until N_FFT) {
                val a = -2.0 * PI * k * n / N_FFT
                cosT[base + n] = cos(a).toFloat()
                sinT[base + n] = sin(a).toFloat()
            }
        }
    }

    fun logMel(pcm: FloatArray): FloatArray {
        val samples = FloatArray(N_SAMPLES)
        pcm.copyInto(samples, 0, 0, minOf(pcm.size, N_SAMPLES))

        val mel = FloatArray(nMel * N_FRAMES)
        val frame = FloatArray(N_FFT)

        for (f in 0 until N_FRAMES) {
            val offset = f * HOP
            for (i in 0 until N_FFT) {
                val idx = offset + i
                frame[i] = if (idx < N_SAMPLES) samples[idx] * hann[i] else 0f
            }
            for (k in 0 until nFreq) {
                val base = k * N_FFT
                var re = 0f
                var im = 0f
                for (n in 0 until N_FFT) {
                    val s = frame[n]
                    re += s * cosT[base + n]
                    im += s * sinT[base + n]
                }
                val power = re * re + im * im
                for (m in 0 until nMel) {
                    mel[m * N_FRAMES + f] += assets.melFilters[m * nFreq + k] * power
                }
            }
        }

        var maxVal = Float.NEGATIVE_INFINITY
        for (i in mel.indices) {
            val v = log10(if (mel[i] < 1e-10f) 1e-10f else mel[i])
            mel[i] = v
            if (v > maxVal) maxVal = v
        }
        val floor = maxVal - 8.0f
        for (i in mel.indices) {
            val v = if (mel[i] < floor) floor else mel[i]
            mel[i] = (v + 4.0f) / 4.0f
        }
        return mel
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val N_FFT = 400
        const val HOP = 160
        const val N_FRAMES = 3000
        const val N_SAMPLES = 480_000 // 30 s
    }
}
