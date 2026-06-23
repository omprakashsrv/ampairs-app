package com.ampairs.agent.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class BackendSelectorTest {

    private class FakeBackend(
        override val id: String,
        private val supportedBackendIds: Set<String> = setOf(id),
    ) : LlmBackend {
        override fun supports(model: ModelDescriptor): Boolean = model.backendId in supportedBackendIds
        override fun create(): LlmEngine = throw UnsupportedOperationException("not needed for selection tests")
    }

    private val e2b = ModelCatalog.GEMMA_3N_E2B          // backendId = "litert-lm"
    private val qwen = ModelCatalog.QWEN2_5_3B           // backendId = "llamacpp"

    @Test
    fun emptyBackends_returnsNull() {
        assertNull(BackendSelector.select(emptyList(), e2b, listOf("litert-lm", "llamacpp")))
    }

    @Test
    fun prefersFirstOrderedIdThatSupportsModel() {
        val litert = FakeBackend("litert-lm")
        val llama = FakeBackend("llamacpp")
        val picked = BackendSelector.select(listOf(llama, litert), e2b, listOf("litert-lm", "llamacpp"))
        assertSame(litert, picked)
    }

    @Test
    fun skipsPreferredIdThatDoesNotSupportModel_thenUsesNext() {
        // "litert-lm" backend registered but only supports litert models; model is a GGUF (llamacpp).
        val litert = FakeBackend("litert-lm")
        val llama = FakeBackend("llamacpp")
        val picked = BackendSelector.select(listOf(litert, llama), qwen, listOf("litert-lm", "llamacpp"))
        assertSame(llama, picked)
    }

    @Test
    fun fallsBackToModelDeclaredBackend_whenNonePreferredFit() {
        val llama = FakeBackend("llamacpp")
        // No preferred ids match anything registered → fall back to model.backendId.
        val picked = BackendSelector.select(listOf(llama), qwen, listOf("nonexistent-engine"))
        assertSame(llama, picked)
    }

    @Test
    fun fallsBackToAnySupportingBackend_asLastResort() {
        val onlySupportsLitert = FakeBackend(id = "custom", supportedBackendIds = setOf("litert-lm"))
        val picked = BackendSelector.select(listOf(onlySupportsLitert), e2b, emptyList())
        assertSame(onlySupportsLitert, picked)
    }

    @Test
    fun returnsNull_whenNoBackendSupportsModel() {
        val onlyLlama = FakeBackend("llamacpp")
        assertNull(BackendSelector.select(listOf(onlyLlama), e2b, listOf("litert-lm")))
    }

    @Test
    fun orderedIdsAreHonoredEvenWhenModelDeclaresDifferentBackend() {
        // A hypothetical universal backend that supports any model, preferred by id.
        val universal = FakeBackend(id = "universal", supportedBackendIds = setOf("litert-lm", "llamacpp"))
        val llama = FakeBackend("llamacpp")
        val picked = BackendSelector.select(listOf(llama, universal), qwen, listOf("universal", "llamacpp"))
        assertEquals("universal", picked?.id)
    }
}
