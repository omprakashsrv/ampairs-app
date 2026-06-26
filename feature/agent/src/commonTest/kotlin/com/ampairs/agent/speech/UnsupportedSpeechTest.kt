package com.ampairs.agent.speech

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnsupportedSpeechTest {

    @Test
    fun stt_isUnavailable_andEmitsSingleError() = runTest {
        assertFalse(UnsupportedSpeechToText.isAvailable)
        val events = UnsupportedSpeechToText.listen().toList()
        assertEquals(1, events.size)
        assertTrue(events.single() is SttEvent.Error)
        UnsupportedSpeechToText.stop() // no-op, must not throw
    }

    @Test
    fun tts_isUnavailable_andSpeakIsNoOp() = runTest {
        assertFalse(NoOpTextToSpeech.isAvailable)
        NoOpTextToSpeech.speak("hello") // must not throw
        NoOpTextToSpeech.stop()
    }
}
