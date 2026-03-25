package com.ampairs.agent.core

/**
 * Central registry that collects all ActionHandlerProviders.
 *
 * DI-agnostic: this class has zero imports from any DI framework.
 * Modules register via [register] — works from Koin, Dagger, or manual init.
 *
 * Handlers are instantiated lazily — only when their module is first dispatched.
 */
class ActionRegistry {

    private val providers = mutableMapOf<String, ActionHandlerProvider>()

    fun register(provider: ActionHandlerProvider) {
        providers[provider.moduleName] = provider
    }

    /** All supported actions across all modules (metadata only, no instantiation). */
    fun getAllActions(): List<ActionDescriptor> =
        providers.values.flatMap { it.supportedActions }

    /** All registered module names. */
    fun getModuleNames(): List<String> = providers.keys.toList()

    /** Find the provider for a specific module. */
    fun getProvider(moduleName: String): ActionHandlerProvider? = providers[moduleName]

    /**
     * Execute an action by routing to the correct handler.
     * The handler is lazily instantiated on first use.
     */
    suspend fun dispatch(action: AgentAction): ActionResult {
        val provider = providers[action.moduleName]
            ?: return ActionResult.Error("Module '${action.moduleName}' is not available.")

        return try {
            provider.handler.execute(action)
        } catch (e: Exception) {
            ActionResult.Error(
                "Failed to execute ${action.actionType} on ${action.moduleName}: ${e.message}"
            )
        }
    }

    /** Evict a cached handler to free memory. */
    fun unloadModule(moduleName: String) {
        providers.remove(moduleName)
    }

    /**
     * Generate a capabilities summary for the LLM system prompt.
     * This tells the LLM what actions are available.
     */
    fun generateCapabilitiesPrompt(): String = buildString {
        appendLine("Available actions:")
        providers.values.forEach { provider ->
            appendLine("\nModule: ${provider.moduleName}")
            provider.supportedActions.forEach { action ->
                appendLine("  - ${action.actionType}: ${action.description}")
                action.parameters.forEach { param ->
                    val req = if (param.required) "required" else "optional"
                    appendLine("      ${param.name} (${param.type}, $req): ${param.description}")
                }
            }
        }
    }
}
