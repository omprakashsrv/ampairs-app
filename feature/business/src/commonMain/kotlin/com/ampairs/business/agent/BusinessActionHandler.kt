package com.ampairs.business.agent

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
 * Named agent action for the business module. The business profile is a singleton, so the natural
 * action is READ (summarize the profile) rather than COUNT. moduleName "business" matches the
 * existing BusinessQuerySchema key.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("business")
class BusinessActionHandler(
    private val agentDao: BusinessAgentDao,
) : ActionHandler {

    override val moduleName = "business"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.READ -> reportProfile()
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    private suspend fun reportProfile(): ActionResult {
        val profile = agentDao.getProfile()
            ?: return ActionResult.Success("No business profile is set up yet.")
        val location = listOfNotNull(
            profile.city?.ifBlank { null },
            profile.state?.ifBlank { null },
        ).joinToString(", ")
        val parts = buildList {
            add("Business: ${profile.name}")
            if (profile.businessType.isNotBlank()) add("type ${profile.businessType}")
            if (location.isNotBlank()) add("in $location")
            if (profile.currency.isNotBlank()) add("currency ${profile.currency}")
        }
        return ActionResult.Success(parts.joinToString(", ") + ".")
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.READ,
                moduleName = "business",
                description = "Show this workspace's business profile (name, type, location, currency). Use for \"what's my business\", \"my business details\", \"what currency\".",
                parameters = emptyList(),
            ),
        )
    }
}
