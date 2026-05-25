package com.ampairs.form.data.repository

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.db.EntityAttributeDefinitionDao
import com.ampairs.form.data.db.EntityFieldConfigDao
import com.ampairs.form.data.db.toEntity
import com.ampairs.form.data.db.toEntityAttributeDefinition
import com.ampairs.form.data.db.toEntityFieldConfig
import com.ampairs.form.domain.EntityConfigSchema
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Inject
class ConfigRepository(
    private val api: ConfigApi,
    private val fieldConfigDao: EntityFieldConfigDao,
    private val attributeDefinitionDao: EntityAttributeDefinitionDao,
    private val appPreferences: AppPreferencesDataStore
) : ConfigLookup {

    suspend fun getConfigSchema(entityType: String): Result<EntityConfigSchema> {
        return try {
            val schema = api.getConfigSchema(entityType)

            if (schema.fieldConfigs.isNotEmpty() || schema.attributeDefinitions.isNotEmpty()) {
                if (schema.fieldConfigs.isNotEmpty()) {
                    fieldConfigDao.insertFieldConfigs(schema.fieldConfigs.map { it.toEntity() })
                }
                if (schema.attributeDefinitions.isNotEmpty()) {
                    attributeDefinitionDao.insertAttributeDefinitions(schema.attributeDefinitions.map { it.toEntity() })
                }
            }

            Result.success(schema)
        } catch (e: Exception) {
            val cachedConfig = observeConfigSchema(entityType).first()
            if (cachedConfig != null) {
                return Result.success(cachedConfig)
            }
            Result.failure(e)
        }
    }

    override fun observeConfigSchema(entityType: String): Flow<EntityConfigSchema?> {
        return combine(
            fieldConfigDao.getFieldConfigsByEntityType(entityType),
            attributeDefinitionDao.getAttributeDefinitionsByEntityType(entityType)
        ) { fieldConfigs, attributeDefinitions ->
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

    override suspend fun refreshConfig(entityType: String): Result<EntityConfigSchema> {
        return getConfigSchema(entityType)
    }

    override suspend fun syncFormConfigs(): Result<Int> {
        return try {
            val lastSyncTime = appPreferences.getFormConfigLastSyncTime().first()

            val schemas = if (lastSyncTime.isBlank()) {
                api.getAllConfigSchemas()
            } else {
                api.getConfigSchemasSince(lastSyncTime)
            }

            var savedCount = 0
            schemas.forEach { schema ->
                try {
                    if (schema.fieldConfigs.isNotEmpty()) {
                        fieldConfigDao.insertFieldConfigs(schema.fieldConfigs.map { it.toEntity() })
                    }
                    if (schema.attributeDefinitions.isNotEmpty()) {
                        attributeDefinitionDao.insertAttributeDefinitions(schema.attributeDefinitions.map { it.toEntity() })
                    }
                    savedCount++
                } catch (_: Exception) {
                    // Continue with other schemas even if one fails
                }
            }

            val allTimestamps = schemas.flatMap { schema ->
                schema.fieldConfigs.mapNotNull { it.updatedAt } +
                    schema.attributeDefinitions.mapNotNull { it.updatedAt }
            }
            appPreferences.setFormConfigLastSyncTime(allTimestamps.maxOrNull() ?: currentTimestamp())

            Result.success(savedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveConfigSchema(entityType: String, schema: EntityConfigSchema): Result<EntityConfigSchema> {
        return try {
            val savedSchema = api.saveConfigSchema(entityType, schema)

            // Full sync: replace local data with exactly what the backend returned
            fieldConfigDao.deleteFieldConfigsByEntityType(entityType)
            attributeDefinitionDao.deleteAttributeDefinitionsByEntityType(entityType)

            if (savedSchema.fieldConfigs.isNotEmpty()) {
                fieldConfigDao.insertFieldConfigs(savedSchema.fieldConfigs.map { it.toEntity() })
            }
            if (savedSchema.attributeDefinitions.isNotEmpty()) {
                attributeDefinitionDao.insertAttributeDefinitions(savedSchema.attributeDefinitions.map { it.toEntity() })
            }

            Result.success(savedSchema)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun currentTimestamp(): String = Clock.System.now().toString()
}
