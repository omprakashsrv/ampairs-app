package com.ampairs.aiops.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * The audit trail: one row per action the AI Ops engine took or proposed. `before`/`after` make the
 * action reversible (rollback re-applies `before` through the same executor). Local-only this slice.
 */
@Entity(
    tableName = "aiops_decision",
    indices = [
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["finding_id"]),
    ],
)
data class AiOpsDecisionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "finding_id")
    val findingId: String,

    @ColumnInfo(name = "capability")
    val capability: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "field")
    val field: String? = null,

    @ColumnInfo(name = "before_value")
    val beforeValue: String? = null,

    @ColumnInfo(name = "after_value")
    val afterValue: String? = null,

    /** AiOpsActionType name (UPDATE_FIELD, LINK, …). */
    @ColumnInfo(name = "action")
    val action: String,

    @ColumnInfo(name = "confidence")
    val confidence: Double,

    /** Per-source confidence contributors, JSON-encoded (kept for the audit). */
    @ColumnInfo(name = "confidence_contributors")
    val confidenceContributors: String? = null,

    /** LOW | MEDIUM | HIGH */
    @ColumnInfo(name = "risk_level")
    val riskLevel: String,

    @ColumnInfo(name = "reason")
    val reason: String = "",

    /** AUTO | HUMAN */
    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "reversible")
    val reversible: Boolean,

    /** Epoch millis when the decision was reverted; null while still in effect. */
    @ColumnInfo(name = "reverted_at")
    val revertedAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
