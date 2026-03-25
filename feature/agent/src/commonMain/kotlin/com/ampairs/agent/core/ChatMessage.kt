package com.ampairs.agent.core

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = 0L,
    val actionResult: ActionResultSummary? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
)

@Serializable
data class ActionResultSummary(
    val actionType: String,
    val moduleName: String,
    val success: Boolean,
    val navigationRouteDescription: String? = null,
    val navigationRouteData: Map<String, String>? = null,
)
