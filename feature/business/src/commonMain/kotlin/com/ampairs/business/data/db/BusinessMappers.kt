package com.ampairs.business.data.db

import com.ampairs.business.domain.Business
import com.ampairs.business.domain.BusinessType
import com.ampairs.business.util.BusinessConstants
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// Entity <-> domain mappers. The @Entity class lives in :data:database (same package); these
// mappers stay in the feature module because they reference the business domain models.

private val jsonFormatter = Json {
    encodeDefaults = false
    ignoreUnknownKeys = true
}

private val listSerializer = ListSerializer(String.serializer())
private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

@OptIn(ExperimentalTime::class)
fun Business.toEntity(markSynced: Boolean): BusinessEntity {
    // Evaluate timestamp inside function to avoid race conditions
    val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
    return BusinessEntity(
        uid = id.ifBlank { "${BusinessConstants.LOCAL_ID_PREFIX}$nowEpochMillis" },
        seqId = seqId,
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

    val customAttributes = customAttributesJson?.let {
        runCatching { jsonFormatter.decodeFromString(mapSerializer, it) }.getOrNull()
    }

    return Business(
        id = uid,
        seqId = seqId,
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
