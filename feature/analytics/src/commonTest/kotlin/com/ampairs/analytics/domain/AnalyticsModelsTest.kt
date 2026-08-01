package com.ampairs.analytics.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsModelsTest {
    @Test
    fun `period has three grains`() {
        assertEquals(3, Period.entries.size)
    }
}
