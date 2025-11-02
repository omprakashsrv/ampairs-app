package com.ampairs.form.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.form.domain.EntityFieldConfig

@Entity(tableName = "entity_field_configs")
data class EntityFieldConfigEntity(
    @PrimaryKey
    val uid: String,
    val entityType: String,
    val fieldName: String,
    val displayName: String,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val validationType: String? = null,
    val validationParams: String? = null, // JSON string for Map<String, String>
    val placeholder: String? = null,
    val helpText: String? = null,
    val defaultValue: String? = null,
    val synced: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

fun EntityFieldConfigEntity.toEntityFieldConfig(): EntityFieldConfig = EntityFieldConfig(
    uid = uid,
    entityType = entityType,
    fieldName = fieldName,
    displayName = displayName,
    visible = visible,
    mandatory = mandatory,
    enabled = enabled,
    displayOrder = displayOrder,
    validationType = validationType,
    validationParams = validationParams?.let { parseJsonToMap(it) },
    placeholder = placeholder,
    helpText = helpText,
    defaultValue = defaultValue,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun EntityFieldConfig.toEntity(): EntityFieldConfigEntity = EntityFieldConfigEntity(
    uid = uid,
    entityType = entityType,
    fieldName = fieldName,
    displayName = displayName,
    visible = visible,
    mandatory = mandatory,
    enabled = enabled,
    displayOrder = displayOrder,
    validationType = validationType,
    validationParams = validationParams?.let { mapToJson(it) },
    placeholder = placeholder,
    helpText = helpText,
    defaultValue = defaultValue,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// Helper functions for JSON serialization
private fun parseJsonToMap(json: String): Map<String, String> {
    return try {
        kotlinx.serialization.json.Json.decodeFromString(json)
    } catch (e: Exception) {
        emptyMap()
    }
}

private fun mapToJson(map: Map<String, String>): String {
    return try {
        kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.serializer(),
            map
        )
    } catch (e: Exception) {
        "{}"
    }
}
