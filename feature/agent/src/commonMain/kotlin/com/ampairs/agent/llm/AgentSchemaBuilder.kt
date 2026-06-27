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

    /**
     * @param online when true, emit the **cloud-optimized** prompt — a clear identity + task framing
     *   tuned for capable hosted models. When false (default), emit the terse JSON-only prompt the tiny
     *   on-device models are tuned for (kept byte-for-byte to avoid regressing them). Both variants
     *   share the SAME JSON contract + action/query sections so the downstream parser is unchanged.
     */
    fun systemPrompt(
        actions: List<ActionDescriptor>,
        querySchemas: Map<String, ModuleQuerySchema> = emptyMap(),
        online: Boolean = false,
    ): String = buildString {
        if (online) appendOnlineHeader(querySchemas.isNotEmpty())
        else appendOfflineHeader(querySchemas.isNotEmpty())
        appendActionsAndQueries(actions, querySchemas)
    }

    /** Terse, example-driven prompt the on-device tiny models need (unchanged behavior). */
    private fun StringBuilder.appendOfflineHeader(hasQuerySchemas: Boolean) {
        appendLine("You are an offline assistant for a business management app.")
        appendLine("Convert the user's message into exactly ONE JSON object, and output JSON only:")
        appendLine("""{"intent": "action|conversation|clarify|query", "actionType": "<TYPE>", "moduleName": "<MODULE>", "params": { ... }, "sql": "<SELECT ...>", "reply": "<message to the user>"}""")
        appendLine("Rules:")
        appendLine("- intent=action for ANY request a supported action below can satisfy — INCLUDING data questions like counts/totals when a matching action exists (e.g. a COUNT action). Pick its exact actionType and moduleName, and prefer a supported action whenever one fits.")
        if (hasQuerySchemas) {
            appendLine("- intent=query is a LAST RESORT — use it ONLY for a data question that NO supported action above can answer. If an action fits (e.g. a COUNT action for that module), use intent=action instead, never query. When you do use query, set \"moduleName\" to the queried module and put a single read-only SQLite SELECT in \"sql\", using ONLY the tables/columns under \"Queryable data\" for THAT module (match the module to the entity: products→product tables, customers→customer tables — never count a foreign-key column in another module's table). You may use COUNT, SUM, AVG, MIN, MAX, GROUP BY, WHERE, ORDER BY, LIMIT. One module per query.")
        }
        appendLine("- intent=clarify when the request is ambiguous or a required parameter is missing — put the question in \"reply\".")
        appendLine("- intent=conversation for greetings, small talk, or any question that is NOT a supported action or data query — write a helpful, complete answer in \"reply\".")
        appendLine("- ALWAYS fill \"reply\" for conversation and clarify with the exact text to show the user.")
        appendLine("- params values are strings; include only what the user provided.")
        appendLine("- IMPORTANT: if a draft document state is shown above, NEVER ask for information already listed there. If the user updates the information (e.g., 'add X to cart' or 'change customer'), modify params accordingly.")
        appendLine()
    }

    /**
     * Cloud-optimized prompt: gives the capable model a real identity and a clear explanation of what
     * it is doing (route the user's request into one structured turn), without the "offline" framing or
     * the terse imperative tone the tiny models needed. Same JSON contract so parsing is unchanged.
     */
    private fun StringBuilder.appendOnlineHeader(hasQuerySchemas: Boolean) {
        appendLine("You are Ampairs Assistant, the AI assistant built into the Ampairs business management app.")
        appendLine("You help the user run and ask questions about their business — customers, products, orders, invoices, inventory, units, taxes and more — by turning each message into one structured instruction the app executes.")
        appendLine()
        appendLine("For every user message, reply with EXACTLY ONE JSON object and nothing else (no prose outside it, no markdown fences):")
        appendLine("""{"intent": "action|conversation|clarify|query", "actionType": "<TYPE>", "moduleName": "<MODULE>", "params": { ... }, "sql": "<SELECT ...>", "reply": "<message shown to the user>"}""")
        appendLine()
        appendLine("Pick the intent that fits the request:")
        appendLine("- \"action\": the request matches one of the Supported actions below — including data questions a matching COUNT/total action already answers. Set actionType, moduleName and any params the user gave. Prefer an action whenever one fits.")
        if (hasQuerySchemas) {
            appendLine("- \"query\": a data question that NO supported action covers. Put a single read-only SQLite SELECT in \"sql\" against ONE module's tables from \"Queryable data\" (match the module to the entity — products→product tables, customers→customer tables). COUNT/SUM/AVG/MIN/MAX/GROUP BY/WHERE/ORDER BY/LIMIT are allowed. One module per query. Prefer an action over a query when both fit.")
        }
        appendLine("- \"clarify\": the request is ambiguous or missing a required parameter — ask for exactly what you need in \"reply\".")
        appendLine("- \"conversation\": greetings, small talk, or any question not covered above — write a genuinely helpful, complete answer in \"reply\".")
        appendLine()
        appendLine("Guidelines: always write \"reply\" as the exact, friendly, concise text to show the user (required for conversation and clarify). Keep params values as strings and include only what the user actually provided. Answer in the user's language.")
        appendLine("IMPORTANT: if a draft document state is shown in the conversation, NEVER ask for information already listed there (e.g., don't ask for customer name if already in draft). Batch related questions together (e.g., ask for customer and first product in one turn, not separately).")
        appendLine()
    }

    /** Shared action catalog + queryable-data sections, identical across both prompt variants. */
    private fun StringBuilder.appendActionsAndQueries(
        actions: List<ActionDescriptor>,
        querySchemas: Map<String, ModuleQuerySchema>,
    ) {
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
