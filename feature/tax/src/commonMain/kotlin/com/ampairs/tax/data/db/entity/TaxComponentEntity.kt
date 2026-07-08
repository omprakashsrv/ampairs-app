package com.ampairs.tax.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.ampairs.tax.domain.model.JurisdictionLevel
import com.ampairs.tax.domain.model.RateTier
import com.ampairs.tax.domain.model.RateType
import com.ampairs.tax.domain.model.WorkspaceTaxComponent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Tax Component Entity - Component configuration with rates and rules
 * Database isolation per workspace handled by WorkspaceAwareDatabaseFactory
 */
@Entity(
    tableName = "tax_components",
    indices = [
        Index(value = ["jurisdiction"]),
        Index(value = ["component_type_id"]),
        Index(value = ["is_active"])
    ]
)
data class TaxComponentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "component_type_id")
    val componentTypeId: String,

    // Jurisdiction
    @ColumnInfo(name = "jurisdiction")
    val jurisdiction: String,

    @ColumnInfo(name = "jurisdiction_level")
    val jurisdictionLevel: String,

    // Rate configuration
    @ColumnInfo(name = "rate_percentage")
    val ratePercentage: Double,

    @ColumnInfo(name = "rate_type")
    val rateType: String = "FLAT",

    @ColumnInfo(name = "rate_tiers")
    val rateTiers: String? = null,                  // JSON

    // Calculation rules
    @ColumnInfo(name = "calculation_order")
    val calculationOrder: Int = 0,

    @ColumnInfo(name = "is_compound_tax")
    val isCompoundTax: Boolean = false,

    @ColumnInfo(name = "compound_on_components")
    val compoundOnComponents: String? = null,       // JSON

    // Applicability
    @ColumnInfo(name = "applicable_for")
    val applicableFor: String = "[]",               // JSON

    @ColumnInfo(name = "exemptions")
    val exemptions: String = "[]",                  // JSON

    // Effective dates
    @ColumnInfo(name = "effective_from")
    val effectiveFrom: Instant,

    @ColumnInfo(name = "effective_to")
    val effectiveTo: Instant? = null,

    // Accounting
    @ColumnInfo(name = "gl_account_code")
    val glAccountCode: String? = null,

    @ColumnInfo(name = "tax_authority_code")
    val taxAuthorityCode: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)

// Extension functions
fun TaxComponentEntity.toDomain(): WorkspaceTaxComponent {
    return WorkspaceTaxComponent(
        id = id,
        componentTypeId = componentTypeId,
        jurisdiction = jurisdiction,
        jurisdictionLevel = JurisdictionLevel.valueOf(jurisdictionLevel),
        ratePercentage = ratePercentage,
        rateType = RateType.valueOf(rateType),
        rateTiers = rateTiers?.let { Json.decodeFromString<List<RateTier>>(it) },
        calculationOrder = calculationOrder,
        isCompoundTax = isCompoundTax,
        compoundOnComponents = compoundOnComponents?.let { Json.decodeFromString(it) },
        applicableFor = Json.decodeFromString(applicableFor),
        exemptions = Json.decodeFromString(exemptions),
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        glAccountCode = glAccountCode,
        taxAuthorityCode = taxAuthorityCode,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus
    )
}

fun WorkspaceTaxComponent.toEntity(): TaxComponentEntity {
    return TaxComponentEntity(
        id = id,
        componentTypeId = componentTypeId,
        jurisdiction = jurisdiction,
        jurisdictionLevel = jurisdictionLevel.name,
        ratePercentage = ratePercentage,
        rateType = rateType.name,
        rateTiers = rateTiers?.let { Json.encodeToString(it) },
        calculationOrder = calculationOrder,
        isCompoundTax = isCompoundTax,
        compoundOnComponents = compoundOnComponents?.let { Json.encodeToString(it) },
        applicableFor = Json.encodeToString(applicableFor),
        exemptions = Json.encodeToString(exemptions),
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        glAccountCode = glAccountCode,
        taxAuthorityCode = taxAuthorityCode,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus
    )
}
