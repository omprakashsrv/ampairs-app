package com.ampairs.unit.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema for the unit module (FR-016). Curated read-only columns only —
 * internal sync/audit columns omitted.
 */
@ContributesTo(WorkspaceScope::class)
interface UnitQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("unit")
        fun provideUnitQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "unit",
            tables = listOf(
                TableSchema(
                    "units",
                    listOf(
                        ColumnSchema("id", "TEXT", "Unit UID"),
                        ColumnSchema("name", "TEXT", "Unit display name"),
                        ColumnSchema("short_name", "TEXT", "Abbreviated label"),
                        ColumnSchema("decimal_places", "INTEGER", "Decimal precision"),
                        ColumnSchema("description", "TEXT", "Description"),
                        ColumnSchema("category", "TEXT", "Unit category"),
                        ColumnSchema("created_at", "TEXT", "ISO-8601 created timestamp"),
                    ),
                ),
                TableSchema(
                    "unit_conversions",
                    listOf(
                        ColumnSchema("id", "TEXT", "Conversion UID"),
                        ColumnSchema("product_id", "TEXT", "Product UID"),
                        ColumnSchema("base_unit_id", "TEXT", "Base unit UID"),
                        ColumnSchema("derived_unit_id", "TEXT", "Derived unit UID"),
                        ColumnSchema("multiplier", "REAL", "Base→derived factor"),
                    ),
                ),
            ),
        )
    }
}
