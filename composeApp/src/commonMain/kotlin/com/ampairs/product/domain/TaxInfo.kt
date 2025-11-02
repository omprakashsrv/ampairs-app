package com.ampairs.product.domain

import com.ampairs.product.api.model.TaxInfoApiModel
import com.ampairs.product.db.entity.TaxInfoEntity

data class TaxInfo(
    var id: String = "",
    var name: String = "",
    var percentage: Double = 0.0,
    var formattedName: String = "",
    var taxSpec: TaxSpec = TaxSpec.INTRA,
    var active: Boolean = true,
    var softDeleted: Boolean = false,
)

fun List<TaxInfoApiModel>.asDatabaseModel(): List<TaxInfoEntity> {
    return map {
        TaxInfoEntity(
            seq_id = 0,
            id = it.id,
            name = it.name,
            formatted_name = it.formattedName,
            tax_spec = it.taxSpec.name,
            active = if (it.active != false) 1 else 0,
            soft_deleted = if (it.softDeleted == true) 1 else 0,
            percentage = it.percentage,
            synced = 1
        )
    }
}

fun TaxInfo.asDatabaseModel(): TaxInfoEntity {
    return TaxInfoEntity(
        seq_id = 0,
        id = this.id,
        name = this.name,
        formatted_name = this.formattedName,
        tax_spec = this.taxSpec.name,
        active = if (this.softDeleted) 1 else 0,
        soft_deleted = if (this.softDeleted) 1 else 0,
        percentage = this.percentage,
        synced = 0
    )
}

fun List<TaxInfoApiModel>.asDomainModel(): List<TaxInfo> {
    return map {
        TaxInfo(
            id = it.id,
            name = it.name,
            formattedName = it.formattedName,
            taxSpec = TaxSpec.valueOf(it.taxSpec.name),
            percentage = it.percentage,
        )
    }
}

fun List<TaxInfoEntity>.asTaxInfoApiModel(): List<TaxInfoApiModel> {
    return map {
        TaxInfoApiModel(
            id = it.id,
            name = it.name,
            formattedName = it.formatted_name,
            taxSpec = TaxSpec.valueOf(it.tax_spec),
            percentage = it.percentage,
            active = it.active == 1,
            softDeleted = it.soft_deleted == 1,
        )
    }
}


fun List<TaxInfo>.asApiModel(): List<TaxInfoApiModel> {
    return map {
        TaxInfoApiModel(
            id = it.id,
            name = it.name,
            formattedName = it.formattedName,
            taxSpec = it.taxSpec,
            percentage = it.percentage,
            active = it.active,
            softDeleted = it.softDeleted,
        )
    }
}

fun List<TaxInfoEntity>.asTaxInfoDomainModel(): List<TaxInfo> {
    return map {
        it.asTaxInfoDomainModel()
    }
}

fun TaxInfoEntity.asTaxInfoDomainModel(): TaxInfo {
    return TaxInfo(
        id = this.id,
        name = this.name,
        formattedName = this.formatted_name,
        taxSpec = TaxSpec.valueOf(this.tax_spec),
        percentage = this.percentage,
        active = this.active == 1,
        softDeleted = this.soft_deleted == 1
    )
}