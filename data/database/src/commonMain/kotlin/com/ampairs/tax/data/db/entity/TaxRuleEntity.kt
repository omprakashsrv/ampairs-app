package com.ampairs.tax.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlin.time.Instant

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
    val taxCodeDescription: String? = null,

    // Jurisdiction
    @ColumnInfo(name = "jurisdiction")
    val jurisdiction: String,

    @ColumnInfo(name = "jurisdiction_level")
    val jurisdictionLevel: String,

    // Component composition - JSON map
    @ColumnInfo(name = "component_composition")
    val componentComposition: String,               // JSON Map<String, ComponentComposition>

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.fromEpochMilliseconds(0),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.fromEpochMilliseconds(0),

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)
