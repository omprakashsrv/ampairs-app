package com.ampairs.aiops.engine

import com.ampairs.aiops.db.dao.AiOpsDao
import com.ampairs.aiops.db.entity.AiOpsDecisionEntity
import com.ampairs.aiops.db.entity.AiOpsFindingEntity
import com.ampairs.aiops.gate.ConfidenceRiskGate
import com.ampairs.aiops.gate.GateDecision
import com.ampairs.common.aiops.AiOpsCapability
import com.ampairs.common.aiops.AiOpsExecutor
import com.ampairs.common.aiops.AiOpsRunner
import com.ampairs.common.aiops.AiOpsScope
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Confidence
import com.ampairs.common.aiops.Finding
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.workspace.WorkspaceConfig
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Finding.status values persisted to `aiops_finding`. */
internal object AiOpsFindingStatus {
    const val OPEN = "OPEN"
    const val AUTO_FIXED = "AUTO_FIXED"
    const val PENDING_REVIEW = "PENDING_REVIEW"
    const val REJECTED = "REJECTED"
}

/**
 * App-side AI Ops engine runner (ADR 0005). On each entity save the owning feature calls
 * [onEntitySaved]; the runner finds the capabilities for that entity type, runs the fixed pipeline
 * (detect → gather → propose → validate → score), applies the [ConfidenceRiskGate], and either
 * auto-fixes through the capability's [AiOpsExecutor] or records a review finding — always writing an
 * audit row. It must never throw into the caller's save path (all work is best-effort).
 */
@OptIn(ExperimentalTime::class)
@Inject
@SingleIn(WorkspaceScope::class)
@ContributesBinding(WorkspaceScope::class)
class AiOpsRunnerImpl(
    private val capabilities: Map<String, AiOpsCapability>,
    private val executors: Map<String, AiOpsExecutor>,
    private val dao: AiOpsDao,
    private val gate: ConfidenceRiskGate,
    private val preferences: AppPreferencesDataStore,
    private val config: WorkspaceConfig,
) : AiOpsRunner {

    override suspend fun onEntitySaved(entityType: String, entityId: String) {
        val level = preferences.getAiOpsAutonomyLevel().first()
        val scope = AiOpsScope(workspaceId = config.workspaceId)
        capabilities.values
            .filter { it.entityType == entityType }
            .forEach { capability ->
                val findings = runCatching { capability.detect(scope) }
                    .getOrElse { emptyList() }
                    .filter { it.entityId == entityId }
                findings.forEach { finding ->
                    runCatching { process(capability, finding, level) }
                }
            }
    }

    private suspend fun process(capability: AiOpsCapability, finding: Finding, level: com.ampairs.common.aiops.AiOpsAutonomyLevel) {
        val context = capability.gather(finding)
        val candidate = capability.propose(finding, context).firstOrNull() ?: return
        if (!capability.validate(finding, candidate, context).valid) return
        val confidence = capability.score(finding, candidate, context)
        val now = Clock.System.now().toEpochMilliseconds()

        when (gate.decide(confidence, capability.riskLevel, candidate, level)) {
            GateDecision.AUTO_FIX -> {
                val executor = executors[capability.key]
                if (executor != null && executor.apply(candidate, finding).success) {
                    persist(finding, AiOpsFindingStatus.AUTO_FIXED, confidence, now)
                    recordDecision(capability, finding, candidate, confidence, source = "AUTO", now = now)
                } else {
                    persist(finding, AiOpsFindingStatus.PENDING_REVIEW, confidence, now)
                }
            }
            GateDecision.SUGGEST -> persist(finding, AiOpsFindingStatus.PENDING_REVIEW, confidence, now)
            GateDecision.OBSERVE_ONLY -> persist(finding, AiOpsFindingStatus.OPEN, confidence, now)
        }
    }

    private suspend fun persist(finding: Finding, status: String, confidence: Confidence, now: Long) {
        dao.upsertFinding(
            AiOpsFindingEntity(
                id = finding.id.ifBlank { UidGenerator.generateUid(FINDING_PREFIX) },
                capability = finding.capability,
                entityType = finding.entityType,
                entityId = finding.entityId,
                field = finding.field,
                status = status,
                band = confidence.band.name,
                summary = finding.summary,
                signals = encode(finding.signals),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private suspend fun recordDecision(
        capability: AiOpsCapability,
        finding: Finding,
        candidate: Candidate,
        confidence: Confidence,
        source: String,
        now: Long,
    ) {
        dao.insertDecision(
            AiOpsDecisionEntity(
                id = UidGenerator.generateUid(DECISION_PREFIX),
                findingId = finding.id,
                capability = capability.key,
                entityType = finding.entityType,
                entityId = finding.entityId,
                field = candidate.field,
                beforeValue = candidate.before,
                afterValue = candidate.after,
                action = candidate.action.name,
                confidence = confidence.value,
                confidenceContributors = encode(confidence.contributors.mapValues { it.value.toString() }),
                riskLevel = capability.riskLevel.name,
                reason = candidate.rationale,
                source = source,
                reversible = candidate.before != null,
                revertedAt = null,
                createdAt = now,
            ),
        )
    }

    private fun encode(map: Map<String, String>): String? =
        if (map.isEmpty()) null else map.entries.joinToString("&") { "${it.key}=${it.value}" }

    private companion object {
        const val FINDING_PREFIX = "AIO"
        const val DECISION_PREFIX = "AID"
    }
}
