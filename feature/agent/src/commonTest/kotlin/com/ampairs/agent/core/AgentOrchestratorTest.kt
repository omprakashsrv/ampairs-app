package com.ampairs.agent.core

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.CONFIRMED_PARAM
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Confirm-before-persist flow for money actions (FR-006 / US2). Pure unit test — fake handler +
 * fixed resolver, no DB/LLM. Mirrors the two-phase invoice CREATE: first dispatch proposes
 * [ActionResult.Confirm] (nothing written); the orchestrator only persists when the user-confirmed
 * `pendingAction` (carrying [CONFIRMED_PARAM]) is re-dispatched.
 */
class AgentOrchestratorTest {

    /** Two-phase handler: propose -> Confirm, then persist on the confirmed re-dispatch. */
    private class ConfirmingHandler(override val moduleName: String) : ActionHandler {
        override val supportedActions: List<ActionDescriptor> = emptyList()
        var persistCount = 0
        override suspend fun execute(action: AgentAction): ActionResult =
            if (action.params[CONFIRMED_PARAM] == "true") {
                persistCount++
                ActionResult.Success("saved")
            } else {
                ActionResult.Confirm(
                    summary = "Create invoice for Ramesh — total 100?",
                    pendingAction = action.copy(params = action.params + (CONFIRMED_PARAM to "true")),
                )
            }
    }

    private class FixedResolver(private val intent: ResolvedIntent) : IntentResolver {
        override suspend fun resolve(
            userMessage: String,
            conversationHistory: List<ChatMessage>,
            availableActions: List<ActionDescriptor>,
        ): ResolvedIntent = intent
    }

    private fun orchestrator(handler: ActionHandler, intent: ResolvedIntent): AgentOrchestrator =
        AgentOrchestrator(
            actionRegistry = ActionRegistry(mapOf(handler.moduleName to handler)),
            onlineResolver = FixedResolver(intent),
            offlineResolver = FixedResolver(intent),
        )

    @Test
    fun moneyAction_firstTurnProposes_andPersistsNothing() = runTest {
        val handler = ConfirmingHandler("invoice")
        val action = AgentAction(ActionType.CREATE, "invoice", mapOf("customer" to "Ramesh"))
        val orch = orchestrator(handler, ResolvedIntent.Action(action))

        val response = orch.processMessage("make a bill for Ramesh", emptyList(), isOnline = false)

        assertTrue(response.actionResult is ActionResult.Confirm)
        assertNotNull(response.pendingConfirmation)
        assertEquals("true", response.pendingConfirmation?.params?.get(CONFIRMED_PARAM))
        assertEquals(0, handler.persistCount) // FR-006: nothing persisted before confirm
    }

    @Test
    fun confirmAction_persistsExactlyOnce_andReportsSuccess() = runTest {
        val handler = ConfirmingHandler("invoice")
        val action = AgentAction(ActionType.CREATE, "invoice", mapOf("customer" to "Ramesh"))
        val orch = orchestrator(handler, ResolvedIntent.Action(action))

        val proposal = orch.processMessage("make a bill for Ramesh", emptyList(), isOnline = false)
        val pending = proposal.pendingConfirmation!!
        val confirmed = orch.confirmAction(pending)

        assertTrue(confirmed.actionResult is ActionResult.Success)
        assertNull(confirmed.pendingConfirmation) // confirmation consumed
        assertEquals(1, handler.persistCount)
    }

    @Test
    fun plainQuery_hasNoPendingConfirmation() = runTest {
        val handler = object : ActionHandler {
            override val moduleName = "invoice"
            override val supportedActions = emptyList<ActionDescriptor>()
            override suspend fun execute(action: AgentAction) = ActionResult.Success("3 invoices")
        }
        val action = AgentAction(ActionType.COUNT, "invoice")
        val orch = orchestrator(handler, ResolvedIntent.Action(action))

        val response = orch.processMessage("how many invoices", emptyList(), isOnline = false)

        assertTrue(response.actionResult is ActionResult.Success)
        assertNull(response.pendingConfirmation)
    }
}
