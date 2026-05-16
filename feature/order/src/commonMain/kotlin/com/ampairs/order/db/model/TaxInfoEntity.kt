package com.ampairs.order.db.model

import com.ampairs.order.domain.TaxInfo
import com.ampairs.order.domain.TaxSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaxInfoEntity(
    @SerialName("id") var id: String = "",
    @SerialName("name") var name: String = "",
    @SerialName("percentage") var percentage: Double = 0.0,
    @SerialName("formatted_name") var formattedName: String? = "",
    @SerialName("tax_spec") var taxSpec: TaxSpec,
    @SerialName("value") var value: Double? = 0.0,
)

fun List<TaxInfoEntity>.toDomainModel(): List<TaxInfo> {
    return map {
        TaxInfo(
            id = it.id,
            name = it.name,
            percentage = it.percentage,
            formattedName = it.formattedName,
            taxSpec = it.taxSpec,
            value = it.value
        )
    }
}