package com.ampairs.unit.data.db.entity

import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.domain.model.UnitConversion

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the unit domain models.

fun UnitEntity.toUnit(): Unit = Unit(
    uid = id,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    description = description,
    category = category,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Unit.toEntity(): UnitEntity = UnitEntity(
    id = uid,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    description = description,
    category = category,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun UnitConversionEntity.toUnitConversion(): UnitConversion = UnitConversion(
    uid = id,
    productId = productId,
    baseUnitId = baseUnitId,
    derivedUnitId = derivedUnitId,
    multiplier = multiplier,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun UnitConversion.toEntity(): UnitConversionEntity = UnitConversionEntity(
    id = uid,
    productId = productId,
    baseUnitId = baseUnitId,
    derivedUnitId = derivedUnitId,
    multiplier = multiplier,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)
