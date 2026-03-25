package com.ampairs.form.data.repository

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.db.EntityAttributeDefinitionDao
import com.ampairs.form.data.db.EntityFieldConfigDao
import com.ampairs.form.data.db.toEntity
import com.ampairs.form.data.db.toEntityAttributeDefinition
import com.ampairs.form.data.db.toEntityFieldConfig
import com.ampairs.form.domain.EntityConfigSchema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository for managing entity configuration schemas
 * Provides offline-first architecture with database caching and reactive updates per entity type
 */
@OptIn(ExperimentalTime::class)
class ConfigRepository(
    private val api: ConfigApi,
    private val fieldConfigDao: EntityFieldConfigDao,
    private val attributeDefinitionDao: EntityAttributeDefinitionDao,
    private val appPreferences: AppPreferencesDataStore
) {
    // Cache configs by entity type
    private val _configCache = MutableStateFlow<Map<String, EntityConfigSchema>>(emptyMap())

    /**
     * Get config schema for specific entity type
     * Fetches from backend, saves to database, and updates cache
     * Backend is responsible for seeding defaults if config doesn't exist
     */
    suspend fun getConfigSchema(entityType: String): Result<EntityConfigSchema> {
        return try {
            // Backend should return seeded defaults if config doesn't exist
            val schema = api.getConfigSchema(entityType)

            // Only save to database if backend returned data
            if (schema.fieldConfigs.isNotEmpty() || schema.attributeDefinitions.isNotEmpty()) {
                // Save field configs
                if (schema.fieldConfigs.isNotEmpty()) {
                    val fieldConfigEntities = schema.fieldConfigs.map { it.toEntity() }
                    fieldConfigDao.insertFieldConfigs(fieldConfigEntities)
                }

                // Save attribute definitions
                if (schema.attributeDefinitions.isNotEmpty()) {
                    val attributeDefinitionEntities = schema.attributeDefinitions.map { it.toEntity() }
                    attributeDefinitionDao.insertAttributeDefinitions(attributeDefinitionEntities)
                }

                updateCache(entityType, schema)
            } else {
                println("⚠️ Backend returned empty config for $entityType - not saving to database")
            }

            Result.success(schema)
        } catch (e: Exception) {
            println("❌ Failed to fetch config for $entityType: ${e.message}")

            // On network error, use cached data from database
            val cachedConfig = observeConfigSchema(entityType).first()
            if (cachedConfig != null) {
                println("📦 Using cached config for $entityType")
                return Result.success(cachedConfig)
            }

            Result.failure(e)
        }
    }

    /**
     * Observe config schema for specific entity type from database
     * Returns Flow that emits data from local database
     */
    fun observeConfigSchema(entityType: String): Flow<EntityConfigSchema?> {
        return combine(
            fieldConfigDao.getFieldConfigsByEntityType(entityType),
            attributeDefinitionDao.getAttributeDefinitionsByEntityType(entityType)
        ) { fieldConfigs, attributeDefinitions ->
            // Combine field configs and attribute definitions into schema
            if (fieldConfigs.isEmpty() && attributeDefinitions.isEmpty()) {
                null
            } else {
                EntityConfigSchema(
                    fieldConfigs = fieldConfigs.map { it.toEntityFieldConfig() },
                    attributeDefinitions = attributeDefinitions.map { it.toEntityAttributeDefinition() }
                )
            }
        }
    }

    /**
     * Refresh config from backend
     */
    suspend fun refreshConfig(entityType: String): Result<EntityConfigSchema> {
        return getConfigSchema(entityType)
    }

    /**
     * Preload configs for multiple entity types
     * Useful for app initialization
     */
    suspend fun preloadConfigs(entityTypes: List<String>): Result<Unit> {
        return try {
            entityTypes.forEach { type ->
                getConfigSchema(type)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all cached configs
     */
    fun getAllCachedConfigs(): Map<String, EntityConfigSchema> {
        return _configCache.value
    }

    /**
     * Clear cache for specific entity type
     */
    fun clearCache(entityType: String) {
        _configCache.value = _configCache.value - entityType
    }

    /**
     * Clear all cached configs
     */
    fun clearAllCache() {
        _configCache.value = emptyMap()
    }

    /**
     * Sync all form configurations from backend
     * Uses incremental sync based on last sync time
     */
    suspend fun syncFormConfigs(): Result<Int> {
        return try {
            // Get last sync time
            val lastSyncTime = appPreferences.getFormConfigLastSyncTime().first()

            println("🔄 Syncing form configs (lastSync: ${lastSyncTime.ifBlank { "never" }})")

            // Fetch configs updated since last sync
            val schemas = if (lastSyncTime.isBlank()) {
                // First sync - get all configs
                api.getAllConfigSchemas()
            } else {
                // Incremental sync - get only updated configs
                api.getConfigSchemasSince(lastSyncTime)
            }

            println("📥 Received ${schemas.size} config schemas from backend")

            var savedCount = 0

            // Save each schema to database
            schemas.forEach { schema ->
                try {
                    // Save field configs
                    val fieldConfigEntities = schema.fieldConfigs.map { it.toEntity() }
                    if (fieldConfigEntities.isNotEmpty()) {
                        fieldConfigDao.insertFieldConfigs(fieldConfigEntities)
                    }

                    // Save attribute definitions
                    val attributeDefinitionEntities = schema.attributeDefinitions.map { it.toEntity() }
                    if (attributeDefinitionEntities.isNotEmpty()) {
                        attributeDefinitionDao.insertAttributeDefinitions(attributeDefinitionEntities)
                    }

                    savedCount++
                } catch (e: Exception) {
                    println("❌ Failed to save config for entity: ${e.message}")
                    // Continue with other configs even if one fails
                }
            }

            // Update last sync time to current server time
            // Use the latest updatedAt from all configs, or current timestamp
            val allTimestamps = mutableListOf<String>()
            schemas.forEach { schema ->
                allTimestamps.addAll(schema.fieldConfigs.mapNotNull { it.updatedAt })
                allTimestamps.addAll(schema.attributeDefinitions.mapNotNull { it.updatedAt })
            }
            val maxUpdatedAt = allTimestamps.maxOrNull() ?: getCurrentTimestamp()

            appPreferences.setFormConfigLastSyncTime(maxUpdatedAt)

            println("✅ Synced $savedCount form config schemas")
            Result.success(savedCount)

        } catch (e: Exception) {
            println("❌ Form config sync failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get current timestamp in ISO 8601 format
     */
    private fun getCurrentTimestamp(): String {
        return Clock.System.now().toString()
    }

    /**
     * Update field configuration and save to server
     */
    suspend fun updateFieldConfig(fieldConfig: com.ampairs.form.domain.EntityFieldConfig): Result<com.ampairs.form.domain.EntityFieldConfig> {
        return try {
            val updated = api.updateFieldConfig(fieldConfig)
            fieldConfigDao.insertFieldConfig(updated.toEntity())
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update attribute definition and save to server
     */
    suspend fun updateAttributeDefinition(attributeDefinition: com.ampairs.form.domain.EntityAttributeDefinition): Result<com.ampairs.form.domain.EntityAttributeDefinition> {
        return try {
            val updated = api.updateAttributeDefinition(attributeDefinition)
            attributeDefinitionDao.insertAttributeDefinition(updated.toEntity())
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bulk update field configurations for an entity type
     */
    suspend fun updateFieldConfigs(entityType: String, fieldConfigs: List<com.ampairs.form.domain.EntityFieldConfig>): Result<List<com.ampairs.form.domain.EntityFieldConfig>> {
        return try {
            val updated = api.updateFieldConfigs(entityType, fieldConfigs)
            val entities = updated.map { config -> config.toEntity() }
            fieldConfigDao.insertFieldConfigs(entities)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bulk update attribute definitions for an entity type
     */
    suspend fun updateAttributeDefinitions(entityType: String, attributeDefinitions: List<com.ampairs.form.domain.EntityAttributeDefinition>): Result<List<com.ampairs.form.domain.EntityAttributeDefinition>> {
        return try {
            val updated = api.updateAttributeDefinitions(entityType, attributeDefinitions)
            val entities = updated.map { attr -> attr.toEntity() }
            attributeDefinitionDao.insertAttributeDefinitions(entities)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save complete configuration schema (field configs + attribute definitions)
     * Used for initializing backend with defaults from frontend
     */
    suspend fun saveConfigSchema(entityType: String, schema: EntityConfigSchema): Result<EntityConfigSchema> {
        return try {
            println("🔄 Saving complete config schema for $entityType to backend")

            val savedSchema = api.saveConfigSchema(entityType, schema)

            // CRITICAL: Full sync - delete all existing configs for this entity type
            // then insert only what's in the backend response
            println("🗑️ Clearing existing configs for $entityType from local database")
            fieldConfigDao.deleteFieldConfigsByEntityType(entityType)
            attributeDefinitionDao.deleteAttributeDefinitionsByEntityType(entityType)

            // Save to local database - only what backend returned
            if (savedSchema.fieldConfigs.isNotEmpty()) {
                val fieldConfigEntities = savedSchema.fieldConfigs.map { it.toEntity() }
                fieldConfigDao.insertFieldConfigs(fieldConfigEntities)
                println("💾 Saved ${fieldConfigEntities.size} field configs to local database")
            }

            if (savedSchema.attributeDefinitions.isNotEmpty()) {
                val attributeDefinitionEntities = savedSchema.attributeDefinitions.map { it.toEntity() }
                attributeDefinitionDao.insertAttributeDefinitions(attributeDefinitionEntities)
                println("💾 Saved ${attributeDefinitionEntities.size} attribute definitions to local database")
            }

            updateCache(entityType, savedSchema)
            println("✅ Successfully saved config schema for $entityType")

            Result.success(savedSchema)
        } catch (e: Exception) {
            println("❌ Failed to save config schema for $entityType: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update cache with new schema
     */
    private fun updateCache(entityType: String, schema: EntityConfigSchema) {
        _configCache.value = _configCache.value + (entityType to schema)
    }
}
