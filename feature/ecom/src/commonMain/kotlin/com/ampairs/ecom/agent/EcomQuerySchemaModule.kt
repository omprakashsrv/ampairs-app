package com.ampairs.ecom.agent

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QuerySchemaKey
import com.ampairs.common.agent.TableSchema
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides

/**
 * Per-module SAFE_QUERY schema for the ecom (storefront) module (FR-016). Curated read-only columns
 * only — sync/audit columns and JSON blobs (delivery_address, image_urls) omitted; transient cart
 * tables are intentionally excluded.
 */
@ContributesTo(WorkspaceScope::class)
interface EcomQuerySchemaModule {
    companion object {
        @Provides
        @IntoMap
        @QuerySchemaKey("ecom")
        fun provideEcomQuerySchema(): ModuleQuerySchema = ModuleQuerySchema(
            moduleName = "ecom",
            tables = listOf(
                TableSchema(
                    "ecom_order",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Order UID"),
                        ColumnSchema("ecom_order_ref", "TEXT", "Server order reference"),
                        ColumnSchema("storefront_id", "TEXT", "Storefront UID"),
                        ColumnSchema("status", "TEXT", "Pending/confirmed/shipped/delivered"),
                        ColumnSchema("subtotal", "REAL", "Subtotal before charges"),
                        ColumnSchema("total_amount", "REAL", "Order total"),
                        ColumnSchema("placed_at", "TEXT", "Placed at (ISO-8601)"),
                        ColumnSchema("confirmed_at", "TEXT", "Confirmed at (ISO-8601)"),
                    ),
                ),
                TableSchema(
                    "ecom_order_line_item",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Line item UID"),
                        ColumnSchema("order_uid", "TEXT", "Parent order UID"),
                        ColumnSchema("listed_product_id", "TEXT", "Listed product UID"),
                        ColumnSchema("product_name", "TEXT", "Product name"),
                        ColumnSchema("unit_price", "REAL", "Unit price"),
                        ColumnSchema("quantity_ordered", "INTEGER", "Quantity ordered"),
                        ColumnSchema("quantity_confirmed", "INTEGER", "Quantity confirmed"),
                        ColumnSchema("line_total", "REAL", "Line total"),
                        ColumnSchema("status", "TEXT", "Pending/confirmed/cancelled"),
                    ),
                ),
                TableSchema(
                    "listed_product",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Listed product UID"),
                        ColumnSchema("storefront_id", "TEXT", "Storefront UID"),
                        ColumnSchema("name", "TEXT", "Product name"),
                        ColumnSchema("brand", "TEXT", "Brand"),
                        ColumnSchema("category", "TEXT", "Category"),
                        ColumnSchema("subcategory", "TEXT", "Subcategory"),
                        ColumnSchema("price", "REAL", "Selling price"),
                        ColumnSchema("mrp", "REAL", "Maximum retail price"),
                        ColumnSchema("stock_status", "TEXT", "In/out/limited stock"),
                        ColumnSchema("stock_quantity", "INTEGER", "Quantity available"),
                        ColumnSchema("is_visible", "INTEGER", "1 if visible in catalog"),
                    ),
                ),
                TableSchema(
                    "storefront",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Storefront UID"),
                        ColumnSchema("slug", "TEXT", "URL slug"),
                        ColumnSchema("name", "TEXT", "Storefront name"),
                        ColumnSchema("status", "TEXT", "Active/inactive/archived"),
                        ColumnSchema("access_mode", "TEXT", "Public/private"),
                    ),
                ),
                TableSchema(
                    "customer_address",
                    listOf(
                        ColumnSchema("uid", "TEXT", "Address UID"),
                        ColumnSchema("label", "TEXT", "Label (home/office)"),
                        ColumnSchema("city", "TEXT", "City"),
                        ColumnSchema("state", "TEXT", "State"),
                        ColumnSchema("pin_code", "TEXT", "Postal code"),
                        ColumnSchema("country", "TEXT", "Country"),
                        ColumnSchema("is_default", "INTEGER", "1 if default address"),
                    ),
                ),
            ),
        )
    }
}
