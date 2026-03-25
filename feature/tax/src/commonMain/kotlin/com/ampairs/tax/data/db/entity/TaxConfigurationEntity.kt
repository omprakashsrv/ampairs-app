package com.ampairs.tax.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.tax.domain.model.TaxCodeType
import com.ampairs.tax.domain.model.TaxStrategy
import com.ampairs.tax.domain.model.TaxConfiguration
import kotlinx.serialization.json.Json

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
    val syncedAt: Long,

    @ColumnInfo(name = "metadata")
    val metadata: String = "{}"                     // JSON
)

// Extension functions
fun TaxConfigurationEntity.toDomain(): TaxConfiguration {
    return TaxConfiguration(
        id = id,
        countryCode = countryCode,
        taxStrategy = TaxStrategy.valueOf(taxStrategy),
        defaultTaxCodeSystem = TaxCodeType.valueOf(defaultTaxCodeSystem),
        taxJurisdictions = Json.decodeFromString(taxJurisdictions),
        industry = industry,
        autoSubscribeNewCodes = autoSubscribeNewCodes,
        syncedAt = syncedAt,
        metadata = try {
            Json.decodeFromString(metadata)
        } catch (e: Exception) {
            emptyMap()
        }
    )
}

fun TaxConfiguration.toEntity(): TaxConfigurationEntity {
    return TaxConfigurationEntity(
        id = id,
        countryCode = countryCode,
        taxStrategy = taxStrategy.name,
        defaultTaxCodeSystem = defaultTaxCodeSystem.name,
        taxJurisdictions = Json.encodeToString(taxJurisdictions),
        industry = industry,
        autoSubscribeNewCodes = autoSubscribeNewCodes,
        syncedAt = syncedAt,
        metadata = Json.encodeToString(metadata)
    )
}
