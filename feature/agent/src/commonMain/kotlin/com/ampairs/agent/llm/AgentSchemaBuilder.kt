package com.ampairs.agent.llm

import com.ampairs.common.agent.ActionDescriptor

/**
 * Turns the registry's [ActionDescriptor]s into an engine-agnostic [OutputSchema] (constrains the
 * model to a valid AgentAction) plus a [systemPrompt] that lists the supported actions. The resolver
 * (T029) feeds both to whatever engine the `ProviderRegistry` selected — neither the resolver nor the
 * builder knows or cares which engine. Pure Kotlin (commonMain), unit-tested.
 */
object AgentSchemaBuilder {

    val INTENTS = listOf("action", "conversation", "clarify")

    fun build(actions: List<ActionDescriptor>): OutputSchema {
        val actionTypes = actions.map { it.actionType.name }.distinct().sorted()
        val modules = actions.map { it.moduleName }.distinct().sorted()
        val paramsByModule = actions
            .groupBy { it.moduleName }
            .mapValues { (_, list) -> list.flatMap { d -> d.parameters.map { it.name } }.distinct().sorted() }
            .toList().sortedBy { it.first }.toMap()
        return OutputSchema(
            intents = INTENTS,
            actionTypes = actionTypes,
            modules = modules,
            paramsByModule = paramsByModule,
        )
    }

    fun systemPrompt(actions: List<ActionDescriptor>): String = buildString {
        appendLine("You are an offline assistant for a business management app.")
        appendLine("Convert the user's message into exactly ONE JSON object, and output JSON only:")
        appendLine("""{"intent": "action|conversation|clarify", "actionType": "<TYPE>", "moduleName": "<MODULE>", "params": { ... }, "reply": "<message to the user>"}""")
        appendLine("Rules:")
        appendLine("- intent=action only for a supported action listed below; pick its exact actionType and moduleName.")
        appendLine("- intent=clarify when the request is ambiguous or a required parameter is missing — put the question in \"reply\".")
        appendLine("- intent=conversation for greetings, small talk, or any question that is NOT a supported action (including questions about the business) — write a helpful, complete answer in \"reply\".")
        appendLine("- ALWAYS fill \"reply\" for conversation and clarify with the exact text to show the user.")
        appendLine("- params values are strings; include only what the user provided.")
        appendLine()
        appendLine("Supported actions:")
        actions.groupBy { it.moduleName }.toList().sortedBy { it.first }.forEach { (module, list) ->
            appendLine("Module \"$module\":")
            list.sortedBy { it.actionType.name }.forEach { a ->
                val params = a.parameters.joinToString(", ") { p -> p.name + if (p.required) "*" else "" }
                appendLine("  - ${a.actionType.name}: ${a.description}${if (params.isNotBlank()) " [params: $params]" else ""}")
            }
        }
        append("(* = required)")
    }
}
