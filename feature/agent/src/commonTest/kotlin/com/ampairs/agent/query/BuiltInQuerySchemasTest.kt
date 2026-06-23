package com.ampairs.agent.query

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuiltInQuerySchemasTest {

    private val validator = SafeSqlValidator()

    @Test
    fun allModulesPresentAndNonEmpty() {
        assertEquals(setOf("invoice", "customer", "product", "inventory"), BuiltInQuerySchemas.byModule.keys)
        BuiltInQuerySchemas.byModule.values.forEach { schema ->
            assertTrue(schema.tables.isNotEmpty(), "${schema.moduleName} has no tables")
            assertTrue(schema.allowedTables.isNotEmpty())
            schema.tables.forEach { assertTrue(it.columns.isNotEmpty(), "${it.name} has no columns") }
        }
    }

    @Test
    fun curatedSchemasHideInternalColumns() {
        // No plumbing/JSON-blob columns should be exposed to the agent.
        val banned = setOf(
            "synced", "soft_deleted", "seq_id", "last_updated", "last_sync",
            "tax_info", "discount", "attributes_json", "custom_fields",
            "billing_address_json", "shipping_address_json",
        )
        BuiltInQuerySchemas.byModule.values.forEach { schema ->
            schema.tables.forEach { table ->
                table.columns.forEach { col ->
                    assertTrue(col.name !in banned, "${schema.moduleName}.${table.name}.${col.name} should be hidden")
                }
            }
        }
    }

    @Test
    fun representativeQueryPerModuleValidatesAgainstItsSchema() {
        val queries = mapOf(
            "invoice" to "SELECT status, total_cost FROM invoiceEntity WHERE status = 'NEW'",
            "customer" to "SELECT name, city FROM customers WHERE state = 'KA'",
            "product" to "SELECT name, stock_quantity FROM productEntity WHERE stock_quantity < low_stock_alert",
            "inventory" to "SELECT productId, currentStock FROM inventory_items WHERE availableStock <= reorderLevel",
        )
        queries.forEach { (module, sql) ->
            val schema = BuiltInQuerySchemas.forModule(module)!!
            val result = validator.validate(sql, schema)
            assertTrue(result is SqlValidationResult.Valid, "$module query should validate, got: $result")
        }
    }

    @Test
    fun invoiceItemJoinValidates() {
        val sql = "SELECT i.invoice_number, it.quantity FROM invoiceEntity i " +
            "JOIN invoiceItemEntity it ON it.invoice_id = i.id"
        assertTrue(validator.validate(sql, BuiltInQuerySchemas.INVOICE) is SqlValidationResult.Valid)
    }

    @Test
    fun crossModuleTableIsRejected() {
        // 'customers' lives in a different DB; querying it from the invoice schema must be refused.
        val r = validator.validate("SELECT name FROM customers", BuiltInQuerySchemas.INVOICE)
        assertTrue(r is SqlValidationResult.Rejected)
    }

    @Test
    fun promptTextListsTablesAndColumns() {
        val prompt = BuiltInQuerySchemas.INVOICE.toPromptText()
        assertTrue(prompt.contains("invoiceEntity"))
        assertTrue(prompt.contains("status"))
        assertTrue(prompt.contains("module 'invoice'"))
    }
}
