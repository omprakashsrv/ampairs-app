package com.ampairs.aiops.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A human verdict on a finding/decision, used by the learning loop (threshold tuning, few-shot).
 * Local-only this slice.
 */
@Entity(
    tableName = "aiops_feedback",
    indices = [
        Index(value = ["finding_id"]),
    ],
)
data class AiOpsFeedbackEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "finding_id")
    val findingId: String,

    @ColumnInfo(name = "capability")
    val capability: String,

    /** APPROVE | REJECT | EDIT | MERGE | KEEP_BOTH | IGNORE */
    @ColumnInfo(name = "verdict")
    val verdict: String,

    @ColumnInfo(name = "edited_value")
    val editedValue: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
