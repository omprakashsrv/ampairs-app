package com.ampairs.agent.speech.whisper

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

/**
 * Parsed Whisper assets — the mel filterbank and the BPE vocab — loaded from the
 * `filters_vocab_multilingual.bin` companion (whisper.cpp / nyadla-sys format). Used only by the
 * LiteRT `.tflite` path (the desktop whisper.cpp engine has these built into the ggml model).
 *
 * Binary layout (little-endian): magic(int32) · nMel(int32) · nFft(int32) ·
 * filters[nMel*nFft](float32) · nVocab(int32) · {len(int32) · bytes}×nVocab.
 */
class WhisperAssets(
    val nMel: Int,
    val nFft: Int,
    /** Row-major [nMel][nFft] mel filterbank. */
    val melFilters: FloatArray,
    /** Token id → token string (byte-level). */
    val vocab: Array<String>,
) {
    companion object {
        fun load(path: String): WhisperAssets {
            val bytes = SystemFileSystem.source(Path(path)).buffered().use { it.readByteArray() }
            var p = 0
            fun int(): Int {
                val v = (bytes[p].toInt() and 0xFF) or
                    ((bytes[p + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[p + 2].toInt() and 0xFF) shl 16) or
                    ((bytes[p + 3].toInt() and 0xFF) shl 24)
                p += 4
                return v
            }
            fun float(): Float = Float.fromBits(int())

            int() // magic ('USEN') — not validated
            val nMel = int()
            val nFft = int()
            val filters = FloatArray(nMel * nFft) { float() }
            val nVocab = int()
            val vocab = Array(nVocab) {
                val len = int()
                val s = bytes.decodeToString(p, p + len)
                p += len
                s
            }
            return WhisperAssets(nMel, nFft, filters, vocab)
        }
    }
}
