package com.ampairs.aiops.engine

import com.ampairs.aiops.db.dao.AiOpsDao
import com.ampairs.aiops.db.entity.AiOpsDecisionEntity
import com.ampairs.aiops.db.entity.AiOpsFeedbackEntity
import com.ampairs.common.aiops.AiOpsCapability
import com.ampairs.common.aiops.AiOpsExecutor
import com.ampairs.common.aiops.AiOpsReview
import com.ampairs.common.aiops.Finding
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Applies human decisions on pending AI Ops suggestions (see [AiOpsReview]). Lives beside the runner
 * and shares its capability/executor maps + audit DAO. [accept] re-runs the read-only pipeline for the
 * stored finding — `gather` re-reads the live entity, so the fix reflects the current value — then
 * applies it (user-approved, so the gate is bypassed) and records a reversible HUMAN decision, which
 * surfaces in the activity feed with Undo. [dismiss] just marks the finding ignored.
 */
@OptIn(ExperimentalTime::class)
@Inject
@ContributesBinding(WorkspaceScope::class)
class AiOpsReviewService(
    private val capabilities: Map<String, AiOpsCapability>,
    private val executors: Map<String, AiOpsExecutor>,
    private val dao: AiOpsDao,
) : AiOpsReview {

    override suspend fun accept(findingId: String): Boolean {
        val row = dao.getFinding(findingId) ?: return false
        if (row.status != AiOpsFindingStatus.PENDING_REVIEW) return false
        val capability = capabilities[row.capability] ?: return false

        val finding = Finding(
            id = row.id,
            capability = row.capability,
            entityType = row.entityType,
            entityId = row.entityId,
            field = row.field,
            summary = row.summary,
            signals = decode(row.signals),
        )
        val context = capability.gather(finding)
        val candidate = capability.propose(finding, context).firstOrNull() ?: return false
        if (!capability.validate(finding, candidate, context).valid) return false
        val executor = executors[row.capability] ?: return false
        if (!executor.apply(candidate, finding).success) return false

        val confidence = capability.score(finding, candidate, context)
        val now = Clock.System.now().toEpochMilliseconds()
        dao.insertDecision(
            AiOpsDecisionEntity(
                id = UidGenerator.generateUid(DECISION_PREFIX),
                findingId = row.id,
                capability = row.capability,
                entityType = row.entityType,
                entityId = row.entityId,
                field = candidate.field,
                beforeValue = candidate.before,
                afterValue = candidate.after,
                action = candidate.action.name,
                confidence = confidence.value,
                confidenceContributors = encode(confidence.contributors.mapValues { it.value.toString() }),
                riskLevel = capability.riskLevel.name,
                reason = candidate.rationale,
                source = SOURCE_HUMAN,
                reversible = candidate.before != null,
                revertedAt = null,
                createdAt = now,
            ),
        )
        dao.setFindingStatus(row.id, AiOpsFindingStatus.ACCEPTED, now)
        dao.insertFeedback(
            AiOpsFeedbackEntity(
                id = UidGenerator.generateUid(FEEDBACK_PREFIX),
                findingId = row.id,
                capability = row.capability,
                verdict = "APPROVE",
                createdAt = now,
            ),
        )
        return true
    }

    override suspend fun dismiss(findingId: String): Boolean {
        val row = dao.getFinding(findingId) ?: return false
        if (row.status != AiOpsFindingStatus.PENDING_REVIEW) return false
        val now = Clock.System.now().toEpochMilliseconds()
        dao.setFindingStatus(row.id, AiOpsFindingStatus.IGNORED, now)
        dao.insertFeedback(
            AiOpsFeedbackEntity(
                id = UidGenerator.generateUid(FEEDBACK_PREFIX),
                findingId = row.id,
                capability = row.capability,
                verdict = "IGNORE",
                createdAt = now,
            ),
        )
        return true
    }

    private fun encode(map: Map<String, String>): String? =
        if (map.isEmpty()) null else map.entries.joinToString("&") { "${it.key}=${it.value}" }

    private fun decode(encoded: String?): Map<String, String> =
        encoded?.split("&")
            ?.mapNotNull { pair ->
                val i = pair.indexOf('=')
                if (i <= 0) null else pair.substring(0, i) to pair.substring(i + 1)
            }
            ?.toMap()
            ?: emptyMap()

    private companion object {
        const val DECISION_PREFIX = "AID"
        const val FEEDBACK_PREFIX = "AIF"
        const val SOURCE_HUMAN = "HUMAN"
    }
}
