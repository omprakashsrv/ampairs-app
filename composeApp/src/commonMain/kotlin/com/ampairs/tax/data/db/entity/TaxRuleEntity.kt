package com.ampairs.tax.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.tax.domain.model.JurisdictionLevel
import com.ampairs.tax.domain.model.TaxCodeType
import com.ampairs.tax.domain.model.TaxRule
import kotlinx.serialization.json.Json

/**
 * Tax Rule Entity - Component composition for complete tax
 */
@Entity(
    tableName = "tax_rules",
    indices = [
        Index(value = ["country_code"]),
        Index(value = ["tax_code", "jurisdiction"]),
        Index(value = ["tax_code_id"]),
        Index(value = ["is_active"])
    ]
)
data class TaxRuleEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "country_code")
    val countryCode: String,

    // Tax code reference
    @ColumnInfo(name = "tax_code_id")
    val taxCodeId: String,

    @ColumnInfo(name = "tax_code")
    val taxCode: String,

    @ColumnInfo(name = "tax_code_type")
    val taxCodeType: String,

    @ColumnInfo(name = "tax_code_description")
    val taxCodeDescription: String,

    // Jurisdiction
    @ColumnInfo(name = "jurisdiction")
    val jurisdiction: String,

    @ColumnInfo(name = "jurisdiction_type")
    val jurisdictionType: String,

    // Component composition
    @ColumnInfo(name = "component_composition")
    val componentComposition: String,               // JSON

    // Transaction context
    @ColumnInfo(name = "applicable_transaction_types")
    val applicableTransactionTypes: String = "[]",  // JSON

    @ColumnInfo(name = "applicable_business_types")
    val applicableBusinessTypes: String = "[]",     // JSON

    // Effective dates
    @ColumnInfo(name = "effective_from")
    val effectiveFrom: Long,

    @ColumnInfo(name = "effective_to")
    val effectiveTo: Long? = null,

    // Version control
    @ColumnInfo(name = "version")
    val version: Int = 1,

    @ColumnInfo(name = "replaces_rule_id")
    val replacesRuleId: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)

// Extension functions
fun TaxRuleEntity.toDomain(): TaxRule {
    return TaxRule(
        id = id,
        countryCode = countryCode,
        workspaceTaxCodeId = taxCodeId,
        taxCode = taxCode,
        taxCodeType = TaxCodeType.valueOf(taxCodeType),
        taxCodeDescription = taxCodeDescription,
        jurisdiction = jurisdiction,
        jurisdictionType = JurisdictionLevel.valueOf(jurisdictionType),
        componentComposition = Json.decodeFromString(componentComposition),
        applicableTransactionTypes = Json.decodeFromString(applicableTransactionTypes),
        applicableBusinessTypes = Json.decodeFromString(applicableBusinessTypes),
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        version = version,
        replacesRuleId = replacesRuleId,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus
    )
}

fun TaxRule.toEntity(): TaxRuleEntity {
    return TaxRuleEntity(
        id = id,
        countryCode = countryCode,
        taxCodeId = workspaceTaxCodeId,
        taxCode = taxCode,
        taxCodeType = taxCodeType.name,
        taxCodeDescription = taxCodeDescription,
        jurisdiction = jurisdiction,
        jurisdictionType = jurisdictionType.name,
        componentComposition = Json.encodeToString(componentComposition),
        applicableTransactionTypes = Json.encodeToString(applicableTransactionTypes),
        applicableBusinessTypes = Json.encodeToString(applicableBusinessTypes),
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        version = version,
        replacesRuleId = replacesRuleId,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus
    )
}
