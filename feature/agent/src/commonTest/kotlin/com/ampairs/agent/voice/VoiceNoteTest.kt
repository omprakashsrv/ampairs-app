package com.ampairs.agent.voice

import kotlin.test.Test
import kotlin.test.assertEquals

class VoiceNoteTest {

    @Test
    fun formatsSubMinuteDurations() {
        assertEquals("0:00", formatVoiceNoteDuration(0L))
        assertEquals("0:03", formatVoiceNoteDuration(3_200L))   // rounds to nearest second
        assertEquals("0:05", formatVoiceNoteDuration(4_600L))
        assertEquals("0:59", formatVoiceNoteDuration(59_000L))
    }

    @Test
    fun formatsMinutesWithPaddedSeconds() {
        assertEquals("1:00", formatVoiceNoteDuration(60_000L))
        assertEquals("1:05", formatVoiceNoteDuration(65_000L))
        assertEquals("12:30", formatVoiceNoteDuration(750_000L))
    }

    @Test
    fun negativeDurationClampsToZero() {
        assertEquals("0:00", formatVoiceNoteDuration(-500L))
    }
}
