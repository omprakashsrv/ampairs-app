package com.ampairs.product.api.model

import com.ampairs.product.domain.TaxType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class TaxCodeApiModel(
    @SerialName("id") val id: String,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("code") val code: String,
    @SerialName("type") val type: TaxType,
    @SerialName("description") val description: String,
    @SerialName("effective_from") val effectiveFrom: String?,
    @SerialName("active") val active: Boolean,
    @SerialName("soft_deleted") val softDeleted: Boolean,
    @SerialName("tax_infos") val taxInfos: List<TaxInfoApiModel> = arrayListOf(),
)