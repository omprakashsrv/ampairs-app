package com.ampairs.agent.offline

import com.ampairs.agent.core.ActionRegistry
import com.ampairs.agent.core.ResolvedIntent
import com.ampairs.agent.llm.LlmEngine
import com.ampairs.agent.llm.LlmParams
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.OutputSchema
import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.ParameterType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Validates the on-device resolver's parse + strict-validate logic (FR-003 / SC-003) with a fake
 * engine returning canned JSON — no real model. Confirms: valid action parses; unregistered
 * action/module and missing-required-param become Clarification (never a wrong action); non-JSON
 * prose becomes a conversational reply (empty output clarifies); JSON embedded in prose is extracted.
 */
class LlmIntentResolverTest {

    private class FakeHandler(
        override val moduleName: String,
        override val supportedActions: List<ActionDescriptor>,
    ) : ActionHandler {
        override suspend fun execute(action: AgentAction): ActionResult = ActionResult.Success("ok")
    }

    /** Returns a queued sequence of responses (one per generateConstrained call). */
    private class FakeEngine(private val responses: ArrayDeque<String>) : LlmEngine {
        var calls = 0
        override suspend fun load(model: ModelDescriptor, params: LlmParams) {}
        override fun isLoaded() = true
        override suspend fun generate(prompt: String, maxTokens: Int) = ""
        override suspend fun generateConstrained(prompt: String, schema: OutputSchema, maxTokens: Int): String {
            calls++
            return if (responses.isEmpty()) "" else responses.removeFirst()
        }
        override suspend fun close() {}
    }

    private val registry = ActionRegistry(
        mapOf(
            "invoice" to FakeHandler(
                "invoice",
                listOf(
                    ActionDescriptor(ActionType.CREATE, "invoice", "Create a bill",
                        listOf(ActionParameter("customer", ParameterType.STRING, required = true, "Customer"))),
                    ActionDescriptor(ActionType.COUNT, "invoice", "Count invoices", emptyList()),
                ),
            ),
        ),
    )

    private fun resolverReturning(vararg responses: String): Pair<LlmIntentResolver, FakeEngine> {
        val engine = FakeEngine(ArrayDeque(responses.toList()))
        return LlmIntentResolver(registry, engineProvider = { engine }) to engine
    }

    @Test
    fun validActionJson_parsesToAction() = runTest {
        val (resolver, _) = resolverReturning(
            """{"intent":"action","actionType":"CREATE","moduleName":"invoice","params":{"customer":"Ramesh"}}""",
        )
        val intent = resolver.resolve("make a bill for Ramesh", emptyList(), registry.getAllActions())
        assertTrue(intent is ResolvedIntent.Action)
        intent as ResolvedIntent.Action
        assertEquals(ActionType.CREATE, intent.action.actionType)
        assertEquals("invoice", intent.action.moduleName)
        assertEquals("Ramesh", intent.action.params["customer"])
    }

    @Test
    fun jsonEmbeddedInProse_isExtracted() = runTest {
        val (resolver, _) = resolverReturning(
            """Sure! Here you go: {"intent":"action","actionType":"COUNT","moduleName":"invoice"} hope that helps""",
        )
        val intent = resolver.resolve("how many invoices", emptyList(), registry.getAllActions())
        assertTrue(intent is ResolvedIntent.Action)
        assertEquals(ActionType.COUNT, (intent as ResolvedIntent.Action).action.actionType)
    }

    @Test
    fun unregisteredModule_becomesClarification() = runTest {
        val (resolver, _) = resolverReturning(
            """{"intent":"action","actionType":"CREATE","moduleName":"spaceship"}""",
        )
        val intent = resolver.resolve("create a spaceship", emptyList(), registry.getAllActions())
        assertTrue(intent is ResolvedIntent.Clarification)
    }

    @Test
    fun missingRequiredParam_becomesClarification() = runTest {
        val (resolver, _) = resolverReturning(
            """{"intent":"action","actionType":"CREATE","moduleName":"invoice","params":{}}""",
        )
        val intent = resolver.resolve("make a bill", emptyList(), registry.getAllActions())
        assertTrue(intent is ResolvedIntent.Clarification)
        assertTrue((intent as ResolvedIntent.Clarification).question.contains("customer"))
    }

    @Test
    fun nonJsonOutput_becomesConversation() = runTest {
        // When the model answers in prose instead of intent JSON, surface that prose as a
        // conversational reply (single call, no re-ask) so plain chat works.
        val (resolver, engine) = resolverReturning("Hello! How can I help your business today?")
        val intent = resolver.resolve("hi", emptyList(), registry.getAllActions())
        assertTrue(intent is ResolvedIntent.Conversation)
        assertEquals("Hello! How can I help your business today?", (intent as ResolvedIntent.Conversation).response)
        assertEquals(1, engine.calls)
    }

    @Test
    fun emptyOutput_becomesClarification() = runTest {
        val (resolver, _) = resolverReturning("")
        val intent = resolver.resolve("???", emptyList(), registry.getAllActions())
        assertTrue(intent is ResolvedIntent.Clarification)
    }

    @Test
    fun conversationIntent_becomesConversation() = runTest {
        val (resolver, _) = resolverReturning(
            """{"intent":"conversation","reply":"Hello! How can I help?"}""",
        )
        val intent = resolver.resolve("hi", emptyList(), registry.getAllActions())
        assertTrue(intent is ResolvedIntent.Conversation)
    }

    @Test
    fun nullEngine_yieldsClarification_notCrash() = runTest {
        val resolver = LlmIntentResolver(registry, engineProvider = { null })
        val intent = resolver.resolve("anything", emptyList(), registry.getAllActions())
        assertTrue(intent is ResolvedIntent.Clarification)
    }
}
