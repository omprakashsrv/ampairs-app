package com.ampairs.sequence.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.ampairs.sequence.domain.model.SequenceDefinition

/**
 * Local mirror of a server sequence definition. Local admin edits are written with
 * synced = false and pushed in bulk by SequenceSyncDelegate.
 */
@Entity(
    tableName = "sequence_definitions",
    indices = [
        Index(value = ["uid"], unique = true, name = "sequence_definition_uid_idx"),
        Index(value = ["entity_type"], name = "sequence_definition_entity_idx"),
    ]
)
data class SequenceDefinitionEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "scope")
    val scope: String,

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "prefix")
    val prefix: String? = null,

    @ColumnInfo(name = "suffix")
    val suffix: String? = null,

    @ColumnInfo(name = "padding_length")
    val paddingLength: Int = 0,

    @ColumnInfo(name = "start_value")
    val startValue: Long = 1,

    @ColumnInfo(name = "increment_step")
    val incrementStep: Int = 1,

    /** Server high-water mark snapshot — informational on the client (server is authoritative). */
    @ColumnInfo(name = "current_value")
    val currentValue: Long = 0,

    @ColumnInfo(name = "active")
    val active: Boolean = true,

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "ref_id")
    val refId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,
)

fun SequenceDefinitionEntity.toDefinition(): SequenceDefinition = SequenceDefinition(
    uid = uid,
    entityType = entityType,
    scope = scope,
    userId = userId,
    prefix = prefix,
    suffix = suffix,
    paddingLength = paddingLength,
    startValue = startValue,
    incrementStep = incrementStep,
    currentValue = currentValue,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SequenceDefinition.toEntity(): SequenceDefinitionEntity = SequenceDefinitionEntity(
    uid = uid,
    entityType = entityType,
    scope = scope,
    userId = userId,
    prefix = prefix,
    suffix = suffix,
    paddingLength = paddingLength,
    startValue = startValue,
    incrementStep = incrementStep,
    currentValue = currentValue,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
