package com.ampairs.agent.speech.whisper

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhisperFeatureExtractorTest {

    /** Tiny mel filterbank (values irrelevant for silence: power is 0, so filter·power = 0). */
    private fun extractor(nMel: Int, nFreq: Int) =
        WhisperFeatureExtractor(
            WhisperAssets(nMel = nMel, nFft = nFreq, melFilters = FloatArray(nMel * nFreq) { 0.5f }, vocab = emptyArray()),
        )

    @Test
    fun output_is_flat_nMel_by_frames() {
        val nMel = 4
        val mel = extractor(nMel, 3).logMel(FloatArray(16_000)) // 1 s of silence
        assertEquals(nMel * WhisperFeatureExtractor.N_FRAMES, mel.size)
    }

    @Test
    fun silence_normalizes_to_uniform_floor() {
        // Silence → 0 power → log10(1e-10) = -10 everywhere → max = -10, floor = -18,
        // none clamped → (-10 + 4) / 4 = -1.5 for every cell. Validates the whole
        // power → log → clamp → normalize pipeline deterministically.
        val mel = extractor(2, 3).logMel(FloatArray(8_000))
        for (v in mel) assertTrue(abs(v - (-1.5f)) < 1e-3f, "expected -1.5 but was $v")
    }

    @Test
    fun pads_short_input_without_error() {
        // Far shorter than the 30 s window: must pad to N_SAMPLES and still return full frames.
        val mel = extractor(3, 3).logMel(FloatArray(10))
        assertEquals(3 * WhisperFeatureExtractor.N_FRAMES, mel.size)
    }
}
