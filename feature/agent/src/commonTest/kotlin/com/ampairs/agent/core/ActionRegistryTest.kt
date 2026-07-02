package com.ampairs.agent.core

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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the Phase 0 wiring: [ActionRegistry] is populated from the injected
 * `Map<String, ActionHandler>` (assembled by Metro multibinding in production) and routes/dispatches
 * correctly. Pure unit test — fake handlers, no DB, KMP-safe (no MockK).
 */
class ActionRegistryTest {

    /** Minimal fake handler that records the last dispatched action and returns a canned result. */
    private class FakeHandler(
        override val moduleName: String,
        override val supportedActions: List<ActionDescriptor>,
        private val onExecute: suspend (AgentAction) -> ActionResult = {
            ActionResult.Success("ok:${it.actionType}")
        },
    ) : ActionHandler {
        var lastAction: AgentAction? = null
        override suspend fun execute(action: AgentAction): ActionResult {
            lastAction = action
            return onExecute(action)
        }
    }

    private fun descriptor(module: String, type: ActionType) = ActionDescriptor(
        actionType = type,
        moduleName = module,
        description = "$type on $module",
        parameters = listOf(
            ActionParameter("query", ParameterType.STRING, required = true, "Search query"),
        ),
    )

    private fun registryWith(vararg handlers: FakeHandler): ActionRegistry =
        ActionRegistry(handlers.associateBy { it.moduleName })

    @Test
    fun dispatch_routesToTheHandlerForTheActionsModule() = runTest {
        val invoice = FakeHandler("invoice", listOf(descriptor("invoice", ActionType.COUNT)))
        val customer = FakeHandler("customer", listOf(descriptor("customer", ActionType.SEARCH)))
        val registry = registryWith(invoice, customer)

        val result = registry.dispatch(AgentAction(ActionType.COUNT, "invoice"))

        assertTrue(result is ActionResult.Success)
        assertEquals("ok:${ActionType.COUNT}", (result as ActionResult.Success).summary)
        assertEquals(ActionType.COUNT, invoice.lastAction?.actionType)
        assertNull(customer.lastAction) // the other handler was not touched
    }

    @Test
    fun dispatch_passesActionParamsThrough() = runTest {
        val customer = FakeHandler("customer", listOf(descriptor("customer", ActionType.SEARCH)))
        val registry = registryWith(customer)

        registry.dispatch(AgentAction(ActionType.SEARCH, "customer", mapOf("query" to "Ramesh")))

        assertEquals("Ramesh", customer.lastAction?.params?.get("query"))
    }

    @Test
    fun dispatch_unknownModule_returnsErrorWithoutThrowing() = runTest {
        val registry = registryWith(
            FakeHandler("invoice", listOf(descriptor("invoice", ActionType.COUNT))),
        )

        val result = registry.dispatch(AgentAction(ActionType.COUNT, "does-not-exist"))

        assertTrue(result is ActionResult.Error)
        assertTrue((result as ActionResult.Error).message.contains("does-not-exist"))
    }

    @Test
    fun dispatch_handlerThrowing_isCaughtAndReportedAsError() = runTest {
        val boom = FakeHandler(
            moduleName = "invoice",
            supportedActions = listOf(descriptor("invoice", ActionType.COUNT)),
            onExecute = { throw IllegalStateException("db unavailable") },
        )
        val registry = registryWith(boom)

        val result = registry.dispatch(AgentAction(ActionType.COUNT, "invoice"))

        assertTrue(result is ActionResult.Error)
        assertTrue((result as ActionResult.Error).message.contains("db unavailable"))
    }

    @Test
    fun getAllActions_aggregatesDescriptorsAcrossHandlers() {
        val registry = registryWith(
            FakeHandler("invoice", listOf(descriptor("invoice", ActionType.COUNT))),
            FakeHandler(
                "customer",
                listOf(
                    descriptor("customer", ActionType.SEARCH),
                    descriptor("customer", ActionType.CREATE),
                ),
            ),
        )

        val all = registry.getAllActions()

        assertEquals(3, all.size)
        assertEquals(setOf("invoice", "customer"), registry.getModuleNames().toSet())
    }

    @Test
    fun getHandler_returnsHandlerOrNull() {
        val invoice = FakeHandler("invoice", listOf(descriptor("invoice", ActionType.COUNT)))
        val registry = registryWith(invoice)

        assertEquals(invoice, registry.getHandler("invoice"))
        assertNull(registry.getHandler("ghost"))
    }

    @Test
    fun emptyRegistry_dispatchReturnsErrorAndCapabilitiesIsStillRenderable() = runTest {
        val registry = ActionRegistry(emptyMap())

        assertTrue(registry.getAllActions().isEmpty())
        assertTrue(registry.dispatch(AgentAction(ActionType.COUNT, "invoice")) is ActionResult.Error)
        // generateCapabilitiesPrompt must not throw on an empty registry
        assertTrue(registry.generateCapabilitiesPrompt().contains("Available actions"))
    }

    @Test
    fun generateCapabilitiesPrompt_listsModulesActionsAndParams() {
        val registry = registryWith(
            FakeHandler("invoice", listOf(descriptor("invoice", ActionType.SEARCH))),
        )

        val prompt = registry.generateCapabilitiesPrompt()

        assertTrue(prompt.contains("Module: invoice"))
        assertTrue(prompt.contains(ActionType.SEARCH.name))
        assertTrue(prompt.contains("query"))
    }
}
