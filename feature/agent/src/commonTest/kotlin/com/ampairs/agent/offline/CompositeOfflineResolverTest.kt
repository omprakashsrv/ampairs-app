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
 * Verifies the offline-resolver selection logic (T030, FR-004/FR-012): LLM when ready, rule-based
 * otherwise, and rule-based fallback when the LLM path throws. Fake resolvers tag their result so the
 * test can tell which path ran.
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

    private suspend fun resolveWith(composite: CompositeOfflineResolver): String =
        (composite.resolve("hi", emptyList(), emptyList()) as ResolvedIntent.Conversation).response

    @Test
    fun usesLlm_whenReady() = runTest {
        val composite = CompositeOfflineResolver(llm, rule, isLlmReady = { true })
        assertEquals("llm", resolveWith(composite))
    }

    @Test
    fun usesRule_whenNotReady() = runTest {
        val composite = CompositeOfflineResolver(llm, rule, isLlmReady = { false })
        assertEquals("rule", resolveWith(composite))
    }

    @Test
    fun fallsBackToRule_whenLlmThrows() = runTest {
        val composite = CompositeOfflineResolver(ThrowingResolver(), rule, isLlmReady = { true })
        assertEquals("rule", resolveWith(composite))
    }
}
