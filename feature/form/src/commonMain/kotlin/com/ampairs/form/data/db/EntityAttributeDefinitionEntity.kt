package com.ampairs.form.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ampairs.form.domain.EntityAttributeDefinition

@Entity(tableName = "entity_attribute_definitions")
data class EntityAttributeDefinitionEntity(
    @PrimaryKey
    val uid: String,
    val entityType: String,
    val attributeKey: String,
    val displayName: String,
    val dataType: String,
    val visible: Boolean = true,
    val mandatory: Boolean = false,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val category: String? = null,
    val defaultValue: String? = null,
    val validationType: String? = null,
    val validationParams: String? = null, // JSON string for Map<String, String>
    val enumValues: String? = null, // JSON string for List<String>
    val placeholder: String? = null,
    val helpText: String? = null,
    val synced: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

fun EntityAttributeDefinitionEntity.toEntityAttributeDefinition(): EntityAttributeDefinition = EntityAttributeDefinition(
    uid = uid,
    entityType = entityType,
    attributeKey = attributeKey,
    displayName = displayName,
    dataType = dataType,
    visible = visible,
    mandatory = mandatory,
    enabled = enabled,
    displayOrder = displayOrder,
    category = category,
    defaultValue = defaultValue,
    validationType = validationType,
    validationParams = validationParams?.let { parseJsonToMap(it) },
    enumValues = enumValues?.let { parseJsonToList(it) },
    placeholder = placeholder,
    helpText = helpText,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun EntityAttributeDefinition.toEntity(): EntityAttributeDefinitionEntity = EntityAttributeDefinitionEntity(
    uid = uid,
    entityType = entityType,
    attributeKey = attributeKey,
    displayName = displayName,
    dataType = dataType,
    visible = visible,
    mandatory = mandatory,
    enabled = enabled,
    displayOrder = displayOrder,
    category = category,
    defaultValue = defaultValue,
    validationType = validationType,
    validationParams = validationParams?.let { mapToJson(it) },
    enumValues = enumValues?.let { listToJson(it) },
    placeholder = placeholder,
    helpText = helpText,
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

private fun parseJsonToList(json: String): List<String> {
    return try {
        kotlinx.serialization.json.Json.decodeFromString(json)
    } catch (e: Exception) {
        emptyList()
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

private fun listToJson(list: List<String>): String {
    return try {
        kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.serializer(),
            list
        )
    } catch (e: Exception) {
        "[]"
    }
}
