package com.ampairs.domain

import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.product.api.model.UnitApiModel
import com.ampairs.product.api.model.UnitConversionApiModel
import com.ampairs.product.db.entity.UnitConversionEntity
import com.ampairs.product.db.entity.UnitEntity

data class Unit(
    val id: String, val name: String, val shortName: String, var decimalPlaces: Int,
)

fun List<UnitEntity>.asUnitDomainModel(): List<Unit> {
    return map {
        Unit(
            id = it.id,
            name = it.name,
            shortName = it.short_name,
            decimalPlaces = it.decimal_places.toInt()
        )
    }
}

fun List<ProductApiModel>.asUnitConversionModel(): Set<UnitConversionEntity> {
    val unitConversions = mutableSetOf<UnitConversionEntity>()
    map {
        unitConversions.addAll(it.unitConversions.asDatabaseModel())
    }
    return unitConversions
}

fun List<UnitConversionApiModel>.asDatabaseModel(): List<UnitConversionEntity> {
    return map {
        UnitConversionEntity(
            seq_id = 0,
            id = it.id,
            product_id = it.productId,
            base_unit_id = it.baseUnit.id,
            derived_unit_id = it.derivedUnit.id,
            multiplier = it.multiplier,
            active = if (it.active) 1 else 0,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}

fun List<ProductApiModel>.asUnitModel(): Set<UnitEntity> {
    val units = mutableSetOf<UnitEntity>()
    map {
        it.baseUnit?.asUnitDatabaseModel()?.let { it1 -> units.add(it1) }
    }
    return units
}

fun List<UnitApiModel>.asUnitDatabaseModel(): List<UnitEntity> {
    return map {
        it.asUnitDatabaseModel()
    }
}

fun UnitApiModel.asUnitDatabaseModel(): UnitEntity {
    return UnitEntity(
        seq_id = 0,
        id = this.id,
        name = this.name,
        short_name = this.shortName,
        decimal_places = this.decimalPlaces,
        active = if (this.active) 1 else 0,
        soft_deleted = if (this.softDeleted) 1 else 0,
        synced = 1
    )
}