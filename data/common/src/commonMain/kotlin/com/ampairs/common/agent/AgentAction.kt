package com.ampairs.common.agent

import kotlinx.serialization.Serializable

@Serializable
data class AgentAction(
    val actionType: ActionType,
    val moduleName: String,
    val params: Map<String, String> = emptyMap(),
)
