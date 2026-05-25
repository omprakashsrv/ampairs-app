package com.ampairs.common.agent

class ActionHandlerProvider(
    val moduleName: String,
    val supportedActions: List<ActionDescriptor>,
    private val factory: () -> ActionHandler,
) {
    val handler: ActionHandler by lazy { factory() }
}
