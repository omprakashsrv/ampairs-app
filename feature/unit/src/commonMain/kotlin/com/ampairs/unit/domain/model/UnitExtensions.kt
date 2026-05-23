package com.ampairs.unit.domain.model

fun Unit.toListItem(): UnitListItem = UnitListItem(
    id = uid,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    description = description,
    category = category,
    active = active
)
