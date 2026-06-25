package com.ampairs.agent.llm

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ModuleQuerySchema

/**
 * Turns the registry's [ActionDescriptor]s into an engine-agnostic [OutputSchema] (constrains the
 * model to a valid AgentAction) plus a [systemPrompt] that lists the supported actions. The resolver
 * (T029) feeds both to whatever engine the `ProviderRegistry` selected — neither the resolver nor the
 * builder knows or cares which engine. Pure Kotlin (commonMain), unit-tested.
 *
 * When [ModuleQuerySchema]s are supplied (the queryable modules, FR-016), the builder also enables the
 * **query** intent: the model may answer a data question by emitting a read-only `SELECT` against one
 * module's curated schema. The SQL is validated (`SafeSqlValidator`) and run read-only
 * (`SafeQueryService`) downstream, so the model is free to use COUNT/SUM/AVG/GROUP BY/WHERE/ORDER BY.
 */
object AgentSchemaBuilder {

    /** Base intents; "query" is appended only when queryable schemas exist (so the enum stays tight). */
    val INTENTS = listOf("action", "conversation", "clarify")

    fun build(
        actions: List<ActionDescriptor>,
        querySchemas: Map<String, ModuleQuerySchema> = emptyMap(),
    ): OutputSchema {
        val actionTypes = actions.map { it.actionType.name }.distinct().sorted()
        // moduleName enum must cover both action modules and queryable modules (a module may be
        // queryable without exposing any typed action).
        val modules = (actions.map { it.moduleName } + querySchemas.keys).distinct().sorted()
        val paramsByModule = actions
            .groupBy { it.moduleName }
            .mapValues { (_, list) -> list.flatMap { d -> d.parameters.map { it.name } }.distinct().sorted() }
            .toList().sortedBy { it.first }.toMap()
        val intents = if (querySchemas.isEmpty()) INTENTS else INTENTS + "query"
        return OutputSchema(
            intents = intents,
            actionTypes = actionTypes,
            modules = modules,
            paramsByModule = paramsByModule,
        )
    }

    fun systemPrompt(
        actions: List<ActionDescriptor>,
        querySchemas: Map<String, ModuleQuerySchema> = emptyMap(),
    ): String = buildString {
        appendLine("You are an offline assistant for a business management app.")
        appendLine("Convert the user's message into exactly ONE JSON object, and output JSON only:")
        appendLine("""{"intent": "action|conversation|clarify|query", "actionType": "<TYPE>", "moduleName": "<MODULE>", "params": { ... }, "sql": "<SELECT ...>", "reply": "<message to the user>"}""")
        appendLine("Rules:")
        appendLine("- intent=action only for a supported action listed below; pick its exact actionType and moduleName.")
        if (querySchemas.isNotEmpty()) {
            appendLine("- intent=query to ANSWER A QUESTION ABOUT THE DATA (counts, totals, averages, lists, \"how many\", \"top N\", \"this month\"). Set \"moduleName\" to the queried module and put a single read-only SQLite SELECT in \"sql\". Use only the tables/columns listed under \"Queryable data\". You may use COUNT, SUM, AVG, MIN, MAX, GROUP BY, WHERE, ORDER BY, LIMIT. Each query targets ONE module only.")
        }
        appendLine("- intent=clarify when the request is ambiguous or a required parameter is missing — put the question in \"reply\".")
        appendLine("- intent=conversation for greetings, small talk, or any question that is NOT a supported action or data query — write a helpful, complete answer in \"reply\".")
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
        if (querySchemas.isNotEmpty()) {
            appendLine()
            appendLine()
            appendLine("Queryable data (for intent=query; SELECT only, one module per query):")
            querySchemas.toList().sortedBy { it.first }.forEach { (_, schema) ->
                append(schema.toPromptText())
            }
            append("Example: {\"intent\":\"query\",\"moduleName\":\"customer\",\"sql\":\"SELECT COUNT(*) AS count FROM customers\"}")
        }
    }
}
