package com.ampairs.tax.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxCodeType
import kotlin.time.Instant

/**
 * Tax Code Entity - Mobile database storage
 * Subset of master codes actively subscribed (synced from server)
 * Database isolation per workspace handled by WorkspaceAwareDatabaseFactory
 */
@Entity(
    tableName = "tax_codes",
    indices = [
        Index(value = ["is_active"]),
        Index(value = ["usage_count"]),
        Index(value = ["master_tax_code_id"]),
        Index(value = ["code_type"])
    ]
)
data class TaxCodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "master_tax_code_id")
    val masterTaxCodeId: String,

    // Cached master data for offline access
    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "code_type")
    val codeType: String,                           // Stored as String

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "short_description")
    val shortDescription: String,

    // Workspace-specific configuration
    @ColumnInfo(name = "custom_name")
    val customName: String? = null,

    @ColumnInfo(name = "custom_tax_rule_id")
    val customTaxRuleId: String? = null,

    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "added_at")
    val addedAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)

// Extension functions for conversion
fun TaxCodeEntity.toDomain(): TaxCode {
    return TaxCode(
        id = id,
        masterTaxCodeId = masterTaxCodeId,
        code = code,
        codeType = TaxCodeType.valueOf(codeType),
        description = description,
        shortDescription = shortDescription,
        customName = customName,
        customTaxRuleId = customTaxRuleId,
        usageCount = usageCount,
        lastUsedAt = lastUsedAt,
        isFavorite = isFavorite,
        notes = notes,
        isActive = isActive,
        addedAt = Instant.fromEpochMilliseconds(addedAt).toString(),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt).toString(),
        syncStatus = syncStatus
    )
}

fun TaxCode.toEntity(): TaxCodeEntity {
    return TaxCodeEntity(
        id = id,
        masterTaxCodeId = masterTaxCodeId,
        code = code,
        codeType = codeType.name,
        description = description,
        shortDescription = shortDescription,
        customName = customName,
        customTaxRuleId = customTaxRuleId,
        usageCount = usageCount,
        lastUsedAt = lastUsedAt,
        isFavorite = isFavorite,
        notes = notes,
        isActive = isActive,
        addedAt = Instant.parse(addedAt).toEpochMilliseconds(),
        updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
        syncStatus = syncStatus
    )
}
