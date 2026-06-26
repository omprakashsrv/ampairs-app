package com.ampairs.business.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema for the business module (FR-016). Single `business_profile` row;
 * internal sync/audit columns and JSON blobs (operating days, custom attributes) omitted.
 */
@ContributesTo(WorkspaceScope::class)
interface BusinessQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("business")
        fun provideBusinessQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "business",
            tables = listOf(
                TableSchema(
                    "business_profile",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Business UID"),
                        ColumnSchema("name", "TEXT", "Business name"),
                        ColumnSchema("business_type", "TEXT", "Business type"),
                        ColumnSchema("owner_name", "TEXT", "Owner name"),
                        ColumnSchema("city", "TEXT", "City"),
                        ColumnSchema("state", "TEXT", "State"),
                        ColumnSchema("postal_code", "TEXT", "Postal code"),
                        ColumnSchema("country", "TEXT", "Country"),
                        ColumnSchema("phone", "TEXT", "Phone"),
                        ColumnSchema("email", "TEXT", "Email"),
                        ColumnSchema("website", "TEXT", "Website"),
                        ColumnSchema("currency", "TEXT", "Currency code"),
                        ColumnSchema("timezone", "TEXT", "Timezone"),
                    ),
                ),
            ),
        )
    }
}
