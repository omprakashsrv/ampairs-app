package com.ampairs.agent.llm

import kotlin.test.Test
import kotlin.test.assertEquals

class ModelManagerFormatTest {

    @Test
    fun formatsMegabytesAndGigabytes() {
        assertEquals("286.1 MB", formatModelSize(300_000_000L))
        assertEquals("2.8 GB", formatModelSize(3_000_000_000L))
        assertEquals("4.1 GB", formatModelSize(4_400_000_000L))
        assertEquals("1 MB", formatModelSize(1_048_576L))
    }

    @Test
    fun nonPositiveSizeIsBlank() {
        assertEquals("", formatModelSize(0L))
        assertEquals("", formatModelSize(-5L))
    }

    @Test
    fun downloadingFractionIsClampedAndSafe() {
        assertEquals(0.5f, ModelInstallStatus.Downloading(50, 100).fraction)
        assertEquals(1f, ModelInstallStatus.Downloading(150, 100).fraction)   // clamped
        assertEquals(0f, ModelInstallStatus.Downloading(10, -1).fraction)     // unknown total
    }
}
