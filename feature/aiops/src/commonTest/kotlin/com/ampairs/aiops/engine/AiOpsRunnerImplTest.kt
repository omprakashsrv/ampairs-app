package com.ampairs.aiops.engine

import com.ampairs.aiops.FakeAiOpsDao
import com.ampairs.aiops.FixedAiOpsSettings
import com.ampairs.aiops.RecordingExecutor
import com.ampairs.aiops.StubCapability
import com.ampairs.aiops.gate.ConfidenceRiskGate
import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.AiOpsAutonomyLevel
import com.ampairs.common.aiops.AiOpsBand
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Confidence
import com.ampairs.common.aiops.Finding
import com.ampairs.common.workspace.WorkspaceConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the whole app-side engine loop (detect → gather → propose → validate → score → gate →
 * execute/persist/audit) with deterministic doubles, isolating the runner from any real capability.
 */
class AiOpsRunnerImplTest {

    private val dao = FakeAiOpsDao()
    private val config = WorkspaceConfig(workspaceId = "W1", workspaceSlug = "w1")

    private fun finding(id: String = "", entityId: String = "E1", entityType: String = "widget") =
        Finding(id = id, capability = "test.cap", entityType = entityType, entityId = entityId, field = "f")

    private val goodCandidate = Candidate(
        field = "f", before = "old", after = "new", action = AiOpsActionType.UPDATE_FIELD,
    )

    private fun runner(
        level: AiOpsAutonomyLevel,
        findings: List<Finding> = listOf(finding()),
        candidate: Candidate? = goodCandidate,
        confidence: Confidence = Confidence(0.99, AiOpsBand.HIGH),
        valid: Boolean = true,
        executor: RecordingExecutor = RecordingExecutor(),
    ): Pair<AiOpsRunnerImpl, RecordingExecutor> {
        val capability = StubCapability(findings = findings, candidate = candidate, confidence = confidence, valid = valid)
        val runner = AiOpsRunnerImpl(
            capabilities = mapOf(capability.key to capability),
            executors = mapOf(executor.capabilityKey to executor),
            dao = dao,
            gate = ConfidenceRiskGate(),
            settings = FixedAiOpsSettings(level),
            config = config,
        )
        return runner to executor
    }

    @Test
    fun `auto_correct applies the executor, marks finding AUTO_FIXED, and records an audit decision`() = runTest {
        val (runner, executor) = runner(AiOpsAutonomyLevel.AUTO_CORRECT)

        val outcome = runner.onEntitySaved("widget", "E1")

        assertEquals(1, executor.applied.size, "executor should have applied the fix")
        val finding = dao.findings.value.values.single()
        assertEquals("AUTO_FIXED", finding.status)
        val decision = dao.decisions.values.single()
        assertEquals(finding.id, decision.findingId, "decision must link to the persisted finding id")
        assertEquals("AUTO", decision.source)
        assertEquals("new", decision.afterValue)
        assertTrue(decision.reversible)
        // Outcome surfaces the fix (with the decision id for Undo) to the caller/UI.
        val fix = outcome.autoFixed.single()
        assertEquals(decision.id, fix.decisionId)
        assertEquals("new", fix.after)
        assertTrue(outcome.suggestions.isEmpty())
    }

    @Test
    fun `recommend records a review finding and never touches the executor`() = runTest {
        val (runner, executor) = runner(AiOpsAutonomyLevel.RECOMMEND)

        val outcome = runner.onEntitySaved("widget", "E1")

        assertTrue(executor.applied.isEmpty())
        assertEquals("PENDING_REVIEW", dao.findings.value.values.single().status)
        assertTrue(dao.decisions.isEmpty(), "no audit decision without an action")
        assertTrue(outcome.autoFixed.isEmpty())
        assertEquals(1, outcome.suggestions.size, "a review finding is surfaced as a suggestion")
    }

    @Test
    fun `observe records an open finding only`() = runTest {
        val (runner, executor) = runner(AiOpsAutonomyLevel.OBSERVE)

        runner.onEntitySaved("widget", "E1")

        assertTrue(executor.applied.isEmpty())
        assertEquals("OPEN", dao.findings.value.values.single().status)
    }

    @Test
    fun `a failed executor downgrades the finding to review and records no decision`() = runTest {
        val (runner, _) = runner(AiOpsAutonomyLevel.AUTO_CORRECT, executor = RecordingExecutor(succeed = false))

        runner.onEntitySaved("widget", "E1")

        assertEquals("PENDING_REVIEW", dao.findings.value.values.single().status)
        assertTrue(dao.decisions.isEmpty())
    }

    @Test
    fun `findings are filtered to the saved entity id`() = runTest {
        val (runner, executor) = runner(
            AiOpsAutonomyLevel.AUTO_CORRECT,
            findings = listOf(finding(id = "AIO-1", entityId = "E1"), finding(id = "AIO-2", entityId = "E2")),
        )

        runner.onEntitySaved("widget", "E1")

        assertEquals(1, dao.findings.value.size)
        assertEquals("E1", dao.findings.value.values.single().entityId)
        assertEquals(1, executor.applied.size)
    }

    @Test
    fun `a capability for a different entity type is skipped`() = runTest {
        val (runner, executor) = runner(AiOpsAutonomyLevel.AUTO_CORRECT)

        runner.onEntitySaved("something-else", "E1")

        assertTrue(dao.findings.value.isEmpty())
        assertTrue(executor.applied.isEmpty())
    }

    @Test
    fun `an invalid candidate produces no finding and no action`() = runTest {
        val (runner, executor) = runner(AiOpsAutonomyLevel.AUTO_CORRECT, valid = false)

        runner.onEntitySaved("widget", "E1")

        assertTrue(dao.findings.value.isEmpty())
        assertTrue(executor.applied.isEmpty())
    }
}
