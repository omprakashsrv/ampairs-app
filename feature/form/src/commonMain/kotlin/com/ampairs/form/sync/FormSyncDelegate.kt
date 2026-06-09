package com.ampairs.form.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.db.EntityAttributeDefinitionDao
import com.ampairs.form.data.db.EntityFieldConfigDao
import com.ampairs.form.data.db.toEntity
import com.ampairs.form.data.db.toEntityAttributeDefinition
import com.ampairs.form.data.db.toEntityFieldConfig
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns ALL form-config ↔ server traffic on the unified /sync contract. The form domain has two
 * record types (field configs + attribute definitions), each with its own /sync feed:
 *
 *   PULL  GET  v1/config/{field-configs|attribute-definitions}/sync
 *   PUSH  POST v1/config/{field-configs|attribute-definitions}/sync
 *
 * Both feeds are paged (batch 100) and reconciled local-unsynced-wins / upsert-as-synced. The single
 * `lastSyncedAtIso` checkpoint for SyncEntity.FORM tracks the max `updatedAt` seen across BOTH feeds.
 *
 * LIMITATION — no soft-delete: the backend form tables (FieldConfig / AttributeDefinition) have no
 * soft-delete column, so the pull feed never carries DELETED rows and the push cannot delete. This
 * delegate therefore upserts only; it never hard-deletes local rows from a server DELETED marker
 * (there is none). Propagating deletions needs a backend `deleted`/`status` column (Flyway migration,
 * out of scope). Local edits are still pushed; physical deletions do not round-trip.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.FORM)
class FormSyncDelegate(
    private val api: ConfigApi,
    private val fieldConfigDao: EntityFieldConfigDao,
    private val attributeDefinitionDao: EntityAttributeDefinitionDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.FORM

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pull().fold(
            onSuccess = { SyncResult.Success(it) },
            onFailure = { SyncResult.Failure(it) },
        )

    // --- Push -----------------------------------------------------------------------------------

    /**
     * Bulk push all locally unsynced field configs and attribute definitions in batches of 100.
     * After a successful batch, the pushed rows are marked synced = true locally. (No soft-delete:
     * see the class LIMITATION note.)
     */
    private suspend fun pushPending(): Result<Int> {
        return try {
            var syncedCount = 0
            var failedCount = 0

            val unsyncedFields = fieldConfigDao.getUnsyncedFieldConfigs()
            for (batch in unsyncedFields.chunked(100)) {
                try {
                    api.pushFieldConfigs(batch.map { it.toEntityFieldConfig() })
                    fieldConfigDao.insertFieldConfigs(batch.map { it.copy(synced = true) })
                    syncedCount += batch.size
                } catch (_: Exception) {
                    failedCount += batch.size
                }
            }

            val unsyncedAttributes = attributeDefinitionDao.getUnsyncedAttributeDefinitions()
            for (batch in unsyncedAttributes.chunked(100)) {
                try {
                    api.pushAttributeDefinitions(batch.map { it.toEntityAttributeDefinition() })
                    attributeDefinitionDao.insertAttributeDefinitions(batch.map { it.copy(synced = true) })
                    syncedCount += batch.size
                } catch (_: Exception) {
                    failedCount += batch.size
                }
            }

            if (syncedCount == 0 && failedCount > 0) {
                Result.failure(Exception("$failedCount form config row(s) failed to push — will retry on reconnect"))
            } else {
                Result.success(syncedCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Pull -----------------------------------------------------------------------------------

    /**
     * Batched incremental pull of BOTH feeds. Local unsynced edits win; everything else is upserted
     * as synced. Advances the shared FORM checkpoint to the max `updatedAt` across both feeds.
     */
    private suspend fun pull(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.FORM) ?: ""
            var totalSynced = 0
            var maxServerTime = ""

            // Field configs feed
            var page = 0
            do {
                val response = api.getFieldConfigsSync(lastSync, page, batchSize)
                val rows = response.content
                if (rows.isNotEmpty()) {
                    val entities = rows.mapNotNull { server ->
                        val existing = fieldConfigDao.getFieldConfigById(server.uid)
                        if (existing != null && !existing.synced) null            // local unsynced wins
                        else server.toEntity().copy(synced = true)
                    }
                    if (entities.isNotEmpty()) fieldConfigDao.insertFieldConfigs(entities)
                    val batchMax = rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
                    if (batchMax > maxServerTime) maxServerTime = batchMax
                    totalSynced += rows.size
                }
                page++
            } while (response.hasNext && totalSynced < 10000)

            // Attribute definitions feed
            page = 0
            do {
                val response = api.getAttributeDefinitionsSync(lastSync, page, batchSize)
                val rows = response.content
                if (rows.isNotEmpty()) {
                    val entities = rows.mapNotNull { server ->
                        val existing = attributeDefinitionDao.getAttributeDefinitionById(server.uid)
                        if (existing != null && !existing.synced) null            // local unsynced wins
                        else server.toEntity().copy(synced = true)
                    }
                    if (entities.isNotEmpty()) attributeDefinitionDao.insertAttributeDefinitions(entities)
                    val batchMax = rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
                    if (batchMax > maxServerTime) maxServerTime = batchMax
                    totalSynced += rows.size
                }
                page++
            } while (response.hasNext && totalSynced < 20000)

            if (maxServerTime.isNotBlank()) {
                syncStateDao.setLastSyncedAtIso(SyncEntity.FORM, maxServerTime)
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
