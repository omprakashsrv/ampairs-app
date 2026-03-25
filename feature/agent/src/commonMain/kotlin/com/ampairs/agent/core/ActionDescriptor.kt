package com.ampairs.agent.core

data class ActionDescriptor(
    val actionType: ActionType,
    val moduleName: String,
    val description: String,
    val parameters: List<ActionParameter>,
)

data class ActionParameter(
    val name: String,
    val type: ParameterType,
    val required: Boolean,
    val description: String,
)

enum class ParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
    DATE,
    LIST_STRING,
}
