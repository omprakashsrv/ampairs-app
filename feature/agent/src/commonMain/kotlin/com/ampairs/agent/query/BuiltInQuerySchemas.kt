package com.ampairs.agent.query

/**
 * Curated, read-only [ModuleQuerySchema]s for the SAFE_QUERY fallback — the allow-listed tables/columns
 * the agent may query per module, with business descriptions to help the LLM write correct SQL.
 *
 * Derived (by hand) from each module's Room `@Entity` (the source of truth) + the exported schema JSON
 * under `shared/schemas/…`. Internal/plumbing columns are deliberately **omitted** — `seq_id`, `synced`,
 * `soft_deleted`, `active`, `last_updated`/`last_sync`, audit (`created_by`/`updated_by`), `ref_id`, and
 * JSON-blob columns (`*_json`, `tax_info`, `discount`, `custom_fields`) — so they're unreachable and the
 * prompt stays small.
 *
 * NOTE (ownership): these live in `feature/agent` as the initial catalog. When the per-module
 * `SqlQueryDelegate` lands (tasks T037, contracts in `data/common`), each module should own and
 * contribute its own schema so it can't drift from the entities. Until then, keep these in sync if a
 * module renames a column.
 */
object BuiltInQuerySchemas {

    val INVOICE = ModuleQuerySchema(
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

    val CUSTOMER = ModuleQuerySchema(
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

    val PRODUCT = ModuleQuerySchema(
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

    val INVENTORY = ModuleQuerySchema(
        moduleName = "inventory",
        tables = listOf(
            TableSchema(
                "inventoryEntity",
                listOf(
                    ColumnSchema("id", "TEXT", "Inventory row UID"),
                    ColumnSchema("product_id", "TEXT", "Product UID"),
                    ColumnSchema("unit_id", "TEXT", "Unit UID"),
                    ColumnSchema("description", "TEXT", "Description"),
                    ColumnSchema("mrp", "REAL", "Maximum retail price"),
                    ColumnSchema("buying_price", "REAL", "Buying price"),
                    ColumnSchema("dp", "REAL", "Dealer price"),
                    ColumnSchema("selling_price", "REAL", "Selling price"),
                    ColumnSchema("stock", "REAL", "On-hand stock quantity"),
                    ColumnSchema("created_at", "TEXT", "ISO-8601 creation timestamp"),
                ),
            ),
        ),
    )

    /** All built-in curated schemas, keyed by module name (matches `AgentAction.moduleName`). */
    val byModule: Map<String, ModuleQuerySchema> =
        listOf(INVOICE, CUSTOMER, PRODUCT, INVENTORY).associateBy { it.moduleName }

    fun forModule(moduleName: String): ModuleQuerySchema? = byModule[moduleName]
}
