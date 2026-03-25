package com.ampairs.agent.core

/**
 * Each business module implements one ActionHandler that declares
 * what operations it supports and how to execute them.
 *
 * The agent core discovers all handlers via ActionHandlerProvider.
 * Handlers are instantiated lazily — only when their module is first used.
 */
interface ActionHandler {

    /** Module identifier, e.g. "customer", "product", "order" */
    val moduleName: String

    /** List of actions this handler supports */
    val supportedActions: List<ActionDescriptor>

    /**
     * Execute an action. Returns ActionResult.
     * The handler is responsible for calling the correct repository method.
     */
    suspend fun execute(action: AgentAction): ActionResult
}
