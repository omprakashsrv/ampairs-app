package com.ampairs.invoice.domain

import com.ampairs.invoice.api.model.TaxInfoApiModel
import com.ampairs.invoice.db.model.TaxInfoEntity

data class TaxInfo(
    var id: String = "",
    var name: String = "",
    var percentage: Double = 0.0,
    var formattedName: String? = "",
    var taxSpec: TaxSpec,
    var value: Double? = 0.0,
)

fun List<TaxInfo>.toApiModel(): List<TaxInfoApiModel> {
    return map {
        TaxInfoApiModel(
            id = it.id,
            name = it.name,
            formattedName = it.formattedName ?: "",
            value = it.value,
            taxSpec = it.taxSpec.name,
            percentage = it.percentage
        )
    }
}

fun List<TaxInfo>.toDatabaseEntity(): List<TaxInfoEntity> {
    return map {
        TaxInfoEntity(
            id = it.id,
            name = it.name,
            formattedName = it.formattedName ?: "",
            value = it.value,
            taxSpec = it.taxSpec,
            percentage = it.percentage
        )
    }
}