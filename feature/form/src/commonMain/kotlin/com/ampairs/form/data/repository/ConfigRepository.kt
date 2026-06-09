package com.ampairs.form.data.repository

import com.ampairs.form.data.api.ConfigApi
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
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

/**
 * Form-config repository. Reads/writes are local (Room); the server round-trip lives in
 * [com.ampairs.form.sync.FormSyncDelegate] on the unified /sync contract.
 *
 * The [api] is retained ONLY for the read-only UI fetch [getConfigSchema] (an allowed non-sync,
 * UI-invoked exception). The local-write path ([saveConfigSchema]) does NOT call the API — it writes
 * Room with `synced = false` and flags PENDING_PUSH so the delegate bulk-pushes the change.
 */
@Inject
class ConfigRepository(
    private val api: ConfigApi,
    private val fieldConfigDao: EntityFieldConfigDao,
    private val attributeDefinitionDao: EntityAttributeDefinitionDao,
    private val syncStateDao: SyncStateDao,
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

    /**
     * Incremental pull of BOTH /sync feeds, batched. Mirrors [com.ampairs.form.sync.FormSyncDelegate]
     * — kept so the legacy direct caller (CustomerFormViewModel) still works. New code should prefer
     * `syncService.emit(TriggerPull(SyncEntity.FORM))`.
     */
    override suspend fun syncFormConfigs(): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.FORM) ?: ""
            var total = 0
            var maxServerTime = ""

            var page = 0
            do {
                val response = api.getFieldConfigsSync(lastSync, page, 100)
                val rows = response.content
                if (rows.isNotEmpty()) {
                    val entities = rows.mapNotNull { server ->
                        val existing = fieldConfigDao.getFieldConfigById(server.uid)
                        if (existing != null && !existing.synced) null
                        else server.toEntity().copy(synced = true)
                    }
                    if (entities.isNotEmpty()) fieldConfigDao.insertFieldConfigs(entities)
                    val batchMax = rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
                    if (batchMax > maxServerTime) maxServerTime = batchMax
                    total += rows.size
                }
                page++
            } while (response.hasNext && total < 10000)

            page = 0
            do {
                val response = api.getAttributeDefinitionsSync(lastSync, page, 100)
                val rows = response.content
                if (rows.isNotEmpty()) {
                    val entities = rows.mapNotNull { server ->
                        val existing = attributeDefinitionDao.getAttributeDefinitionById(server.uid)
                        if (existing != null && !existing.synced) null
                        else server.toEntity().copy(synced = true)
                    }
                    if (entities.isNotEmpty()) attributeDefinitionDao.insertAttributeDefinitions(entities)
                    val batchMax = rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
                    if (batchMax > maxServerTime) maxServerTime = batchMax
                    total += rows.size
                }
                page++
            } while (response.hasNext && total < 20000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.FORM, maxServerTime)
            }
            Result.success(total)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Local-only write of an edited schema. Writes both record types to Room as `synced = false`
     * and flags PENDING_PUSH; [com.ampairs.form.sync.FormSyncDelegate] then bulk-pushes to /sync.
     * No API call here (offline-first write path).
     */
    @OptIn(ExperimentalTime::class)
    suspend fun saveConfigSchema(entityType: String, schema: EntityConfigSchema): Result<EntityConfigSchema> {
        return try {
            if (schema.fieldConfigs.isNotEmpty()) {
                fieldConfigDao.insertFieldConfigs(
                    schema.fieldConfigs.map { it.toEntity().copy(synced = false) }
                )
            }
            if (schema.attributeDefinitions.isNotEmpty()) {
                attributeDefinitionDao.insertAttributeDefinitions(
                    schema.attributeDefinitions.map { it.toEntity().copy(synced = false) }
                )
            }
            syncStateDao.markPendingPush(SyncEntity.FORM, Clock.System.now().toEpochMilliseconds())
            Result.success(schema)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
