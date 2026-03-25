package com.ampairs.tax.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.tax.domain.model.TaxCalculationMethod
import com.ampairs.tax.domain.model.TaxComponentCategory
import com.ampairs.tax.domain.model.TaxComponentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tax Component Type Entity - Master data for component definitions
 */
@Entity(
    tableName = "tax_component_types",
    indices = [
        Index(value = ["country_code", "component_code"]),
        Index(value = ["is_active"])
    ]
)
data class TaxComponentTypeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "country_code")
    val countryCode: String,

    @ColumnInfo(name = "component_code")
    val componentCode: String,

    @ColumnInfo(name = "component_name")
    val componentName: String,

    @ColumnInfo(name = "short_name")
    val shortName: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "component_category")
    val componentCategory: String,

    @ColumnInfo(name = "calculation_method")
    val calculationMethod: String,

    @ColumnInfo(name = "is_mandatory")
    val isMandatory: Boolean = true,

    @ColumnInfo(name = "default_rate")
    val defaultRate: Double? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "metadata")
    val metadata: String = "{}",                    // JSON string

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)

// Extension functions
fun TaxComponentTypeEntity.toDomain(): TaxComponentType {
    return TaxComponentType(
        id = id,
        countryCode = countryCode,
        componentCode = componentCode,
        componentName = componentName,
        shortName = shortName,
        displayName = displayName,
        componentCategory = TaxComponentCategory.valueOf(componentCategory),
        calculationMethod = TaxCalculationMethod.valueOf(calculationMethod),
        isMandatory = isMandatory,
        defaultRate = defaultRate,
        sortOrder = sortOrder,
        metadata = try {
            Json.decodeFromString(metadata)
        } catch (e: Exception) {
            emptyMap()
        },
        isActive = isActive
    )
}

fun TaxComponentType.toEntity(): TaxComponentTypeEntity {
    return TaxComponentTypeEntity(
        id = id,
        countryCode = countryCode,
        componentCode = componentCode,
        componentName = componentName,
        shortName = shortName,
        displayName = displayName,
        componentCategory = componentCategory.name,
        calculationMethod = calculationMethod.name,
        isMandatory = isMandatory,
        defaultRate = defaultRate,
        sortOrder = sortOrder,
        metadata = Json.encodeToString(metadata),
        isActive = isActive
    )
}
