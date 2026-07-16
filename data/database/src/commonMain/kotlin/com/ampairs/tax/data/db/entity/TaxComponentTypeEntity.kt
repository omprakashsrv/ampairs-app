package com.ampairs.tax.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
