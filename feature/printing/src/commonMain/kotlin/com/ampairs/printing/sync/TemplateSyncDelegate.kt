package com.ampairs.printing.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.data.api.TemplateApi
import com.ampairs.printing.data.api.toDto
import com.ampairs.printing.data.api.toEntity
import com.ampairs.printing.data.db.PrintTemplateDao
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Owns all template ↔ server traffic on the canonical `/printing/v1/templates/sync` contract. The
 * repository is local-only; this delegate bulk-pushes unsynced templates and pulls the workspace set
 * (full pull — templates are few). Local unsynced edits win; server-inactive rows are hard-deleted.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.PRINT_TEMPLATE)
class TemplateSyncDelegate(
    private val api: TemplateApi,
    private val dao: PrintTemplateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.PRINT_TEMPLATE

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    private suspend fun pushPending(): Result<Int> = runCatching {
        val unsynced = dao.unsynced()
        if (unsynced.isEmpty()) return@runCatching 0
        // Templates are few; one batch. Soft-deleted rows ride along in-band (active = false).
        api.pushTemplates(unsynced.map { it.toDto() })
        unsynced.forEach { row ->
            if (row.active) dao.markSynced(row.id) else dao.delete(row.id)
        }
        unsynced.size
    }

    private suspend fun pull(): Result<Int> = runCatching {
        var page = 0
        var applied = 0
        while (true) {
            val response = api.getTemplatesSync(lastSync = "", page = page)
            for (dto in response.content) {
                val existing = dao.getById(dto.id)
                if (existing != null && !existing.synced) continue // local unsynced edit wins
                if (!dto.active) {
                    if (existing != null) dao.delete(dto.id)
                    continue
                }
                dao.upsert(dto.toEntity(synced = true))
                applied++
            }
            if (!response.hasNext) break
            page++
        }
        applied
    }
}
