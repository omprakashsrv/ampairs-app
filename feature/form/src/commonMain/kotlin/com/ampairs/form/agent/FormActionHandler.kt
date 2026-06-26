package com.ampairs.form.agent

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import com.ampairs.form.data.repository.ConfigRepository
import kotlinx.coroutines.flow.first

/**
 * Form-agnostic action handler for voice-to-form field filling. Enables users to say
 * "set customer name to John" and have the form field automatically populated.
 *
 * This handler bridges voice intents to form field mutations, supporting:
 * - GET_SCHEMA: retrieve field definitions for a form (for model context)
 * - FILL_FIELD: set a single form field value
 * - SUGGEST_FIELDS: suggest which fields to fill based on entity type
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("form")
class FormActionHandler(
    private val configRepository: ConfigRepository,
) : ActionHandler {

    override val moduleName = "form"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.READ -> getFormSchema(action.params)
        ActionType.UPDATE -> fillField(action.params)
        ActionType.SEARCH -> suggestFields(action.params)
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    /**
     * Retrieve the form schema for a given entity type. Used by the LLM to understand
     * available fields and their types before generating field-fill suggestions.
     */
    private suspend fun getFormSchema(params: Map<String, String>): ActionResult {
        val entityType = params["entityType"]
            ?: return ActionResult.NeedsInput("Which entity type? (e.g., 'customer', 'product')", listOf("entityType"))

        val schema = configRepository.observeSchema(entityType).first()
        return if (schema != null) {
            val fieldSummary = buildString {
                appendLine("Available fields for $entityType:")
                schema.visibleFields().forEach { field ->
                    val mandatory = if (field.mandatory) "[required]" else ""
                    appendLine("  - ${field.fieldKey}: ${field.dataType} $mandatory (${field.displayName})")
                }
            }
            ActionResult.Success(fieldSummary)
        } else {
            ActionResult.Error("Form schema not found for entity type '$entityType'.")
        }
    }

    /**
     * Fill a single form field with a value. Returns the field name and value for UI confirmation.
     * The actual form binding is handled by FormValueState on the client composable.
     */
    private suspend fun fillField(params: Map<String, String>): ActionResult {
        val entityType = params["entityType"]
            ?: return ActionResult.NeedsInput("Which entity type?", listOf("entityType"))
        val fieldKey = params["fieldKey"]
            ?: return ActionResult.NeedsInput("Which field?", listOf("fieldKey"))
        val value = params["value"]
            ?: return ActionResult.NeedsInput("What value?", listOf("value"))

        val schema = configRepository.observeSchema(entityType).first()
        if (schema == null) {
            return ActionResult.Error("Form schema not found for entity type '$entityType'.")
        }

        val field = schema.fields.find { it.fieldKey == fieldKey }
        if (field == null) {
            return ActionResult.Error("Field '$fieldKey' not found in '$entityType' form.")
        }

        if (!field.visible || !field.enabled) {
            return ActionResult.Error("Field '$fieldKey' is not available for editing.")
        }

        // Return a success result that the form host can use to update the state.
        // The actual field binding happens via FormFieldFillIntent on the composable side.
        return ActionResult.Success(
            summary = "Filled field '${field.displayName}' with value '$value'.",
            data = mapOf(
                "entityType" to entityType,
                "fieldKey" to fieldKey,
                "fieldValue" to value,
                "fieldDisplayName" to field.displayName,
            )
        )
    }

    /**
     * Suggest which fields to fill for a given entity type, returning a list of commonly-edited fields.
     * Useful for guiding the user on what to provide next.
     */
    private suspend fun suggestFields(params: Map<String, String>): ActionResult {
        val entityType = params["entityType"]
            ?: return ActionResult.NeedsInput("Which entity type?", listOf("entityType"))

        val schema = configRepository.observeSchema(entityType).first()
        return if (schema != null) {
            val mandatory = schema.mandatoryFields()
            val summary = if (mandatory.isEmpty()) {
                "No required fields for $entityType."
            } else {
                buildString {
                    appendLine("Required fields for $entityType:")
                    mandatory.forEach { field ->
                        appendLine("  - ${field.fieldKey}: ${field.displayName}")
                    }
                }
            }
            ActionResult.Success(summary)
        } else {
            ActionResult.Error("Form schema not found for entity type '$entityType'.")
        }
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.READ,
                moduleName = "form",
                description = "Get form schema and available fields for an entity type",
                parameters = listOf(
                    ActionParameter("entityType", ParameterType.STRING, required = true, "Entity type (e.g., 'customer', 'product')"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.UPDATE,
                moduleName = "form",
                description = "Fill a form field with a value",
                parameters = listOf(
                    ActionParameter("entityType", ParameterType.STRING, required = true, "Entity type"),
                    ActionParameter("fieldKey", ParameterType.STRING, required = true, "Field key to fill"),
                    ActionParameter("value", ParameterType.STRING, required = true, "Value to set"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.SEARCH,
                moduleName = "form",
                description = "Suggest required fields for an entity type",
                parameters = listOf(
                    ActionParameter("entityType", ParameterType.STRING, required = true, "Entity type"),
                ),
            ),
        )
    }
}
