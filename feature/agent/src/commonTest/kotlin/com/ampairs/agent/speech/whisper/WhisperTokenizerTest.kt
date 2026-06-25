package com.ampairs.agent.speech.whisper

import kotlin.test.Test
import kotlin.test.assertEquals

class WhisperTokenizerTest {

    private fun tokenizer(vararg vocab: String) =
        WhisperTokenizer(WhisperAssets(nMel = 1, nFft = 1, melFilters = FloatArray(1), vocab = arrayOf(*vocab)))

    @Test
    fun concatenates_tokens_and_normalizes_space_marker() {
        // 'Ġ' is the GPT-2 byte-level space marker → becomes a real space.
        val tok = tokenizer("Hello", "Ġworld", "!")
        assertEquals("Hello world!", tok.decode(intArrayOf(0, 1, 2)))
    }

    @Test
    fun stops_at_end_of_text_token() {
        val tok = tokenizer("Hi", "there")
        // 50257 = <|endoftext|>; tokens after it are ignored.
        assertEquals("Hi", tok.decode(intArrayOf(0, 50257, 1)))
    }

    @Test
    fun skips_special_ids_beyond_vocab() {
        val tok = tokenizer("a", "b")
        // 99 is a special/timestamp id (>= vocab size) → skipped, not appended.
        assertEquals("ab", tok.decode(intArrayOf(0, 99, 1)))
    }

    @Test
    fun trims_surrounding_whitespace() {
        val tok = tokenizer("Ġhi", "Ġthere")
        assertEquals("hi there", tok.decode(intArrayOf(0, 1)))
    }

    @Test
    fun empty_input_yields_empty_string() {
        assertEquals("", tokenizer("a").decode(intArrayOf()))
    }
}
