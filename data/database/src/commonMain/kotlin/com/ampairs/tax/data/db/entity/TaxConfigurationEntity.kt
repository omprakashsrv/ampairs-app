package com.ampairs.tax.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlin.time.Instant

/**
 * Tax Configuration Entity - Workspace tax settings
 * Database isolation per workspace handled by WorkspaceAwareDatabaseFactory
 */
@Entity(tableName = "tax_configuration")
data class TaxConfigurationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "country_code")
    val countryCode: String,

    @ColumnInfo(name = "tax_strategy")
    val taxStrategy: String,

    @ColumnInfo(name = "default_tax_code_system")
    val defaultTaxCodeSystem: String,

    @ColumnInfo(name = "tax_jurisdictions")
    val taxJurisdictions: String = "[]",            // JSON

    @ColumnInfo(name = "industry")
    val industry: String? = null,

    @ColumnInfo(name = "auto_subscribe_new_codes")
    val autoSubscribeNewCodes: Boolean = false,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Instant,

    @ColumnInfo(name = "metadata")
    val metadata: String = "{}"                     // JSON
)
