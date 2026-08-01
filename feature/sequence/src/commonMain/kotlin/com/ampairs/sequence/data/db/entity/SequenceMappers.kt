package com.ampairs.sequence.data.db.entity

import com.ampairs.sequence.domain.model.SequenceAllocation
import com.ampairs.sequence.domain.model.SequenceDefinition

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the sequence domain models.

fun SequenceAllocation.toEntity(synced: Boolean = true): SequenceAllocationEntity = SequenceAllocationEntity(
    uid = uid,
    definitionUid = definitionUid,
    entityType = entityType,
    deviceId = deviceId,
    rangeStart = rangeStart,
    rangeEnd = rangeEnd,
    nextAvailable = nextAvailable,
    status = status,
    prefix = prefix,
    suffix = suffix,
    paddingLength = paddingLength,
    incrementStep = incrementStep,
    synced = synced,
    createdAt = createdAt,
    updatedAt = updatedAt,
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
