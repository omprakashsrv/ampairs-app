package com.ampairs.agent.core

sealed class ActionResult {

    data class Success(
        val summary: String,
        val data: Any? = null,
        val navigationTarget: NavigationTarget? = null,
    ) : ActionResult()

    data class Error(val message: String) : ActionResult()

    data class NeedsInput(
        val question: String,
        val missingParams: List<String>,
    ) : ActionResult()
}

data class NavigationTarget(
    val routeDescription: String,
    val routeData: Map<String, String> = emptyMap(),
)
