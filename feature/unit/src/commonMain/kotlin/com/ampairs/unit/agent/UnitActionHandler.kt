package com.ampairs.unit.agent

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
 * Named agent actions for the unit module. Deterministic counts the model can prefer over free SQL.
 * moduleName "unit" matches the existing UnitQuerySchema key.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("unit")
class UnitActionHandler(
    private val agentDao: UnitAgentDao,
) : ActionHandler {

    override val moduleName = "unit"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.COUNT -> ActionResult.Success("You have ${agentDao.countActive()} unit(s).")
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.COUNT,
                moduleName = "unit",
                description = "Count of units of measure. Use for \"how many units\", \"number of units\".",
                parameters = emptyList(),
            ),
        )
    }
}
