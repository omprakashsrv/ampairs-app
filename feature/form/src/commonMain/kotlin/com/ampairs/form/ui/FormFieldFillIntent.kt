package com.ampairs.form.ui

/**
 * Intent to fill a form field from the agent chat. Sent by FormAgentChatBubble when
 * the user voice command results in a field fill action.
 *
 * The form edit screen (e.g., CustomerGroupFormScreen) receives this intent
 * and updates its FormValueState accordingly.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyFormScreen(
 *     onFieldFill: (fieldKey: String, value: String) -> Unit = { key, value ->
 *         // Update your form state with the new value
 *         viewModel.updateField(key, value)
 *     }
 * ) {
 *     FormAgentChatBubble(
 *         entityType = "customer",
 *         onFieldFill = onFieldFill,
 *     )
 * }
 * ```
 */
data class FormFieldFillIntent(
    val fieldKey: String,
    val fieldValue: String,
    val fieldDisplayName: String? = null,
)
