package com.ampairs.analytics.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the analytics agent-query schema (feature 022, T046 / feedback_agent_models Rule 7): the
 * exposed columns must be the exact Room `@ColumnInfo` names on `DemandForecastEntity` (the validator
 * only checks table names, so a wrong column fails at runtime), and internal columns must be omitted.
 */
class AnalyticsQuerySchemaTest {

    private val schema = AnalyticsQuerySchemaModule.provideAnalyticsQuerySchema()

    @Test
    fun `module name is analytics`() {
        assertEquals("analytics", schema.moduleName)
    }

    @Test
    fun `exposes only the demand_forecast table`() {
        assertEquals(listOf("demand_forecast"), schema.tables.map { it.name })
        assertEquals(setOf("demand_forecast"), schema.allowedTables)
    }

    @Test
    fun `columns are the real entity columns, internal ones omitted`() {
        val columns = schema.tables.single().columns.map { it.name }.toSet()
        val expected = setOf(
            "product_id", "period_start", "horizon", "mean_qty", "std_dev_qty", "method", "confidence",
        )
        assertEquals(expected, columns)
        // uid / generated_at / updated_at are internal plumbing — never exposed to the model.
        assertTrue("uid" !in columns)
        assertTrue("generated_at" !in columns)
        assertTrue("updated_at" !in columns)
    }
}
