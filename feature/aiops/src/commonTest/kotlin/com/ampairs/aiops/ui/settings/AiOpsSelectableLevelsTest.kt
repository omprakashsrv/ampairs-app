package com.ampairs.aiops.ui.settings

import com.ampairs.common.aiops.AiOpsAutonomyLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the app-tier autonomy levels offered by the settings UI. L3/L4 (`AUTO_EXECUTE`/`AUTONOMOUS`)
 * are backend-managed — surfacing them on device would let a user claim an autonomy the app can't
 * honor, so this pins the exact selectable set.
 */
class AiOpsSelectableLevelsTest {

    @Test
    fun `offers exactly the three app-tier levels in order`() {
        assertEquals(
            listOf(
                AiOpsAutonomyLevel.OBSERVE,
                AiOpsAutonomyLevel.RECOMMEND,
                AiOpsAutonomyLevel.AUTO_CORRECT,
            ),
            AiOpsSelectableLevels,
        )
    }

    @Test
    fun `never exposes the backend-managed tiers`() {
        assertFalse(AiOpsAutonomyLevel.AUTO_EXECUTE in AiOpsSelectableLevels)
        assertFalse(AiOpsAutonomyLevel.AUTONOMOUS in AiOpsSelectableLevels)
    }

    @Test
    fun `includes the default level so the current value is always shown`() {
        assertTrue(AiOpsAutonomyLevel.Default in AiOpsSelectableLevels)
    }
}
