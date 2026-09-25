package com.ampairs.aiops.ui.activity

import com.ampairs.aiops.db.entity.AiOpsDecisionEntity

/**
 * A display row for one audited AI Ops decision. Pure view model derived from [AiOpsDecisionEntity] so
 * the mapping (what's undoable, what was changed) is unit-testable without Compose or a dispatcher.
 */
data class AiOpsActivityItem(
    val decisionId: String,
    val capability: String,
    val entityType: String,
    val field: String?,
    val before: String?,
    val after: String?,
    val source: String,
    val createdAt: Long,
    val reversible: Boolean,
    val reverted: Boolean,
) {
    /** Undo is offered only for a reversible decision that hasn't already been rolled back. */
    val canUndo: Boolean get() = reversible && !reverted
}

/** Maps an audit row to its display form. `reverted` is derived from [AiOpsDecisionEntity.revertedAt]. */
fun AiOpsDecisionEntity.toActivityItem(): AiOpsActivityItem = AiOpsActivityItem(
    decisionId = id,
    capability = capability,
    entityType = entityType,
    field = field,
    before = beforeValue,
    after = afterValue,
    source = source,
    createdAt = createdAt,
    reversible = reversible,
    reverted = revertedAt != null,
)
