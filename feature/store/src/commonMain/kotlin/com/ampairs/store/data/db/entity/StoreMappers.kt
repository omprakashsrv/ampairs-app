package com.ampairs.store.data.db.entity

import com.ampairs.store.domain.SettingValueType
import com.ampairs.store.domain.definition.StoreSettingDefinition
import com.ampairs.store.domain.model.StoreSetting

// Entity <-> domain mappers. The @Entity classes live in :data:database (same package); these
// mappers stay in the feature module because they reference the store domain models/enums.

fun StoreSettingEntity.toStoreSetting(): StoreSetting = StoreSetting(
    uid = id,
    module = module,
    key = settingKey,
    value = value,
    valueType = valueType,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun StoreSetting.toEntity(): StoreSettingEntity = StoreSettingEntity(
    id = uid,
    module = module,
    settingKey = key,
    value = value,
    valueType = valueType,
    active = active,
    refId = refId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun StoreSettingDefinitionEntity.toDefinition(): StoreSettingDefinition = StoreSettingDefinition(
    module = module,
    key = settingKey,
    valueType = SettingValueType.fromName(valueType),
    defaultValue = defaultValue,
    allowedValues = if (allowedValues.isBlank()) emptyList() else allowedValues.split("\n"),
    label = label,
    description = description,
)

fun StoreSettingDefinition.toEntity(): StoreSettingDefinitionEntity = StoreSettingDefinitionEntity(
    id = "$module/$key",
    module = module,
    settingKey = key,
    valueType = valueType.name,
    defaultValue = defaultValue,
    allowedValues = allowedValues.joinToString("\n"),
    label = label,
    description = description,
)
