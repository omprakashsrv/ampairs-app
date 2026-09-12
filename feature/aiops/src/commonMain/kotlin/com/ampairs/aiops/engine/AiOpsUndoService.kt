package com.ampairs.aiops.engine

import com.ampairs.aiops.db.dao.AiOpsDao
import com.ampairs.aiops.db.entity.AiOpsFeedbackEntity
import com.ampairs.common.aiops.AiOpsActionType
import com.ampairs.common.aiops.AiOpsExecutor
import com.ampairs.common.aiops.Candidate
import com.ampairs.common.aiops.Finding
import com.ampairs.common.id_generator.UidGenerator
import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Undo an auto-applied (or accepted) AI Ops decision: re-apply the inverse (`before`) through the same
 * [AiOpsExecutor], stamp the decision reverted, and record a REJECT [AiOpsFeedbackEntity] for the
 * learning loop. Reversibility is guaranteed by the audit row keeping the prior value.
 */
@OptIn(ExperimentalTime::class)
@Inject
class AiOpsUndoService(
    private val dao: AiOpsDao,
    private val executors: Map<String, AiOpsExecutor>,
) {
    /** @return true if the decision was found, reversible, un-reverted, and successfully rolled back. */
    suspend fun undo(decisionId: String): Boolean {
        val decision = dao.getDecision(decisionId) ?: return false
        if (decision.revertedAt != null || !decision.reversible) return false
        val executor = executors[decision.capability] ?: return false

        val inverse = Candidate(
            field = decision.field,
            before = decision.afterValue,   // swap: applying restores the original value
            after = decision.beforeValue,
            action = runCatching { AiOpsActionType.valueOf(decision.action) }.getOrDefault(AiOpsActionType.UPDATE_FIELD),
            rationale = "Undo of decision ${decision.id}",
        )
        val finding = Finding(
            id = decision.findingId,
            capability = decision.capability,
            entityType = decision.entityType,
            entityId = decision.entityId,
            field = decision.field,
        )
        if (!executor.apply(inverse, finding).success) return false

        val now = Clock.System.now().toEpochMilliseconds()
        dao.markDecisionReverted(decision.id, now)
        dao.setFindingStatus(decision.findingId, AiOpsFindingStatus.REJECTED, now)
        dao.insertFeedback(
            AiOpsFeedbackEntity(
                id = UidGenerator.generateUid("AIF"),
                findingId = decision.findingId,
                capability = decision.capability,
                verdict = "REJECT",
                createdAt = now,
            ),
        )
        return true
    }
}
