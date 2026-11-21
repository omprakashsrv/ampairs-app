package com.ampairs.aiops.engine

import com.ampairs.aiops.FakeAiOpsDao
import com.ampairs.aiops.RecordingExecutor
import com.ampairs.aiops.StubCapability
import com.ampairs.aiops.db.entity.AiOpsFindingEntity
import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Confidence
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the human review actions (accept/dismiss) end-to-end against the in-memory fakes: accept
 * re-derives + applies the fix and records a reversible HUMAN decision; dismiss just marks the finding
 * ignored; both are no-ops unless the finding is still PENDING_REVIEW.
 */
class AiOpsReviewServiceTest {

    private val candidate = Candidate(
        field = "email",
        before = "Jo@X.com",
        after = "jo@x.com",
        action = AiOpsActionType.UPDATE_FIELD,
        rationale = "normalize",
    )
    private val confidence = Confidence(0.999, AiOpsBand.HIGH)

    private fun service(
        dao: FakeAiOpsDao,
        executor: RecordingExecutor = RecordingExecutor(capabilityKey = "customer.email"),
        valid: Boolean = true,
    ): Pair<AiOpsReviewService, RecordingExecutor> {
        val capability = StubCapability(
            key = "customer.email",
            entityType = "customer",
            findings = emptyList(),
            candidate = candidate,
            confidence = confidence,
            valid = valid,
        )
        return AiOpsReviewService(
            capabilities = mapOf("customer.email" to capability),
            executors = mapOf("customer.email" to executor),
            dao = dao,
        ) to executor
    }

    private suspend fun FakeAiOpsDao.seedPending(id: String = "AIO1") {
        upsertFinding(
            AiOpsFindingEntity(
                id = id,
                capability = "customer.email",
                entityType = "customer",
                entityId = "C1",
                field = "email",
                status = "PENDING_REVIEW",
                band = "HIGH",
                summary = "Normalize email",
                signals = "current=Jo@X.com&canonical=jo@x.com",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
    }

    @Test
    fun `accept applies the fix, records a reversible HUMAN decision, and marks the finding accepted`() = runTest {
        val dao = FakeAiOpsDao()
        dao.seedPending()
        val (svc, executor) = service(dao)

        assertTrue(svc.accept("AIO1"))
        assertEquals(1, executor.applied.size)
        assertEquals(1, dao.decisions.size)
        val decision = dao.decisions.values.single()
        assertEquals("HUMAN", decision.source)
        assertTrue(decision.reversible)
        assertEquals("jo@x.com", decision.afterValue)
        assertEquals("ACCEPTED", dao.getFinding("AIO1")?.status)
        assertTrue(dao.feedback.any { it.verdict == "APPROVE" })
    }

    @Test
    fun `accept is a no-op for a finding that is not pending`() = runTest {
        val dao = FakeAiOpsDao()
        dao.seedPending()
        dao.setFindingStatus("AIO1", "AUTO_FIXED", 2L)
        val (svc, executor) = service(dao)

        assertFalse(svc.accept("AIO1"))
        assertTrue(executor.applied.isEmpty())
        assertTrue(dao.decisions.isEmpty())
    }

    @Test
    fun `accept does not apply or record when the candidate fails validation`() = runTest {
        val dao = FakeAiOpsDao()
        dao.seedPending()
        val (svc, executor) = service(dao, valid = false)

        assertFalse(svc.accept("AIO1"))
        assertTrue(executor.applied.isEmpty())
        assertTrue(dao.decisions.isEmpty())
    }

    @Test
    fun `accept returns false for a missing finding`() = runTest {
        val dao = FakeAiOpsDao()
        val (svc, _) = service(dao)
        assertFalse(svc.accept("nope"))
    }

    @Test
    fun `dismiss marks the finding ignored without changing data`() = runTest {
        val dao = FakeAiOpsDao()
        dao.seedPending()
        val (svc, executor) = service(dao)

        assertTrue(svc.dismiss("AIO1"))
        assertEquals("IGNORED", dao.getFinding("AIO1")?.status)
        assertTrue(executor.applied.isEmpty())
        assertTrue(dao.decisions.isEmpty())
        assertTrue(dao.feedback.any { it.verdict == "IGNORE" })
    }

    @Test
    fun `dismiss is a no-op for a finding that is not pending`() = runTest {
        val dao = FakeAiOpsDao()
        dao.seedPending()
        dao.setFindingStatus("AIO1", "ACCEPTED", 2L)
        val (svc, _) = service(dao)
        assertFalse(svc.dismiss("AIO1"))
    }
}
