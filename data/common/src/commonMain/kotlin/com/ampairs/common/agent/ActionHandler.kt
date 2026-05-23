package com.ampairs.common.agent

interface ActionHandler {
    val moduleName: String
    val supportedActions: List<ActionDescriptor>
    suspend fun execute(action: AgentAction): ActionResult
}
