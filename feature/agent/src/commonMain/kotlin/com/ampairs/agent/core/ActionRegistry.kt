package com.ampairs.agent.core

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import dev.zacsweers.metro.Inject

/**
 * Central registry of all module [ActionHandler]s.
 *
 * Handlers are contributed via Metro multibinding — each handler is annotated
 * `@ContributesIntoMap(WorkspaceScope::class)` + `@ActionHandlerKey("<module>")`, and Metro
 * assembles them into the `Map<String, ActionHandler>` injected here. Adding a new module's handler
 * therefore requires no change to this class.
 *
 * Resolved inside the WorkspaceScope graph (its handlers depend on workspace-scoped repositories),
 * so a fresh registry with the current workspace's handlers is created per workspace session.
 */
@Inject
class ActionRegistry(
    private val handlers: Map<String, ActionHandler>,
) {

    /** All supported actions across all registered modules. */
    fun getAllActions(): List<ActionDescriptor> =
        handlers.values.flatMap { it.supportedActions }

    /** All registered module names. */
    fun getModuleNames(): List<String> = handlers.keys.toList()

    /** Find the handler for a specific module, if registered. */
    fun getHandler(moduleName: String): ActionHandler? = handlers[moduleName]

    /** Execute an action by routing to the correct handler. */
    suspend fun dispatch(action: AgentAction): ActionResult {
        val handler = handlers[action.moduleName]
            ?: return ActionResult.Error("Module '${action.moduleName}' is not available.")

        return try {
            handler.execute(action)
        } catch (e: Exception) {
            ActionResult.Error(
                "Failed to execute ${action.actionType} on ${action.moduleName}: ${e.message}"
            )
        }
    }

    /**
     * Capabilities summary for an LLM system prompt — tells the model which actions are available.
     * Consumed by the on-device intent resolver (Phase 2).
     */
    fun generateCapabilitiesPrompt(): String = buildString {
        appendLine("Available actions:")
        handlers.values.forEach { handler ->
            appendLine("\nModule: ${handler.moduleName}")
            handler.supportedActions.forEach { action ->
                appendLine("  - ${action.actionType}: ${action.description}")
                action.parameters.forEach { param ->
                    val req = if (param.required) "required" else "optional"
                    appendLine("      ${param.name} (${param.type}, $req): ${param.description}")
                }
            }
        }
    }
}
