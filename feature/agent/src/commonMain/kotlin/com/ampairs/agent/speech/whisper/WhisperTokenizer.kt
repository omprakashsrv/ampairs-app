package com.ampairs.agent.speech.whisper

/**
 * Detokenizes the LiteRT Whisper graph's output ids into text using the BPE vocab from [WhisperAssets].
 * The graph emits the text tokens for a window followed by the end-of-text token; special/timestamp
 * tokens (id ≥ vocab size) are skipped and decoding stops at end-of-text.
 *
 * Token strings are concatenated as stored in the vocab (whisper.cpp stores byte-level UTF-8). The
 * GPT-2 space marker is normalized to a real space. Exact special-token boundaries are runtime-tuned
 * on device against the specific model.
 */
class WhisperTokenizer(private val assets: WhisperAssets) {

    fun decode(ids: IntArray): String {
        val sb = StringBuilder()
        for (id in ids) {
            if (id < 0 || id == END_OF_TEXT) break
            if (id < assets.vocab.size) sb.append(assets.vocab[id])
            // ids >= vocab size are SOT / language / notimestamps / timestamp tokens — skip
        }
        return sb.toString()
            .replace('Ġ', ' ') // GPT-2 byte-level space marker
            .replace('Ċ', '\n') // newline marker
            .trim()
    }

    private companion object {
        // Multilingual Whisper end-of-text token id (<|endoftext|>).
        const val END_OF_TEXT = 50257
    }
}
