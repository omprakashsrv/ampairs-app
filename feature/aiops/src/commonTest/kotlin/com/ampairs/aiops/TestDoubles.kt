package com.ampairs.aiops

import com.ampairs.aiops.db.dao.AiOpsDao
import com.ampairs.aiops.db.entity.AiOpsDecisionEntity
import com.ampairs.aiops.db.entity.AiOpsFeedbackEntity
import com.ampairs.aiops.db.entity.AiOpsFindingEntity
import com.ampairs.common.aiops.AiOpsAutonomyLevel
import com.ampairs.common.aiops.AiOpsCapability
import com.ampairs.common.aiops.AiOpsExecutor
import com.ampairs.common.aiops.AiOpsRiskLevel
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.AiOpsSettings
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Confidence
import com.ampairs.common.aiops.ExecResult
import com.ampairs.common.aiops.Finding
import com.ampairs.common.aiops.FindingContext
import com.ampairs.common.aiops.Validation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** In-memory [AiOpsDao] for engine tests — no Room. */
internal class FakeAiOpsDao : AiOpsDao {
    val findings = MutableStateFlow<Map<String, AiOpsFindingEntity>>(emptyMap())
    val decisions = mutableMapOf<String, AiOpsDecisionEntity>()
    val feedback = mutableListOf<AiOpsFeedbackEntity>()

    override suspend fun upsertFinding(finding: AiOpsFindingEntity) {
        findings.value = findings.value + (finding.id to finding)
    }

    override fun observeFindingsByStatus(status: String): Flow<List<AiOpsFindingEntity>> =
        findings.map { m -> m.values.filter { it.status == status }.sortedByDescending { it.createdAt } }

    override suspend fun getFinding(id: String): AiOpsFindingEntity? = findings.value[id]

    override suspend fun setFindingStatus(id: String, status: String, updatedAt: Long) {
        findings.value[id]?.let {
            findings.value = findings.value + (id to it.copy(status = status, updatedAt = updatedAt))
        }
    }

    override suspend fun insertDecision(decision: AiOpsDecisionEntity) {
        decisions[decision.id] = decision
    }

    override suspend fun getDecision(id: String): AiOpsDecisionEntity? = decisions[id]

    override suspend fun getDecisionsForEntity(entityType: String, entityId: String): List<AiOpsDecisionEntity> =
        decisions.values.filter { it.entityType == entityType && it.entityId == entityId }
            .sortedByDescending { it.createdAt }

    override suspend fun markDecisionReverted(id: String, revertedAt: Long) {
        decisions[id]?.let { decisions[id] = it.copy(revertedAt = revertedAt) }
    }

    override suspend fun insertFeedback(feedback: AiOpsFeedbackEntity) {
        this.feedback += feedback
    }
}

/** Fixed autonomy level. Ignores writes — the runner only reads. */
internal class FixedAiOpsSettings(private val level: AiOpsAutonomyLevel) : AiOpsSettings {
    override fun autonomyLevel(): Flow<AiOpsAutonomyLevel> = flowOf(level)
    override suspend fun setAutonomyLevel(level: AiOpsAutonomyLevel) = Unit
}

/**
 * Configurable capability whose stages are deterministic. [candidate] `null` ⇒ propose emits nothing;
 * [valid] drives the validator.
 */
internal class StubCapability(
    override val key: String = "test.cap",
    override val entityType: String = "widget",
    override val riskLevel: AiOpsRiskLevel = AiOpsRiskLevel.LOW,
    private val findings: List<Finding>,
    private val candidate: Candidate?,
    private val confidence: Confidence,
    private val valid: Boolean = true,
) : AiOpsCapability {
    override suspend fun detect(scope: AiOpsScope): List<Finding> = findings
    override suspend fun gather(finding: Finding): FindingContext = FindingContext()
    override suspend fun propose(finding: Finding, context: FindingContext): List<Candidate> =
        listOfNotNull(candidate)
    override suspend fun validate(finding: Finding, candidate: Candidate, context: FindingContext): Validation =
        Validation(valid)
    override suspend fun score(finding: Finding, candidate: Candidate, context: FindingContext): Confidence =
        confidence
}

/** Executor that records what it applied and returns a fixed outcome. */
internal class RecordingExecutor(
    override val capabilityKey: String = "test.cap",
    private val succeed: Boolean = true,
) : AiOpsExecutor {
    val applied = mutableListOf<Pair<Candidate, Finding>>()
    override suspend fun apply(candidate: Candidate, finding: Finding): ExecResult {
        applied += candidate to finding
        return ExecResult(succeed, if (succeed) "ok" else "boom")
    }
}
