package com.ampairs.form.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Generic configuration schema combining field configs and attribute definitions
 * Can be used for any entity type (Customer, Product, Inventory, etc.)
 */
@Serializable
data class EntityConfigSchema(
    @SerialName("field_configs")
    val fieldConfigs: List<EntityFieldConfig> = emptyList(),
    @SerialName("attribute_definitions")
    val attributeDefinitions: List<EntityAttributeDefinition> = emptyList(),
    @SerialName("last_updated")
    val lastUpdated: String? = null
) {
    /**
     * Get visible field configurations sorted by display order
     */
    fun getVisibleFields(): List<EntityFieldConfig> {
        return fieldConfigs
            .filter { it.visible && it.enabled }
            .sortedBy { it.displayOrder }
    }

    /**
     * Get mandatory field configurations
     */
    fun getMandatoryFields(): List<EntityFieldConfig> {
        return fieldConfigs
            .filter { it.mandatory && it.enabled }
    }

    /**
     * Get visible attribute definitions sorted by display order
     */
    fun getVisibleAttributes(): List<EntityAttributeDefinition> {
        return attributeDefinitions
            .filter { it.visible && it.enabled }
            .sortedBy { it.displayOrder }
    }

    /**
     * Get mandatory attribute definitions
     */
    fun getMandatoryAttributes(): List<EntityAttributeDefinition> {
        return attributeDefinitions
            .filter { it.mandatory && it.enabled }
    }

    /**
     * Get attributes grouped by category
     */
    fun getAttributesByCategory(): Map<String, List<EntityAttributeDefinition>> {
        return getVisibleAttributes()
            .groupBy { it.category ?: "Other" }
    }

    /**
     * Get field configuration by field name
     */
    fun getFieldConfig(fieldName: String): EntityFieldConfig? {
        return fieldConfigs.find { it.fieldName == fieldName }
    }

    /**
     * Get attribute definition by attribute key
     */
    fun getAttributeDefinition(attributeKey: String): EntityAttributeDefinition? {
        return attributeDefinitions.find { it.attributeKey == attributeKey }
    }

    /**
     * Check if a field is visible
     */
    fun isFieldVisible(fieldName: String): Boolean {
        return getFieldConfig(fieldName)?.let { it.visible && it.enabled } ?: true
    }

    /**
     * Check if a field is mandatory
     */
    fun isFieldMandatory(fieldName: String): Boolean {
        return getFieldConfig(fieldName)?.let { it.mandatory && it.enabled } ?: false
    }

    /**
     * Check if an attribute is visible
     */
    fun isAttributeVisible(attributeKey: String): Boolean {
        return getAttributeDefinition(attributeKey)?.let { it.visible && it.enabled } ?: true
    }

    /**
     * Check if an attribute is mandatory
     */
    fun isAttributeMandatory(attributeKey: String): Boolean {
        return getAttributeDefinition(attributeKey)?.let { it.mandatory && it.enabled } ?: false
    }
}
