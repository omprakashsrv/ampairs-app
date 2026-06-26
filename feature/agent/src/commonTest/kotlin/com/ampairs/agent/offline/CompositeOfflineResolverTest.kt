package com.ampairs.agent.offline

import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.core.ResolvedIntent
import com.ampairs.common.agent.ActionDescriptor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the offline-resolver selection logic (T030, FR-004/FR-012): when an LLM model is selected
 * it is used (or its not-ready/error states surface) and there is **no** silent fall-back to the
 * rule-based resolver; rule-based runs only when no model is selected. Fake resolvers tag their result
 * so the test can tell which path ran.
 */
class CompositeOfflineResolverTest {

    private class TaggingResolver(private val tag: String) : IntentResolver {
        override suspend fun resolve(
            userMessage: String,
            conversationHistory: List<ChatMessage>,
            availableActions: List<ActionDescriptor>,
        ): ResolvedIntent = ResolvedIntent.Conversation(tag)
    }

    private class ThrowingResolver : IntentResolver {
        override suspend fun resolve(
            userMessage: String,
            conversationHistory: List<ChatMessage>,
            availableActions: List<ActionDescriptor>,
        ): ResolvedIntent = throw IllegalStateException("model exploded")
    }

    private val llm = TaggingResolver("llm")
    private val rule = TaggingResolver("rule")

    private suspend fun tagOf(composite: CompositeOfflineResolver): String =
        (composite.resolve("hi", emptyList(), emptyList()) as ResolvedIntent.Conversation).response

    @Test
    fun usesLlm_whenSelectedAndReady() = runTest {
        val composite = CompositeOfflineResolver(llm, rule, isLlmReady = { true }, isLlmSelected = { true })
        assertEquals("llm", tagOf(composite))
    }

    @Test
    fun clarifies_whenSelectedButNotReady_noRuleFallback() = runTest {
        val composite = CompositeOfflineResolver(llm, rule, isLlmReady = { false }, isLlmSelected = { true })
        val intent = composite.resolve("hi", emptyList(), emptyList())
        // Must surface "getting ready" — NOT silently run the rule-based resolver.
        assertTrue(intent is ResolvedIntent.Clarification, "expected a not-ready clarification, got $intent")
    }

    @Test
    fun errors_whenSelectedAndLlmThrows_noRuleFallback() = runTest {
        val composite = CompositeOfflineResolver(ThrowingResolver(), rule, isLlmReady = { true }, isLlmSelected = { true })
        val intent = composite.resolve("hi", emptyList(), emptyList())
        // A failed inference surfaces an error — NOT a rule-based answer.
        assertTrue(intent is ResolvedIntent.Error, "expected an error, got $intent")
    }

    @Test
    fun usesRule_whenNoModelSelected() = runTest {
        val composite = CompositeOfflineResolver(llm, rule, isLlmReady = { false }, isLlmSelected = { false })
        assertEquals("rule", tagOf(composite))
    }

    @Test
    fun biasesToSelected_whenSelectionCheckThrows_noRuleFallback() = runTest {
        // If the selection check itself fails we must NOT default to rule-based.
        val composite = CompositeOfflineResolver(
            llm, rule,
            isLlmReady = { true },
            isLlmSelected = { throw IllegalStateException("datastore hiccup") },
        )
        assertEquals("llm", tagOf(composite))
    }
}
