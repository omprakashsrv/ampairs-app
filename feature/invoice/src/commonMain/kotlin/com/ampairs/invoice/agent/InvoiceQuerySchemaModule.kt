package com.ampairs.invoice.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema ownership (FR-016, T037): the invoice module contributes its own
 * curated read-only [ModuleQuerySchema] into the workspace-scope schema map via
 * `@Provides @IntoMap @QuerySchemaKey("invoice")`. `SafeQueryService` reads this map directly.
 * Internal/sync/JSON columns are deliberately omitted.
 */
@ContributesTo(WorkspaceScope::class)
interface InvoiceQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("invoice")
        fun provideInvoiceQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "invoice",
            tables = listOf(
                TableSchema(
                    "invoiceEntity",
                    listOf(
                        ColumnSchema("id", "TEXT", "Invoice UID (matches invoiceItemEntity.invoice_id)"),
                        ColumnSchema("invoice_number", "TEXT", "Human-facing invoice number"),
                        ColumnSchema("invoice_date", "TEXT", "ISO-8601 date string; filter by string comparison"),
                        ColumnSchema("status", "TEXT", "One of: DRAFT, NEW, INVOICEED"),
                        ColumnSchema("customer_id", "TEXT", "Buyer customer UID"),
                        ColumnSchema("customer_name", "TEXT", "Buyer name (denormalized snapshot)"),
                        ColumnSchema("customer_gst", "TEXT", "Buyer GSTIN"),
                        ColumnSchema("total_cost", "REAL", "Invoice grand total (tax inclusive)"),
                        ColumnSchema("total_tax", "REAL", "Total tax amount"),
                        ColumnSchema("base_price", "REAL", "Pre-tax total"),
                        ColumnSchema("total_items", "INTEGER", "Number of line items"),
                        ColumnSchema("total_quantity", "REAL", "Sum of line quantities"),
                        ColumnSchema("order_ref_id", "TEXT", "Source order UID, if generated from an order"),
                    ),
                ),
                TableSchema(
                    "invoiceItemEntity",
                    listOf(
                        ColumnSchema("invoice_id", "TEXT", "Parent invoice UID (= invoiceEntity.id)"),
                        ColumnSchema("product_id", "TEXT", "Product UID"),
                        ColumnSchema("description", "TEXT", "Line description"),
                        ColumnSchema("quantity", "REAL", "Quantity sold"),
                        ColumnSchema("selling_price", "REAL", "Unit price charged"),
                        ColumnSchema("total_cost", "REAL", "Line total (tax inclusive)"),
                        ColumnSchema("total_tax", "REAL", "Line tax amount"),
                        ColumnSchema("mrp", "REAL", "Maximum retail price"),
                        ColumnSchema("item_no", "INTEGER", "Line ordinal within the invoice"),
                    ),
                ),
            ),
        )
    }
}
