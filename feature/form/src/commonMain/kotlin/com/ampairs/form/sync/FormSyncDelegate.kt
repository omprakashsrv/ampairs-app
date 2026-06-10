package com.ampairs.form.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.FORM_SYNC_BATCH_SIZE
import com.ampairs.form.FormLogger
import com.ampairs.form.data.api.ConfigApi
import com.ampairs.form.data.db.FormFieldDao
import com.ampairs.form.data.db.FormSchemaDao
import com.ampairs.form.data.db.FormSchemaEntity
import com.ampairs.form.data.db.FormSectionDao
import com.ampairs.form.data.db.buildSchema
import com.ampairs.form.data.db.toEntity
import com.ampairs.form.domain.FormSchema
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns ALL form-config ↔ server traffic on the single aggregate `/config/schema/sync` feed (spec 011).
 * One record per entityType. Pull REPLACES each local aggregate (members absent on the server copy are
 * dropped → delete-by-absence); a locally-dirty aggregate wins (skipped). Push sends dirty aggregates
 * with `base_version`. The shared FORM checkpoint tracks the max `lastUpdated` seen.
 *
 * NOTE (spec 011): drafted, compile-gate on device.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.FORM)
class FormSyncDelegate(
    private val api: ConfigApi,
    private val schemaDao: FormSchemaDao,
    private val sectionDao: FormSectionDao,
    private val fieldDao: FormFieldDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.FORM

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    private suspend fun pull(): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.FORM) ?: ""
        var total = 0
        var maxServerTime = ""
        var page = 0
        do {
            val response = api.getSchemaSync(lastSync, page, FORM_SYNC_BATCH_SIZE)
            val schemas = response.content
            for (schema in schemas) {
                val localDirty = schemaDao.getByEntityType(schema.entityType)?.dirty == true
                if (!localDirty) replaceLocal(schema, dirty = false)   // local unsynced edits win otherwise
                schema.lastUpdated?.takeIf { it.isNotBlank() && it > maxServerTime }?.let { maxServerTime = it }
                total++
            }
            page++
        } while (response.hasNext && total < 10_000)

        if (maxServerTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.FORM, maxServerTime)
        total
    }

    private suspend fun pushPending(): Result<Int> = runCatching {
        val dirty = schemaDao.getDirty()
        if (dirty.isEmpty()) return@runCatching 0
        var pushed = 0
        var failed = 0
        for (header in dirty) {
            val local = buildSchema(
                entityType = header.entityType,
                header = header,
                sections = sectionDao.getByEntityType(header.entityType),
                fields = fieldDao.getByEntityType(header.entityType),
            )
            try {
                val resolved = api.pushSchema(listOf(local)).firstOrNull()
                if (resolved != null) replaceLocal(resolved, dirty = false) else schemaDao.setDirty(header.entityType, false)
                pushed++
            } catch (e: Exception) {
                FormLogger.w("FormSyncDelegate", "push failed for '${header.entityType}'", e)
                failed++
            }
        }
        if (pushed == 0 && failed > 0) throw Exception("$failed form schema(s) failed to push — will retry on reconnect")
        pushed
    }

    /** Replace the local aggregate for one entityType (delete-by-absence) and set its header. */
    private suspend fun replaceLocal(schema: FormSchema, dirty: Boolean) {
        sectionDao.deleteByEntityType(schema.entityType)
        fieldDao.deleteByEntityType(schema.entityType)
        sectionDao.insertAll(schema.sections.map { it.toEntity(schema.entityType) })
        fieldDao.insertAll(schema.fields.map { it.toEntity(schema.entityType) })
        schemaDao.upsert(
            FormSchemaEntity(
                entityType = schema.entityType,
                version = schema.version,
                dirty = dirty,
                updatedAt = schema.lastUpdated,
            )
        )
    }
}
