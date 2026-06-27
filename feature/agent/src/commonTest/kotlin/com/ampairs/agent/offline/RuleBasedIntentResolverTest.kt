package com.ampairs.agent.offline

import com.ampairs.agent.core.ResolvedIntent
import com.ampairs.common.agent.ActionType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure tests for the offline rule-based resolver — focused on the new "create a bill/invoice for
 * {customer}" routing (and that it doesn't cannibalise the generic create patterns).
 */
class RuleBasedIntentResolverTest {

    private val resolver = RuleBasedIntentResolver()

    private suspend fun resolve(text: String): ResolvedIntent =
        resolver.resolve(text, conversationHistory = emptyList(), availableActions = emptyList())

    @Test
    fun createBillFor_routesToInvoiceCreateWithCustomerParam() = runTest {
        val intent = resolve("create a bill for Ramesh Traders")
        assertTrue(intent is ResolvedIntent.Action)
        val action = (intent as ResolvedIntent.Action).action
        assertEquals(ActionType.CREATE, action.actionType)
        assertEquals("invoice", action.moduleName)
        assertEquals("Ramesh Traders", action.params["customer"])
    }

    @Test
    fun makeInvoiceFor_isAlsoRecognised() = runTest {
        val intent = resolve("make an invoice for Priya")
        assertTrue(intent is ResolvedIntent.Action)
        val action = (intent as ResolvedIntent.Action).action
        assertEquals(ActionType.CREATE, action.actionType)
        assertEquals("invoice", action.moduleName)
        assertEquals("Priya", action.params["customer"])
    }

    @Test
    fun createCustomer_stillRoutesToCustomerModule_notInvoice() = runTest {
        val intent = resolve("add customer John")
        assertTrue(intent is ResolvedIntent.Action)
        val action = (intent as ResolvedIntent.Action).action
        assertEquals(ActionType.CREATE, action.actionType)
        assertEquals("customer", action.moduleName)
    }

    @Test
    fun countInvoices_routesToCountInvoice() = runTest {
        val intent = resolve("how many invoices")
        assertTrue(intent is ResolvedIntent.Action)
        val action = (intent as ResolvedIntent.Action).action
        assertEquals(ActionType.COUNT, action.actionType)
        assertEquals("invoice", action.moduleName)
    }

    @Test
    fun unrecognised_fallsBackToConversation() = runTest {
        val intent = resolve("tell me a joke")
        assertTrue(intent is ResolvedIntent.Conversation)
    }
}
