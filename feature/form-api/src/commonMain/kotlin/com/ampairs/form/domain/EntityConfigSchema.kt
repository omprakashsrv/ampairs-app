package com.ampairs.form.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntityConfigSchema(
    @SerialName("field_configs") val fieldConfigs: List<EntityFieldConfig> = emptyList(),
    @SerialName("attribute_definitions") val attributeDefinitions: List<EntityAttributeDefinition> = emptyList(),
    @SerialName("last_updated") val lastUpdated: String? = null,
) {
    fun getVisibleFields(): List<EntityFieldConfig> =
        fieldConfigs.filter { it.visible && it.enabled }.sortedBy { it.displayOrder }

    fun getMandatoryFields(): List<EntityFieldConfig> =
        fieldConfigs.filter { it.mandatory && it.enabled }

    fun getVisibleAttributes(): List<EntityAttributeDefinition> =
        attributeDefinitions.filter { it.visible && it.enabled }.sortedBy { it.displayOrder }

    fun getMandatoryAttributes(): List<EntityAttributeDefinition> =
        attributeDefinitions.filter { it.mandatory && it.enabled }

    fun getAttributesByCategory(): Map<String, List<EntityAttributeDefinition>> =
        getVisibleAttributes().groupBy { it.category ?: "Other" }

    fun getFieldConfig(fieldName: String): EntityFieldConfig? =
        fieldConfigs.find { it.fieldName == fieldName }

    fun getAttributeDefinition(attributeKey: String): EntityAttributeDefinition? =
        attributeDefinitions.find { it.attributeKey == attributeKey }

    fun isFieldVisible(fieldName: String): Boolean =
        getFieldConfig(fieldName)?.let { it.visible && it.enabled } ?: true

    fun isFieldMandatory(fieldName: String): Boolean =
        getFieldConfig(fieldName)?.let { it.mandatory && it.enabled } ?: false

    fun isAttributeVisible(attributeKey: String): Boolean =
        getAttributeDefinition(attributeKey)?.let { it.visible && it.enabled } ?: true

    fun isAttributeMandatory(attributeKey: String): Boolean =
        getAttributeDefinition(attributeKey)?.let { it.mandatory && it.enabled } ?: false
}
