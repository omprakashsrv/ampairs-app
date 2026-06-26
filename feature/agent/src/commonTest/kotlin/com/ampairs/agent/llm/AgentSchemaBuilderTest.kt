package com.ampairs.agent.llm

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.agent.TableSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the engine-agnostic schema/prompt generation (T028, SC-003 backbone): the output is
 * constrained to exactly the registered action types + modules, in both JSON-schema and GBNF
 * renderings, and the system prompt enumerates the supported actions with required-param markers.
 * Pure unit test — no engine, no DB.
 */
class AgentSchemaBuilderTest {

    private fun desc(module: String, type: ActionType, vararg params: ActionParameter) =
        ActionDescriptor(type, module, "$type on $module", params.toList())

    private val actions = listOf(
        desc("invoice", ActionType.CREATE, ActionParameter("customer", ParameterType.STRING, required = true, "Customer")),
        desc("invoice", ActionType.COUNT),
        desc("customer", ActionType.SEARCH, ActionParameter("query", ParameterType.STRING, required = true, "Query")),
    )

    @Test
    fun build_collectsDistinctSortedTypesAndModules() {
        val schema = AgentSchemaBuilder.build(actions)
        assertEquals(listOf(ActionType.COUNT.name, ActionType.CREATE.name, ActionType.SEARCH.name), schema.actionTypes)
        assertEquals(listOf("customer", "invoice"), schema.modules)
        assertEquals(listOf("customer"), schema.paramsByModule["invoice"])
        assertEquals(listOf("query"), schema.paramsByModule["customer"])
        assertEquals(AgentSchemaBuilder.INTENTS, schema.intents)
    }

    @Test
    fun jsonSchema_enumeratesOnlyRegisteredTypesAndModules() {
        val json = AgentSchemaBuilder.build(actions).toJsonSchema()
        assertTrue(json.contains("\"intent\""))
        assertTrue(json.contains("\"CREATE\"") && json.contains("\"COUNT\"") && json.contains("\"SEARCH\""))
        assertTrue(json.contains("\"invoice\"") && json.contains("\"customer\""))
        assertTrue(json.contains("\"enum\""))
        // a module that wasn't registered must not appear
        assertTrue(!json.contains("\"product\""))
    }

    @Test
    fun gbnf_containsRootAndEnumLiterals() {
        val gbnf = AgentSchemaBuilder.build(actions).toGbnf()
        assertTrue(gbnf.contains("root ::="))
        assertTrue(gbnf.contains("actionType ::="))
        assertTrue(gbnf.contains("moduleName ::="))
        // GBNF string-literals for the JSON values, e.g. "\"CREATE\""
        assertTrue(gbnf.contains("\\\"CREATE\\\""))
        assertTrue(gbnf.contains("\\\"invoice\\\""))
    }

    @Test
    fun systemPrompt_listsModulesActionsAndRequiredMarkers() {
        val prompt = AgentSchemaBuilder.systemPrompt(actions)
        assertTrue(prompt.contains("Module \"invoice\""))
        assertTrue(prompt.contains("Module \"customer\""))
        assertTrue(prompt.contains("CREATE"))
        assertTrue(prompt.contains("customer*"), "required params marked with *")
        assertTrue(prompt.contains("intent=action"))
    }

    private val querySchemas = mapOf(
        "customer" to ModuleQuerySchema(
            "customer",
            listOf(TableSchema("customers", listOf(ColumnSchema("id", "TEXT"), ColumnSchema("name", "TEXT")))),
        ),
    )

    @Test
    fun querySchemas_enableQueryIntentModuleAndSqlField() {
        val schema = AgentSchemaBuilder.build(actions, querySchemas)
        // "query" intent only appears when queryable schemas exist
        assertTrue(schema.intents.contains("query"))
        // queryable module is in the moduleName enum even without a typed action of its own
        assertTrue(schema.modules.contains("customer"))
        // the constrained output now allows an "sql" field
        assertTrue(schema.toJsonSchema().contains("\"sql\""))
    }

    @Test
    fun systemPrompt_includesQueryableSchemaAndExample_whenSchemasGiven() {
        val prompt = AgentSchemaBuilder.systemPrompt(actions, querySchemas)
        assertTrue(prompt.contains("intent=query"))
        assertTrue(prompt.contains("Queryable data"))
        assertTrue(prompt.contains("customers("), "table+columns rendered for the LLM")
        assertTrue(prompt.contains("SELECT COUNT(*)"), "worked example present")
    }

    @Test
    fun noQuerySchemas_keepsQueryIntentOff() {
        val schema = AgentSchemaBuilder.build(actions)
        assertTrue(!schema.intents.contains("query"))
        assertTrue(!AgentSchemaBuilder.systemPrompt(actions).contains("intent=query"))
    }

    @Test
    fun systemPrompt_online_usesAmpairsIdentityNotOffline_andKeepsContract() {
        val prompt = AgentSchemaBuilder.systemPrompt(actions, querySchemas, online = true)
        // Identity fix: the cloud prompt must NOT tell the model it is "offline".
        assertTrue(!prompt.contains("offline", ignoreCase = true), "online prompt must drop the offline framing")
        assertTrue(prompt.contains("Ampairs Assistant"), "online prompt gives a real identity")
        // Same JSON contract + shared sections so the downstream parser is unchanged.
        assertTrue(prompt.contains("\"intent\""))
        assertTrue(prompt.contains("Supported actions:"))
        assertTrue(prompt.contains("Module \"invoice\""))
        assertTrue(prompt.contains("customer*"), "required params still marked with *")
        assertTrue(prompt.contains("Queryable data"))
        assertTrue(prompt.contains("SELECT COUNT(*)"))
    }

    @Test
    fun systemPrompt_offlineDefault_stillUsesOfflineFraming() {
        // The on-device prompt is intentionally unchanged (tiny models are tuned for it).
        assertTrue(AgentSchemaBuilder.systemPrompt(actions).contains("offline assistant"))
    }

    @Test
    fun emptyActions_produceEmptyEnumsWithoutThrowing() {
        val schema = AgentSchemaBuilder.build(emptyList())
        assertTrue(schema.actionTypes.isEmpty())
        assertTrue(schema.modules.isEmpty())
        assertTrue(schema.toJsonSchema().contains("\"intent\""))
        assertTrue(AgentSchemaBuilder.systemPrompt(emptyList()).contains("Supported actions:"))
    }
}
