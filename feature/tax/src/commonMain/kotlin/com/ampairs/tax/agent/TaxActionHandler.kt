package com.ampairs.tax.agent

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Named agent actions for the tax module. moduleName "tax" matches the existing TaxQuerySchema key.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("tax")
class TaxActionHandler(
    private val agentDao: TaxAgentDao,
) : ActionHandler {

    override val moduleName = "tax"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.COUNT -> ActionResult.Success("You have ${agentDao.countActive()} tax code(s).")
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.COUNT,
                moduleName = "tax",
                description = "Count of tax codes (e.g. GST rates) set up in this workspace. Use for \"how many tax codes\".",
                parameters = emptyList(),
            ),
        )
    }
}
