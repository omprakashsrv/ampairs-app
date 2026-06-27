package com.ampairs.tax.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema for the tax module (FR-016). Curated read-only columns only —
 * internal sync/audit columns and serialized JSON blobs (tiers, exemptions, metadata) omitted.
 */
@ContributesTo(WorkspaceScope::class)
interface TaxQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("tax")
        fun provideTaxQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "tax",
            tables = listOf(
                TableSchema(
                    "tax_codes",
                    listOf(
                        ColumnSchema("id", "TEXT", "Tax code UID"),
                        ColumnSchema("code", "TEXT", "Tax code value"),
                        ColumnSchema("code_type", "TEXT", "Tax code type"),
                        ColumnSchema("description", "TEXT", "Description"),
                        ColumnSchema("short_description", "TEXT", "Short description"),
                        ColumnSchema("custom_name", "TEXT", "User-defined name"),
                        ColumnSchema("usage_count", "INTEGER", "Times used on invoices"),
                        ColumnSchema("is_favorite", "INTEGER", "1 if favorite"),
                    ),
                ),
                TableSchema(
                    "tax_components",
                    listOf(
                        ColumnSchema("id", "TEXT", "Component UID"),
                        ColumnSchema("component_type_id", "TEXT", "Component type UID"),
                        ColumnSchema("jurisdiction", "TEXT", "Jurisdiction"),
                        ColumnSchema("jurisdiction_level", "TEXT", "National/state/city"),
                        ColumnSchema("rate_percentage", "REAL", "Rate percent"),
                        ColumnSchema("rate_type", "TEXT", "Flat/tiered"),
                        ColumnSchema("is_compound_tax", "INTEGER", "1 if compound"),
                        ColumnSchema("effective_from", "TEXT", "Effective from (ISO-8601)"),
                        ColumnSchema("effective_to", "TEXT", "Effective to (ISO-8601)"),
                    ),
                ),
                TableSchema(
                    "tax_component_types",
                    listOf(
                        ColumnSchema("id", "TEXT", "Component type UID"),
                        ColumnSchema("country_code", "TEXT", "ISO country code"),
                        ColumnSchema("component_code", "TEXT", "Component code"),
                        ColumnSchema("component_name", "TEXT", "Component name"),
                        ColumnSchema("display_name", "TEXT", "Display name"),
                        ColumnSchema("component_category", "TEXT", "Category"),
                        ColumnSchema("default_rate", "REAL", "Default rate percent"),
                    ),
                ),
                TableSchema(
                    "tax_rules",
                    listOf(
                        ColumnSchema("id", "TEXT", "Tax rule UID"),
                        ColumnSchema("country_code", "TEXT", "ISO country code"),
                        ColumnSchema("tax_code_id", "TEXT", "Tax code UID"),
                        ColumnSchema("tax_code", "TEXT", "Tax code value"),
                        ColumnSchema("tax_code_type", "TEXT", "Tax code type"),
                        ColumnSchema("jurisdiction", "TEXT", "Jurisdiction"),
                        ColumnSchema("jurisdiction_level", "TEXT", "National/state/city"),
                    ),
                ),
                TableSchema(
                    "tax_configuration",
                    listOf(
                        ColumnSchema("id", "TEXT", "Configuration UID"),
                        ColumnSchema("country_code", "TEXT", "ISO country code"),
                        ColumnSchema("tax_strategy", "TEXT", "Tax strategy"),
                        ColumnSchema("default_tax_code_system", "TEXT", "Default code system"),
                        ColumnSchema("industry", "TEXT", "Industry"),
                    ),
                ),
            ),
        )
    }
}
