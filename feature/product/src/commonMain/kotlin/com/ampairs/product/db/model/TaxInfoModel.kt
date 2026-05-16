package com.ampairs.product.db.entity

import com.ampairs.product.domain.TaxInfo
import com.ampairs.product.domain.TaxSpec

fun List<TaxInfoEntity>.toDomainModel(): List<TaxInfo> {
    return map {
        TaxInfo(
            id = it.id,
            name = it.name,
            percentage = it.percentage,
            formattedName = it.formatted_name,
            taxSpec = TaxSpec.valueOf(it.tax_spec),
        )
    }
}