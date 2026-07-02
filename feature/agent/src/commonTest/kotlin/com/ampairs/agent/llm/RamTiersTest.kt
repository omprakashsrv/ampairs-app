package com.ampairs.agent.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RamTiersTest {

    private val gb = 1024L * 1024 * 1024

    @Test
    fun under3gb_isRuleBasedOnly() {
        assertFalse(RamTiers.canRunLlm(2 * gb))
        assertNull(RamTiers.recommendedChatModelId(2 * gb))
        assertNull(RamTiers.recommendedChatModelId(0L)) // unknown RAM → rule-based
    }

    @Test
    fun midRange_picksE2b() {
        assertTrue(RamTiers.canRunLlm(4 * gb))
        assertEquals(ModelCatalog.GEMMA_3N_E2B.id, RamTiers.recommendedChatModelId(4 * gb))
    }

    @Test
    fun highRam_picksE4b() {
        assertEquals(ModelCatalog.GEMMA_3N_E4B.id, RamTiers.recommendedChatModelId(8 * gb))
    }

    @Test
    fun boundaries_areInclusiveAtTierStart() {
        assertEquals(ModelCatalog.GEMMA_3N_E2B.id, RamTiers.recommendedChatModelId(RamTiers.MIN_LLM_BYTES))
        assertEquals(ModelCatalog.GEMMA_3N_E4B.id, RamTiers.recommendedChatModelId(RamTiers.E4B_MIN_BYTES))
        assertNull(RamTiers.recommendedChatModelId(RamTiers.MIN_LLM_BYTES - 1))
    }
}
