package com.ampairs.agent.llm

import com.ampairs.agent.config.AssistantConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelCatalogTest {

    @Test
    fun ids_areUnique() {
        val ids = ModelCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "model ids must be unique")
    }

    @Test
    fun roles_areCovered() {
        assertTrue(ModelCatalog.byRole(ModelRole.INTENT).isNotEmpty())
        assertTrue(ModelCatalog.byRole(ModelRole.CHAT).size >= 2) // E2B + E4B tiers
        assertTrue(ModelCatalog.byRole(ModelRole.FALLBACK).isNotEmpty())
    }

    @Test
    fun byId_andByBackend_resolve() {
        assertNotNull(ModelCatalog.byId("function-gemma-270m"))
        assertNull(ModelCatalog.byId("does-not-exist"))
        assertTrue(ModelCatalog.byBackend("litert-lm").all { it.backendId == "litert-lm" })
        assertTrue(ModelCatalog.byBackend("llamacpp").isNotEmpty())
    }

    @Test
    fun ramTiers_areOrdered_e2bUnderE4b() {
        // The low-RAM chat tier must require less than the high-RAM tier (drives DeviceCapability gating).
        assertTrue(ModelCatalog.GEMMA_3N_E2B.estimatedPeakMemoryBytes < ModelCatalog.GEMMA_3N_E4B.estimatedPeakMemoryBytes)
    }

    @Test
    fun defaultConfig_pointsAtCatalogEntries_andSafeQueryIsOff() {
        val cfg = AssistantConfig.Default
        assertNotNull(ModelCatalog.byId(cfg.intentModelId))
        assertNotNull(ModelCatalog.byId(cfg.chatModelId))
        assertTrue(!cfg.safeQueryEnabled, "FR-016 SAFE_QUERY must default off")
        assertTrue(cfg.llmEnabled)
    }
}
