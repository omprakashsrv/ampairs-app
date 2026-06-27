package com.ampairs.agent.speech.whisper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoiceActivityDetectorTest {

    private val frame = 320 // 20 ms @ 16 kHz
    private fun silence() = FloatArray(frame) { 0.001f }
    private fun speech() = FloatArray(frame) { 0.1f }

    @Test
    fun detectsSpeechThenEndpointsAfterTrailingSilence() {
        val vad = VoiceActivityDetector()
        // Lead-in silence: no endpoint, no speech yet.
        repeat(10) { assertEquals(VadDecision.CONTINUE, vad.accept(silence())) }
        assertFalse(vad.speechStarted)

        // Speech: arms after the onset debounce.
        repeat(20) { assertEquals(VadDecision.CONTINUE, vad.accept(speech())) }
        assertTrue(vad.speechStarted, "speech should be detected")

        // Trailing silence shorter than the 1.2 s hangover → still CONTINUE.
        repeat(40) { assertEquals(VadDecision.CONTINUE, vad.accept(silence())) } // 40*20 = 800 ms

        // Cross the hangover (≥1.2 s total trailing silence) → ENDPOINT.
        var endpointed = false
        repeat(40) { if (vad.accept(silence()) == VadDecision.ENDPOINT) endpointed = true }
        assertTrue(endpointed, "should endpoint after ~1.2 s of trailing silence")
    }

    @Test
    fun givesUpWhenNoSpeechIsEverDetected() {
        val vad = VoiceActivityDetector()
        var timedOut = false
        // 10 s window = 500 frames; feed a bit more to be safe.
        repeat(600) { if (vad.accept(silence()) == VadDecision.NO_SPEECH_TIMEOUT) timedOut = true }
        assertTrue(timedOut, "should report no-speech timeout")
        assertFalse(vad.speechStarted)
    }

    @Test
    fun shortClickDoesNotTriggerSpeech() {
        val vad = VoiceActivityDetector() // onsetFrames = 3
        repeat(5) { vad.accept(silence()) }
        // A single loud frame (a click/tap) shouldn't pass the onset debounce.
        vad.accept(speech())
        vad.accept(silence())
        assertFalse(vad.speechStarted, "a 1-frame transient must not count as speech")
    }
}
