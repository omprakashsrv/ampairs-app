package com.ampairs.aiops.engine

import com.ampairs.aiops.FakeAiOpsDao
import com.ampairs.aiops.RecordingExecutor
import com.ampairs.aiops.db.entity.AiOpsDecisionEntity
import com.ampairs.aiops.db.entity.AiOpsFindingEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Undo re-applies the inverse (`before`) of an audited decision through the same executor, stamps the
 * decision reverted, flips the finding to REJECTED, and records REJECT feedback for the learning loop.
 */
class AiOpsUndoServiceTest {

    private val dao = FakeAiOpsDao()

    private suspend fun seed(
        reversible: Boolean = true,
        revertedAt: Long? = null,
    ) {
        dao.upsertFinding(
            AiOpsFindingEntity(
                id = "F1", capability = "test.cap", entityType = "widget", entityId = "E1",
                field = "f", status = "AUTO_FIXED", createdAt = 1, updatedAt = 1,
            ),
        )
        dao.insertDecision(
            AiOpsDecisionEntity(
                id = "D1", findingId = "F1", capability = "test.cap", entityType = "widget", entityId = "E1",
                field = "f", beforeValue = "old", afterValue = "new", action = "UPDATE_FIELD",
                confidence = 0.99, riskLevel = "LOW", source = "AUTO", reversible = reversible,
                revertedAt = revertedAt, createdAt = 1,
            ),
        )
    }

    @Test
    fun `undo re-applies the inverse and records the rollback`() = runTest {
        seed()
        val executor = RecordingExecutor(capabilityKey = "test.cap")
        val service = AiOpsUndoService(dao, mapOf(executor.capabilityKey to executor))

        val ok = service.undo("D1")

        assertTrue(ok)
        val (candidate, _) = executor.applied.single()
        assertEquals("new", candidate.before, "inverse before is the value that was applied")
        assertEquals("old", candidate.after, "inverse after restores the original")
        assertNotNull(dao.getDecision("D1")?.revertedAt)
        assertEquals("REJECTED", dao.getFinding("F1")?.status)
        assertEquals(listOf("REJECT"), dao.feedback.map { it.verdict })
    }

    @Test
    fun `undo of a missing decision returns false`() = runTest {
        val service = AiOpsUndoService(dao, emptyMap())
        assertFalse(service.undo("nope"))
    }

    @Test
    fun `undo of an already-reverted decision is a no-op`() = runTest {
        seed(revertedAt = 5)
        val executor = RecordingExecutor(capabilityKey = "test.cap")
        val service = AiOpsUndoService(dao, mapOf(executor.capabilityKey to executor))

        assertFalse(service.undo("D1"))
        assertTrue(executor.applied.isEmpty())
    }

    @Test
    fun `undo of a non-reversible decision is refused`() = runTest {
        seed(reversible = false)
        val executor = RecordingExecutor(capabilityKey = "test.cap")
        val service = AiOpsUndoService(dao, mapOf(executor.capabilityKey to executor))

        assertFalse(service.undo("D1"))
        assertTrue(executor.applied.isEmpty())
    }

    @Test
    fun `undo without a matching executor returns false`() = runTest {
        seed()
        val service = AiOpsUndoService(dao, emptyMap())
        assertFalse(service.undo("D1"))
    }
}
