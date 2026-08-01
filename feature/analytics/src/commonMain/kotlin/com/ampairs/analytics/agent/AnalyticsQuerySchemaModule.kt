package com.ampairs.analytics.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Curated agent-queryable schema for the analytics module (feature 022, T048): the server-generated
 * `demand_forecast` mirror. Column names are the exact Room `@ColumnInfo` names from
 * `DemandForecastEntity` — the validator only checks table names, so a wrong column would fail at
 * runtime. Internal columns (uid, generated_at, updated_at) are omitted to keep the prompt small.
 */
@ContributesTo(WorkspaceScope::class)
interface AnalyticsQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("analytics")
        fun provideAnalyticsQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "analytics",
            tables = listOf(
                TableSchema(
                    "demand_forecast",
                    listOf(
                        ColumnSchema("product_id", "TEXT", "Product UID the forecast is for"),
                        ColumnSchema("period_start", "TEXT", "ISO date the forecast horizon begins"),
                        ColumnSchema("horizon", "INTEGER", "Forecast horizon length in days"),
                        ColumnSchema("mean_qty", "REAL", "Expected total demand (units) over the whole horizon"),
                        ColumnSchema("std_dev_qty", "REAL", "Std-dev of demand (units) — variability over the horizon"),
                        ColumnSchema("method", "TEXT", "Forecast method: HOLT_WINTERS or MOVING_AVG"),
                        ColumnSchema("confidence", "TEXT", "Confidence: HIGH / MEDIUM / LOW"),
                    ),
                ),
            ),
        )
    }
}
