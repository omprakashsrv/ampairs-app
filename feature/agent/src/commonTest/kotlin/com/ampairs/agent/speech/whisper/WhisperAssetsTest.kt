package com.ampairs.agent.speech.whisper

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.write
import kotlin.test.Test
import kotlin.test.assertEquals

class WhisperAssetsTest {

    private fun le(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte(),
    )

    private fun leFloat(f: Float): ByteArray = le(f.toRawBits())

    /** Writes a synthetic filters_vocab binary and asserts [WhisperAssets.load] parses it back exactly. */
    @Test
    fun parses_filters_and_vocab_round_trip() {
        val nMel = 2
        val nFft = 2
        val filters = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val vocab = listOf("a", "Ġb", "héllo") // includes the space marker + multi-byte UTF-8

        val bytes = buildList {
            addAll(le(0x4E455355).toList()) // magic 'USEN' (ignored by the parser)
            addAll(le(nMel).toList())
            addAll(le(nFft).toList())
            filters.forEach { addAll(leFloat(it).toList()) }
            addAll(le(vocab.size).toList())
            vocab.forEach { token ->
                val b = token.encodeToByteArray()
                addAll(le(b.size).toList())
                addAll(b.toList())
            }
        }.toByteArray()

        val path = Path(SystemTemporaryDirectory, "whisper_assets_test.bin")
        SystemFileSystem.sink(path).buffered().use { it.write(bytes) }

        val assets = WhisperAssets.load(path.toString())

        assertEquals(nMel, assets.nMel)
        assertEquals(nFft, assets.nFft)
        assertEquals(filters.toList(), assets.melFilters.toList())
        assertEquals(vocab, assets.vocab.toList())
    }
}
