package com.ampairs.order.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema for the order module (FR-016). Curated read-only columns only —
 * internal sync/audit columns and serialized JSON blobs (tax_info, discount, addresses) are omitted.
 */
@ContributesTo(WorkspaceScope::class)
interface OrderQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("order")
        fun provideOrderQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "order",
            tables = listOf(
                TableSchema(
                    "orderEntity",
                    listOf(
                        ColumnSchema("id", "TEXT", "Order UID"),
                        ColumnSchema("order_number", "TEXT", "Human-readable order number"),
                        ColumnSchema("order_date", "TEXT", "ISO-8601 order timestamp"),
                        ColumnSchema("status", "TEXT", "Order status"),
                        ColumnSchema("customer_id", "TEXT", "Customer UID"),
                        ColumnSchema("customer_name", "TEXT", "Customer name"),
                        ColumnSchema("place_of_supply", "TEXT", "Destination state"),
                        ColumnSchema("total_cost", "REAL", "Order total excluding tax"),
                        ColumnSchema("total_tax", "REAL", "Total tax"),
                        ColumnSchema("total_items", "INTEGER", "Distinct line item count"),
                        ColumnSchema("total_quantity", "REAL", "Total quantity ordered"),
                        ColumnSchema("base_price", "REAL", "Base price before discounts"),
                        ColumnSchema("invoice_ref_id", "TEXT", "Linked invoice UID, if any"),
                    ),
                ),
                TableSchema(
                    "orderItemEntity",
                    listOf(
                        ColumnSchema("id", "TEXT", "Order line UID"),
                        ColumnSchema("order_id", "TEXT", "Parent order UID"),
                        ColumnSchema("product_id", "TEXT", "Product UID"),
                        ColumnSchema("description", "TEXT", "Item description / product name"),
                        ColumnSchema("quantity", "REAL", "Quantity ordered"),
                        ColumnSchema("selling_price", "REAL", "Selling price per unit"),
                        ColumnSchema("mrp", "REAL", "Maximum retail price"),
                        ColumnSchema("total_cost", "REAL", "Line total before tax"),
                        ColumnSchema("total_tax", "REAL", "Line tax amount"),
                        ColumnSchema("tax_code", "TEXT", "Tax code"),
                        ColumnSchema("item_no", "INTEGER", "Line sequence number"),
                    ),
                ),
            ),
        )
    }
}
