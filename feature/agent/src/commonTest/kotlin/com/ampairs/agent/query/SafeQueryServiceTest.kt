package com.ampairs.agent.query

import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.QueryResultSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SAFE_QUERY pipeline tests (FR-016 / SC-009): the service must validate against the module's curated
 * schema before touching the executor, never reach the executor for a rejected query, and stay
 * read-only. Pure unit test — a fake executor records every SQL it's asked to run.
 */
class SafeQueryServiceTest {

    /** Records executed SQL and returns a canned row set; asserts read-only by never mutating. */
    private class FakeExecutor(override val moduleName: String) : ModuleQueryExecutor {
        val executed = mutableListOf<String>()
        override suspend fun executeReadOnly(sql: String): QueryResultSet {
            executed += sql
            return QueryResultSet(columns = listOf("status", "total_cost"), rows = listOf(listOf("NEW", "100.0")))
        }
    }

    private fun service(executor: FakeExecutor?): Pair<SafeQueryService, FakeExecutor?> {
        val map = executor?.let { mapOf(it.moduleName to (it as ModuleQueryExecutor)) } ?: emptyMap()
        // schemas left empty → falls back to BuiltInQuerySchemas (invoice present) for these tests
        return SafeQueryService(emptyMap(), map) to executor
    }

    @Test
    fun validSelect_executesAndRendersRows() = runTest {
        val (svc, exec) = service(FakeExecutor("invoice"))
        val outcome = svc.run("invoice", "SELECT status, total_cost FROM invoiceEntity WHERE status = 'NEW'")

        assertTrue(outcome is SafeQueryOutcome.Rows, "expected Rows, got $outcome")
        assertEquals(1, exec!!.executed.size)
        assertTrue(exec.executed.single().contains("LIMIT"), "validator must enforce a LIMIT")
        assertTrue(outcome.toText().contains("NEW"))
    }

    @Test
    fun mutatingSql_isRejected_andExecutorNeverCalled() = runTest {
        val (svc, exec) = service(FakeExecutor("invoice"))
        val outcome = svc.run("invoice", "DELETE FROM invoiceEntity")

        assertTrue(outcome is SafeQueryOutcome.Rejected)
        assertTrue(exec!!.executed.isEmpty(), "a rejected query must never reach the executor (read-only proof)")
    }

    @Test
    fun offAllowlistTable_isRejected() = runTest {
        val (svc, exec) = service(FakeExecutor("invoice"))
        // 'customers' lives in a different module/DB — not in the invoice schema allowlist.
        val outcome = svc.run("invoice", "SELECT name FROM customers")

        assertTrue(outcome is SafeQueryOutcome.Rejected)
        assertTrue(exec!!.executed.isEmpty())
    }

    @Test
    fun unknownModule_isUnavailable() = runTest {
        val (svc, _) = service(FakeExecutor("invoice"))
        val outcome = svc.run("ghost", "SELECT 1")
        assertTrue(outcome is SafeQueryOutcome.Unavailable)
    }

    @Test
    fun knownSchemaButNoExecutor_isUnavailable_notRejected() = runTest {
        // Valid module schema exists, but no executor contributed yet (pre-T037).
        val (svc, _) = service(executor = null)
        val outcome = svc.run("invoice", "SELECT status FROM invoiceEntity")
        assertTrue(outcome is SafeQueryOutcome.Unavailable)
    }

    @Test
    fun executorFailure_isReportedAsFailed() = runTest {
        val boom = object : ModuleQueryExecutor {
            override val moduleName = "invoice"
            override suspend fun executeReadOnly(sql: String): QueryResultSet = throw IllegalStateException("disk error")
        }
        val svc = SafeQueryService(emptyMap(), mapOf("invoice" to boom))
        val outcome = svc.run("invoice", "SELECT status FROM invoiceEntity")

        assertTrue(outcome is SafeQueryOutcome.Failed)
        assertTrue(outcome.toText().contains("disk error"))
    }

    @Test
    fun rejectionRateIsTotalForBadInputs() = runTest {
        val (svc, exec) = service(FakeExecutor("invoice"))
        val bad = listOf(
            "DROP TABLE invoiceEntity",
            "SELECT * FROM invoiceEntity; DELETE FROM invoiceEntity",
            "UPDATE invoiceEntity SET status = 'PAID'",
            "SELECT status FROM invoiceEntity -- comment",
            "INSERT INTO invoiceEntity VALUES (1)",
            "SELECT name FROM customers",
        )
        bad.forEach { sql ->
            assertTrue(svc.run("invoice", sql) is SafeQueryOutcome.Rejected, "should reject: $sql")
        }
        assertFalse(exec!!.executed.any(), "no bad input may reach the executor (SC-009)")
    }
}
