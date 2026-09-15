package com.ampairs.aiops.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A problem the AI Ops engine detected for one entity (see `com.ampairs.common.aiops`).
 * Local-only this slice — audit sync to the server is a later (Epic-2) concern.
 *
 * Lives in the consolidated `AmpairsWorkspaceDatabase` (features no longer own DBs); the runner in
 * `feature/aiops` injects [com.ampairs.aiops.db.dao.AiOpsDao] via `WorkspaceDatabaseDaoModule`.
 */
@Entity(
    tableName = "aiops_finding",
    indices = [
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["capability", "status"]),
    ],
)
data class AiOpsFindingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "capability")
    val capability: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "field")
    val field: String? = null,

    /** OPEN | AUTO_FIXED | PENDING_REVIEW | ACCEPTED | REJECTED | IGNORED */
    @ColumnInfo(name = "status")
    val status: String,

    /** HIGH | MEDIUM | LOW (nullable until scored). */
    @ColumnInfo(name = "band")
    val band: String? = null,

    @ColumnInfo(name = "summary")
    val summary: String = "",

    /** Raw evidence the detector saw, JSON-encoded. */
    @ColumnInfo(name = "signals")
    val signals: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
