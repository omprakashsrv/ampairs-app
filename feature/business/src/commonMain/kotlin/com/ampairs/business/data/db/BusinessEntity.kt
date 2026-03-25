package com.ampairs.business.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.business.domain.Business
import com.ampairs.business.domain.BusinessType
import com.ampairs.business.util.BusinessConstants
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(
    tableName = "business_profile",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["workspace_id"], unique = false),
        Index(value = ["active"]),
        Index(value = ["synced"])  // Performance optimization for sync queries
    ]
)
data class BusinessEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,
    @ColumnInfo(name = "seq_id")
    val seqId: String?,
    @ColumnInfo(name = "workspace_id")
    val workspaceId: String?,
    val name: String,
    @ColumnInfo(name = "business_type")
    val businessType: String,
    val description: String?,
    @ColumnInfo(name = "owner_name")
    val ownerName: String?,
    @ColumnInfo(name = "address_line1")
    val addressLine1: String?,
    @ColumnInfo(name = "address_line2")
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    @ColumnInfo(name = "postal_code")
    val postalCode: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
    val phone: String?,
    val email: String?,
    val website: String?,
    @ColumnInfo(name = "tax_id")
    val taxId: String?,
    @ColumnInfo(name = "registration_number")
    val registrationNumber: String?,
    @ColumnInfo(name = "tax_settings_json")
    val taxSettingsJson: String?,
    val timezone: String,
    val currency: String,
    val language: String,
    @ColumnInfo(name = "date_format")
    val dateFormat: String,
    @ColumnInfo(name = "time_format")
    val timeFormat: String,
    @ColumnInfo(name = "opening_hours")
    val openingHours: String?,
    @ColumnInfo(name = "closing_hours")
    val closingHours: String?,
    @ColumnInfo(name = "operating_days_json")
    val operatingDaysJson: String?,
    val active: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String?,
    @ColumnInfo(name = "created_by")
    val createdBy: String?,
    @ColumnInfo(name = "updated_by")
    val updatedBy: String?,
    @ColumnInfo(name = "custom_attributes_json")
    val customAttributesJson: String?,
    val synced: Boolean,
    @ColumnInfo(name = "last_sync_epoch")
    val lastSyncEpoch: Long,
    @ColumnInfo(name = "local_created_at")
    val localCreatedAt: Long,
    @ColumnInfo(name = "local_updated_at")
    val localUpdatedAt: Long
)

private val jsonFormatter = Json {
    encodeDefaults = false
    ignoreUnknownKeys = true
}

private val listSerializer = ListSerializer(String.serializer())
private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

@OptIn(ExperimentalTime::class)
fun Business.toEntity(
    markSynced: Boolean,
    workspaceId: String?
): BusinessEntity {
    // Evaluate timestamp inside function to avoid race conditions
    val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
    return BusinessEntity(
        uid = id.ifBlank { "${BusinessConstants.LOCAL_ID_PREFIX}$nowEpochMillis" },
        seqId = seqId,
        workspaceId = workspaceId,
        name = name,
        businessType = businessType.name,
        description = description,
        ownerName = ownerName,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        city = city,
        state = state,
        postalCode = postalCode,
        country = country,
        latitude = latitude,
        longitude = longitude,
        phone = phone,
        email = email,
        website = website,
        taxId = taxId,
        registrationNumber = registrationNumber,
        taxSettingsJson = taxSettings?.let { jsonFormatter.encodeToString(mapSerializer, it) },
        timezone = timezone,
        currency = currency,
        language = language,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
        openingHours = openingHours,
        closingHours = closingHours,
        operatingDaysJson = operatingDays.takeIf { it.isNotEmpty() }
            ?.let { jsonFormatter.encodeToString(listSerializer, it) },
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy,
        customAttributesJson = customAttributes?.takeIf { it.isNotEmpty() }
            ?.let { jsonFormatter.encodeToString(mapSerializer, it) },
        synced = markSynced,
        lastSyncEpoch = if (markSynced) nowEpochMillis else 0L,
        localCreatedAt = nowEpochMillis,
        localUpdatedAt = nowEpochMillis
    )
}

fun BusinessEntity.toDomain(): Business {
    val operatingDays = operatingDaysJson?.let {
        runCatching { jsonFormatter.decodeFromString(listSerializer, it) }.getOrNull()
    } ?: emptyList()

    val taxSettings = taxSettingsJson?.let {
        runCatching { jsonFormatter.decodeFromString(mapSerializer, it) }.getOrNull()
    }

    val customAttributes = customAttributesJson?.let {
        runCatching { jsonFormatter.decodeFromString(mapSerializer, it) }.getOrNull()
    }

    return Business(
        id = uid,
        seqId = seqId,
        workspaceId = workspaceId,
        name = name,
        businessType = runCatching { BusinessType.valueOf(businessType) }
            .getOrDefault(BusinessType.RETAIL),
        description = description,
        ownerName = ownerName,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        city = city,
        state = state,
        postalCode = postalCode,
        country = country,
        latitude = latitude,
        longitude = longitude,
        phone = phone,
        email = email,
        website = website,
        taxId = taxId,
        registrationNumber = registrationNumber,
        taxSettings = taxSettings,
        timezone = timezone,
        currency = currency,
        language = language,
        dateFormat = dateFormat,
        timeFormat = timeFormat,
        openingHours = openingHours,
        closingHours = closingHours,
        operatingDays = operatingDays,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy,
        customAttributes = customAttributes
    )
}
