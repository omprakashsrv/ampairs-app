package com.ampairs.agent.query

import com.ampairs.common.agent.ColumnSchema
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.TableSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafeSqlValidatorTest {

    private val schema = ModuleQuerySchema(
        moduleName = "invoice",
        tables = listOf(
            TableSchema(
                "invoices",
                listOf(ColumnSchema("invoice_number"), ColumnSchema("status"), ColumnSchema("total_cost")),
            ),
            TableSchema("invoice_items", listOf(ColumnSchema("invoice_id"), ColumnSchema("quantity"))),
        ),
    )
    private val validator = SafeSqlValidator(maxRows = 200)

    private fun reason(sql: String): String =
        (validator.validate(sql, schema) as SqlValidationResult.Rejected).reason

    private fun validSql(sql: String): String =
        (validator.validate(sql, schema) as SqlValidationResult.Valid).sql

    @Test
    fun allowsSelectOnAllowlistedTable_andAppendsLimit() {
        val out = validSql("SELECT status, total_cost FROM invoices WHERE status = 'unpaid'")
        assertTrue(out.endsWith("LIMIT 200"), "expected an enforced LIMIT, got: $out")
    }

    @Test
    fun keepsExplicitSmallLimit() {
        val out = validSql("SELECT * FROM invoices LIMIT 10")
        assertTrue(out.contains("LIMIT 10"))
        assertTrue(!out.contains("LIMIT 200"))
    }

    @Test
    fun capsOversizedLimit() {
        val out = validSql("SELECT * FROM invoices LIMIT 999999")
        assertTrue(out.contains("LIMIT 200"), "expected cap, got: $out")
        assertTrue(!out.contains("999999"))
    }

    @Test
    fun allowsJoinBetweenAllowlistedTables() {
        val r = validator.validate(
            "SELECT i.invoice_number FROM invoices i JOIN invoice_items it ON it.invoice_id = i.invoice_number",
            schema,
        )
        assertTrue(r is SqlValidationResult.Valid)
    }

    @Test
    fun allowsCteSelect() {
        val r = validator.validate(
            "WITH paid AS (SELECT * FROM invoices WHERE status = 'paid') SELECT count(*) FROM paid",
            schema,
        )
        assertTrue(r is SqlValidationResult.Valid, "CTE select should pass: $r")
    }

    @Test
    fun rejectsUnknownTable() {
        assertTrue(reason("SELECT * FROM customers").contains("not queryable"))
        assertTrue(reason("SELECT * FROM sqlite_master").contains("not queryable"))
    }

    @Test
    fun rejectsMutatingStatements() {
        assertTrue(reason("INSERT INTO invoices(status) VALUES ('x')").isNotEmpty())
        assertTrue(reason("UPDATE invoices SET status = 'paid'").isNotEmpty())
        assertTrue(reason("DELETE FROM invoices").isNotEmpty())
        assertTrue(reason("DROP TABLE invoices").isNotEmpty())
    }

    @Test
    fun rejectsDangerousKeywords() {
        assertTrue(reason("SELECT * FROM invoices; PRAGMA table_info(invoices)").isNotEmpty())
        assertTrue(reason("ATTACH DATABASE 'x' AS y").isNotEmpty())
    }

    @Test
    fun rejectsMultipleStatements() {
        assertEquals("Only a single statement is allowed.", reason("SELECT 1 FROM invoices; SELECT 2 FROM invoices"))
    }

    @Test
    fun rejectsComments() {
        assertEquals("Comments are not allowed.", reason("SELECT * FROM invoices -- sneaky"))
        assertEquals("Comments are not allowed.", reason("SELECT * FROM invoices /* block */"))
    }

    @Test
    fun rejectsNonSelect() {
        assertTrue(reason("EXPLAIN SELECT * FROM invoices").contains("Only SELECT"))
        assertTrue(reason("").contains("Empty"))
    }

    @Test
    fun trailingSemicolonIsTolerated() {
        val r = validator.validate("SELECT status FROM invoices;", schema)
        assertTrue(r is SqlValidationResult.Valid)
    }
}
