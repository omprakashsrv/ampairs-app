package com.ampairs.product.domain

import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.product.api.model.TaxCodeApiModel
import com.ampairs.product.db.entity.TaxCodeEntity
import com.ampairs.product.db.entity.TaxInfoEntity
import com.ampairs.product.db.entity.toDomainModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime


//val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

data class TaxCode(
    var id: String = "",
    var code: String = "",
    var type: TaxType = TaxType.HSN,
    var description: String = "",
    var effectiveFrom: LocalDate? = null,
    var taxInfos: List<TaxInfo> = arrayListOf(),
    var active: Boolean = true,
    var softDeleted: Boolean = false,
)

fun List<ProductApiModel>.asTaxCodeModel(): Set<TaxCodeEntity> {
    val taxCodes = mutableSetOf<TaxCodeEntity>()
    map {
        taxCodes.addAll(it.taxCodes.asDatabaseModel())
    }
    return taxCodes
}

fun List<TaxCodeApiModel>.asDatabaseModel(): List<TaxCodeEntity> {
    return map {
        TaxCodeEntity(
            seq_id = 0,
            id = it.id,
            code = it.code,
            type = it.type.name,
            description = it.description,
            effective_from = it.effectiveFrom,
            tax_info = Json.encodeToString(it.taxInfos),
            active = if (it.active) 1 else 0,
            soft_deleted = if (it.softDeleted) 1 else 0,
            synced = 1
        )
    }
}

fun TaxCode.asDatabaseModel(): TaxCodeEntity {
    return TaxCodeEntity(
        seq_id = 0,
        id = this.id,
        code = this.code,
        type = this.type.name,
        description = this.description,
        effective_from = DateTimeAdapter.toDateString(this.effectiveFrom),
        tax_info = Json.encodeToString(this.taxInfos.asApiModel()),
        active = if (this.active) 1 else 0,
        soft_deleted = if (this.softDeleted) 1 else 0,
        synced = 0
    )
}

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalTime::class)
fun TaxCodeEntity.asDomainModel(): TaxCode {
    val taxInfos = json.decodeFromString<List<TaxInfoEntity>>(this.tax_info)
    return TaxCode(
        id = this.id,
        code = this.code,
        type = TaxType.valueOf(this.type),
        description = this.description,
        effectiveFrom = this.effective_from?.let {
            DateTimeAdapter.fromDateString(it)
                ?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
        },
        taxInfos = taxInfos.toDomainModel(),
        active = this.active == 1,
        softDeleted = this.soft_deleted == 1
    )
}

fun List<TaxCodeEntity>.asDomainModel(): List<TaxCode> {
    return map { it.asDomainModel() }
}


fun List<TaxCodeEntity>.asTaxCodeApiModel(): List<TaxCodeApiModel> {
    return map {
        TaxCodeApiModel(
            id = it.id,
            code = it.code,
            type = TaxType.valueOf(it.type),
            description = it.description,
            effectiveFrom = it.effective_from,
            taxInfos = Json.decodeFromString(it.tax_info),
            active = it.active == 1,
            softDeleted = it.soft_deleted == 1
        )
    }
}