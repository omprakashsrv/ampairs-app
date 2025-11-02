package com.ampairs.product.api.model

import com.ampairs.product.domain.TaxSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaxInfoApiModel(
    @SerialName("id")
    var id: String = "",
    @SerialName("ref_id")
    var refId: String? = null,
    @SerialName("name")
    var name: String = "",
    @SerialName("percentage")
    var percentage: Double = 0.0,
    @SerialName("formatted_name")
    var formattedName: String = "",
    @SerialName("tax_spec")
    var taxSpec: TaxSpec,
    @SerialName("active")
    var active: Boolean? = null,
    @SerialName("soft_deleted")
    var softDeleted: Boolean? = null,
)