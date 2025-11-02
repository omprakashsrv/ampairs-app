package com.ampairs.tax.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.tax.domain.BusinessType
import com.ampairs.tax.domain.TaxRate
import com.ampairs.tax.domain.TaxType

@Entity(
    tableName = "tax_rates",
    indices = [
        Index(value = ["hsn_code"]),
        Index(value = ["tax_type"]),
        Index(value = ["effective_from"]),
        Index(value = ["effective_to"]),
        Index(value = ["business_type"]),
        Index(value = ["is_active"]),
        Index(value = ["hsn_code", "effective_from", "effective_to", "business_type"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = HsnCodeEntity::class,
            parentColumns = ["hsn_code"],
            childColumns = ["hsn_code"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaxRateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "hsn_code")
    val hsnCode: String,

    @ColumnInfo(name = "tax_type")
    val taxType: String,

    @ColumnInfo(name = "rate_percentage")
    val ratePercentage: Double,

    @ColumnInfo(name = "cess_rate")
    val cessRate: Double? = null,

    @ColumnInfo(name = "cess_amount_per_unit")
    val cessAmountPerUnit: Double? = null,

    @ColumnInfo(name = "effective_from")
    val effectiveFrom: Long,

    @ColumnInfo(name = "effective_to")
    val effectiveTo: Long? = null,

    @ColumnInfo(name = "geographical_zone")
    val geographicalZone: String = "PAN_INDIA",

    @ColumnInfo(name = "business_type")
    val businessType: String,

    @ColumnInfo(name = "version_number")
    val versionNumber: Int = 1,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED",

    @ColumnInfo(name = "last_sync")
    val lastSync: Long = 0
)

fun TaxRateEntity.toDomain(): TaxRate {
    return TaxRate(
        id = id,
        hsnCode = hsnCode,
        taxType = try {
            TaxType.valueOf(taxType.uppercase())
        } catch (e: IllegalArgumentException) {
            TaxType.GST
        },
        ratePercentage = ratePercentage,
        cessRate = cessRate,
        cessAmountPerUnit = cessAmountPerUnit,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        geographicalZone = geographicalZone,
        businessType = try {
            BusinessType.valueOf(businessType.uppercase())
        } catch (e: IllegalArgumentException) {
            BusinessType.REGULAR
        },
        versionNumber = versionNumber,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TaxRate.toEntity(): TaxRateEntity {
    return TaxRateEntity(
        id = id,
        hsnCode = hsnCode,
        taxType = taxType.name,
        ratePercentage = ratePercentage,
        cessRate = cessRate,
        cessAmountPerUnit = cessAmountPerUnit,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        geographicalZone = geographicalZone,
        businessType = businessType.name,
        versionNumber = versionNumber,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}