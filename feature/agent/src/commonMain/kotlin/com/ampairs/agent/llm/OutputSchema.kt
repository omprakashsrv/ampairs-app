package com.ampairs.agent.llm

/**
 * Engine-agnostic description of the constrained output the intent resolver expects — a valid
 * `AgentAction` JSON. Built by [AgentSchemaBuilder] from the registry's `ActionDescriptor`s. The
 * structured fields are the source of truth; each engine adapter renders them to its own constraint
 * format: [toJsonSchema] for LiteRT-LM constrained decoding / function-calling (primary engine),
 * [toGbnf] for the llama.cpp fallback. This is what guarantees 100% structurally-valid actions
 * (SC-003) regardless of engine.
 */
data class OutputSchema(
    val intents: List<String>,
    val actionTypes: List<String>,
    val modules: List<String>,
    val paramsByModule: Map<String, List<String>>,
) {

    /** JSON-schema rendering (LiteRT-LM / any schema-constrained decoder). */
    fun toJsonSchema(): String {
        fun enumArr(values: List<String>) = values.joinToString(", ") { "\"$it\"" }
        return """
            {
              "type": "object",
              "properties": {
                "intent": { "type": "string", "enum": [${enumArr(intents)}] },
                "actionType": { "type": "string", "enum": [${enumArr(actionTypes)}] },
                "moduleName": { "type": "string", "enum": [${enumArr(modules)}] },
                "params": { "type": "object", "additionalProperties": { "type": "string" } },
                "sql": { "type": "string" },
                "reply": { "type": "string" }
              },
              "required": ["intent"]
            }
        """.trimIndent()
    }

    /**
     * GBNF rendering for the llama.cpp fallback. Constrains the JSON to the allow-listed intents /
     * action types / modules. Validated on-device with the llama.cpp adapter (T027); the unit tests
     * assert the enumerations are present.
     */
    fun toGbnf(): String {
        // Build GBNF string-literals via char vars to avoid Kotlin/GBNF double-escaping confusion.
        val dq = "\""
        val bs = "\\"
        fun lit(v: String) = "$dq$bs$dq$v$bs$dq$dq" // matches the JSON string "v"
        fun key(k: String) = "$dq$bs$dq$k$bs$dq$dq" // matches the JSON key "k"
        fun alt(values: List<String>) = values.joinToString(" | ") { lit(it) }
        return buildString {
            appendLine("root ::= \"{\" ws kv (\",\" ws kv)* ws \"}\"")
            appendLine("kv ::= intentkv | actionkv | modulekv | paramskv | sqlkv | replykv")
            appendLine("intentkv ::= ${key("intent")} ws \":\" ws intent")
            appendLine("actionkv ::= ${key("actionType")} ws \":\" ws actionType")
            appendLine("modulekv ::= ${key("moduleName")} ws \":\" ws moduleName")
            appendLine("paramskv ::= ${key("params")} ws \":\" ws object")
            appendLine("sqlkv ::= ${key("sql")} ws \":\" ws str")
            appendLine("replykv ::= ${key("reply")} ws \":\" ws str")
            appendLine("intent ::= ${alt(intents)}")
            appendLine("actionType ::= ${alt(actionTypes)}")
            appendLine("moduleName ::= ${alt(modules)}")
            appendLine("object ::= \"{\" ws (pair (\",\" ws pair)*)? ws \"}\"")
            appendLine("pair ::= str ws \":\" ws str")
            appendLine("str ::= \"\\\"\" ([^\"\\\\])* \"\\\"\"")
            append("ws ::= [ \\t\\n]*")
        }
    }
}
