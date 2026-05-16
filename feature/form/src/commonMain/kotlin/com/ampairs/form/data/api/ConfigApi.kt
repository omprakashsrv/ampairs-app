package com.ampairs.form.data.api

import com.ampairs.form.domain.EntityConfigSchema
import com.ampairs.form.domain.EntityFieldConfig
import com.ampairs.form.domain.EntityAttributeDefinition

/**
 * API interface for form configuration
 */
interface ConfigApi {
    /**
     * Get configuration schema for specific entity type
     * @param entityType: "customer", "product", "inventory", etc.
     */
    suspend fun getConfigSchema(entityType: String): EntityConfigSchema

    /**
     * Get all configuration schemas (for admin/settings screens)
     */
    suspend fun getAllConfigSchemas(): List<EntityConfigSchema>

    /**
     * Get all configuration schemas updated since a specific timestamp
     * Used for incremental sync
     * @param lastUpdated: ISO 8601 timestamp string (yyyy-mm-ddTHH:mm:ss)
     */
    suspend fun getConfigSchemasSince(lastUpdated: String): List<EntityConfigSchema>

    /**
     * Update field configuration
     */
    suspend fun updateFieldConfig(fieldConfig: EntityFieldConfig): EntityFieldConfig

    /**
     * Update attribute definition
     */
    suspend fun updateAttributeDefinition(attributeDefinition: EntityAttributeDefinition): EntityAttributeDefinition

    /**
     * Bulk update field configurations for an entity type
     */
    suspend fun updateFieldConfigs(entityType: String, fieldConfigs: List<EntityFieldConfig>): List<EntityFieldConfig>

    /**
     * Bulk update attribute definitions for an entity type
     */
    suspend fun updateAttributeDefinitions(entityType: String, attributeDefinitions: List<EntityAttributeDefinition>): List<EntityAttributeDefinition>

    /**
     * Bulk save complete configuration schema (field configs + attribute definitions)
     * Used for initializing defaults or bulk updates
     */
    suspend fun saveConfigSchema(entityType: String, schema: EntityConfigSchema): EntityConfigSchema
}
