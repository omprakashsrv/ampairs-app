package com.ampairs.product.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema ownership (FR-016, T037): the product module contributes its own
 * curated read-only [ModuleQuerySchema]. Internal/sync/audit columns are deliberately omitted.
 */
@ContributesTo(WorkspaceScope::class)
interface ProductQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("product")
        fun provideProductQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "product",
            tables = listOf(
                TableSchema(
                    "productEntity",
                    listOf(
                        ColumnSchema("id", "TEXT", "Product UID"),
                        ColumnSchema("name", "TEXT", "Product name"),
                        ColumnSchema("code", "TEXT", "Product code / SKU"),
                        ColumnSchema("tax_code", "TEXT", "HSN / tax code"),
                        ColumnSchema("mrp", "REAL", "Maximum retail price"),
                        ColumnSchema("dp", "REAL", "Dealer price"),
                        ColumnSchema("selling_price", "REAL", "Selling price"),
                        ColumnSchema("stock_quantity", "REAL", "On-hand stock (nullable)"),
                        ColumnSchema("low_stock_alert", "REAL", "Reorder threshold (nullable)"),
                        ColumnSchema("product_type", "TEXT", "Product type"),
                        ColumnSchema("service_type", "TEXT", "Service type"),
                        ColumnSchema("category_id", "TEXT", "Category UID"),
                        ColumnSchema("brand_id", "TEXT", "Brand UID"),
                        ColumnSchema("created_at", "TEXT", "ISO-8601 creation timestamp"),
                    ),
                ),
            ),
        )
    }
}
