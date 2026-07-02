package com.ampairs.customer.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema ownership (FR-016, T037): the customer module contributes its own
 * curated read-only [ModuleQuerySchema]. Internal/sync/audit columns are deliberately omitted.
 */
@ContributesTo(WorkspaceScope::class)
interface CustomerQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("customer")
        fun provideCustomerQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "customer",
            tables = listOf(
                TableSchema(
                    "customers",
                    listOf(
                        ColumnSchema("id", "TEXT", "Customer UID"),
                        ColumnSchema("name", "TEXT", "Customer / business name"),
                        ColumnSchema("email", "TEXT", "Email"),
                        ColumnSchema("phone", "TEXT", "Phone number"),
                        ColumnSchema("customer_type", "TEXT", "Customer type UID/label"),
                        ColumnSchema("customer_group", "TEXT", "Customer group UID/label"),
                        ColumnSchema("gstNumber", "TEXT", "GSTIN"),
                        ColumnSchema("city", "TEXT", "City"),
                        ColumnSchema("state", "TEXT", "State"),
                        ColumnSchema("pincode", "TEXT", "Postal code"),
                        ColumnSchema("country", "TEXT", "Country"),
                        ColumnSchema("created_at", "TEXT", "ISO-8601 creation timestamp"),
                    ),
                ),
            ),
        )
    }
}
