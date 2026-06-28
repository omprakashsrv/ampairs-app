package com.ampairs.communication.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.communication.data.api.CommunicationSyncApi
import com.ampairs.communication.data.api.toDto
import com.ampairs.communication.data.api.toEntity
import com.ampairs.communication.data.db.CommBindingDao
import com.ampairs.communication.data.db.CommCampaignDao
import com.ampairs.communication.data.db.CommLogDao
import com.ampairs.communication.data.db.CommPreferenceDao
import com.ampairs.communication.data.db.CommScheduleDao
import com.ampairs.communication.data.db.CommTemplateDao
import com.ampairs.communication.util.CommunicationLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

private const val PAGE = 100
private const val MAX = 10_000

/**
 * Templates — aggregate-grained `/sync`. The repository is local-only; this delegate bulk-pushes
 * unsynced aggregates (variants bundled) and pulls the workspace set. The server response carries
 * the fresh `base_version`, applied as synced=true. Local unsynced edits win; server-inactive rows
 * are hard-deleted.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.COMM_TEMPLATE)
class CommTemplateSyncDelegate(
    private val api: CommunicationSyncApi,
    private val dao: CommTemplateDao,
) : SyncDelegate {
    override val entity = SyncEntity.COMM_TEMPLATE

    override suspend fun pullFromServer(): SyncResult = runCatching {
        var page = 0
        var applied = 0
        do {
            val resp = api.pullTemplates(page, PAGE, null)
            if (resp.error != null) throw Exception(resp.error?.message ?: "Network error")
            val batch = resp.data?.content ?: emptyList()
            for (dto in batch) {
                val existing = dao.getByUid(dto.uid)
                if (existing != null && !existing.synced) continue
                if (!dto.active) { if (existing != null) dao.hardDelete(dto.uid); continue }
                dao.upsert(dto.toEntity(synced = true))
                applied++
            }
            page++
            val hasNext = resp.data?.hasNext == true
        } while (hasNext && applied < MAX)
        applied
    }.toResult("CommTemplateSyncDelegate")

    override suspend fun pushPendingToServer(): SyncResult = runCatching {
        val unsynced = dao.unsynced()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0
        for (batch in unsynced.chunked(PAGE)) {
            val resp = api.pushTemplates(batch.map { it.toDto() })
            if (resp.data != null && resp.error == null) {
                for (dto in resp.data ?: emptyList()) {
                    if (!dto.active) dao.hardDelete(dto.uid) else dao.upsert(dto.toEntity(synced = true))
                }
                synced += batch.size
            } else {
                throw Exception(resp.error?.message ?: "Template push failed")
            }
        }
        synced
    }.toResult("CommTemplateSyncDelegate")

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.COMM_BINDING)
class CommBindingSyncDelegate(
    private val api: CommunicationSyncApi,
    private val dao: CommBindingDao,
) : SyncDelegate {
    override val entity = SyncEntity.COMM_BINDING
    override val pushDependencies = listOf(SyncEntity.COMM_TEMPLATE)

    override suspend fun pullFromServer(): SyncResult = runCatching {
        var page = 0; var applied = 0
        do {
            val resp = api.pullBindings(page, PAGE, null)
            if (resp.error != null) throw Exception(resp.error?.message ?: "Network error")
            val batch = resp.data?.content ?: emptyList()
            for (dto in batch) {
                val existing = dao.getByUid(dto.uid)
                if (existing != null && !existing.synced) continue
                if (!dto.active) { if (existing != null) dao.hardDelete(dto.uid); continue }
                dao.upsert(dto.toEntity(synced = true)); applied++
            }
            page++
            val hasNext = resp.data?.hasNext == true
        } while (hasNext && applied < MAX)
        applied
    }.toResult("CommBindingSyncDelegate")

    override suspend fun pushPendingToServer(): SyncResult = runCatching {
        val unsynced = dao.unsynced()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0
        for (batch in unsynced.chunked(PAGE)) {
            val resp = api.pushBindings(batch.map { it.toDto() })
            if (resp.data != null && resp.error == null) {
                for (row in batch) { if (!row.active) dao.hardDelete(row.uid) else dao.markSynced(row.uid) }
                synced += batch.size
            } else throw Exception(resp.error?.message ?: "Binding push failed")
        }
        synced
    }.toResult("CommBindingSyncDelegate")

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.COMM_SCHEDULE)
class CommScheduleSyncDelegate(
    private val api: CommunicationSyncApi,
    private val dao: CommScheduleDao,
) : SyncDelegate {
    override val entity = SyncEntity.COMM_SCHEDULE
    override val pushDependencies = listOf(SyncEntity.COMM_TEMPLATE)

    override suspend fun pullFromServer(): SyncResult = runCatching {
        var page = 0; var applied = 0
        do {
            val resp = api.pullSchedules(page, PAGE, null)
            if (resp.error != null) throw Exception(resp.error?.message ?: "Network error")
            val batch = resp.data?.content ?: emptyList()
            for (dto in batch) {
                val existing = dao.getByUid(dto.uid)
                if (existing != null && !existing.synced) continue
                if (!dto.active) { if (existing != null) dao.hardDelete(dto.uid); continue }
                dao.upsert(dto.toEntity(synced = true)); applied++
            }
            page++
            val hasNext = resp.data?.hasNext == true
        } while (hasNext && applied < MAX)
        applied
    }.toResult("CommScheduleSyncDelegate")

    override suspend fun pushPendingToServer(): SyncResult = runCatching {
        val unsynced = dao.unsynced()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0
        for (batch in unsynced.chunked(PAGE)) {
            val resp = api.pushSchedules(batch.map { it.toDto() })
            if (resp.data != null && resp.error == null) {
                // Server owns next_run_at/last_run_at — apply the returned rows as synced.
                for (dto in resp.data ?: emptyList()) {
                    if (!dto.active) dao.hardDelete(dto.uid) else dao.upsert(dto.toEntity(synced = true))
                }
                synced += batch.size
            } else throw Exception(resp.error?.message ?: "Schedule push failed")
        }
        synced
    }.toResult("CommScheduleSyncDelegate")

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.COMM_CAMPAIGN)
class CommCampaignSyncDelegate(
    private val api: CommunicationSyncApi,
    private val dao: CommCampaignDao,
) : SyncDelegate {
    override val entity = SyncEntity.COMM_CAMPAIGN
    override val pushDependencies = listOf(SyncEntity.COMM_TEMPLATE)

    override suspend fun pullFromServer(): SyncResult = runCatching {
        var page = 0; var applied = 0
        do {
            val resp = api.pullCampaigns(page, PAGE, null)
            if (resp.error != null) throw Exception(resp.error?.message ?: "Network error")
            val batch = resp.data?.content ?: emptyList()
            for (dto in batch) {
                val existing = dao.getByUid(dto.uid)
                if (existing != null && !existing.synced) continue
                if (!dto.active) { if (existing != null) dao.hardDelete(dto.uid); continue }
                dao.upsert(dto.toEntity(synced = true)); applied++
            }
            page++
            val hasNext = resp.data?.hasNext == true
        } while (hasNext && applied < MAX)
        applied
    }.toResult("CommCampaignSyncDelegate")

    override suspend fun pushPendingToServer(): SyncResult = runCatching {
        val unsynced = dao.unsynced()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0
        for (batch in unsynced.chunked(PAGE)) {
            val resp = api.pushCampaigns(batch.map { it.toDto() })
            if (resp.data != null && resp.error == null) {
                for (dto in resp.data ?: emptyList()) {
                    if (!dto.active) dao.hardDelete(dto.uid) else dao.upsert(dto.toEntity(synced = true))
                }
                synced += batch.size
            } else throw Exception(resp.error?.message ?: "Campaign push failed")
        }
        synced
    }.toResult("CommCampaignSyncDelegate")

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.COMM_PREFERENCE)
class CommPreferenceSyncDelegate(
    private val api: CommunicationSyncApi,
    private val dao: CommPreferenceDao,
) : SyncDelegate {
    override val entity = SyncEntity.COMM_PREFERENCE

    override suspend fun pullFromServer(): SyncResult = runCatching {
        var page = 0; var applied = 0
        do {
            val resp = api.pullPreferences(page, PAGE, null)
            if (resp.error != null) throw Exception(resp.error?.message ?: "Network error")
            val batch = resp.data?.content ?: emptyList()
            for (dto in batch) {
                val existing = dao.getByUid(dto.uid)
                if (existing != null && !existing.synced) continue
                if (!dto.active) { if (existing != null) dao.hardDelete(dto.uid); continue }
                dao.upsert(dto.toEntity(synced = true)); applied++
            }
            page++
            val hasNext = resp.data?.hasNext == true
        } while (hasNext && applied < MAX)
        applied
    }.toResult("CommPreferenceSyncDelegate")

    override suspend fun pushPendingToServer(): SyncResult = runCatching {
        val unsynced = dao.unsynced()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0
        for (batch in unsynced.chunked(PAGE)) {
            val resp = api.pushPreferences(batch.map { it.toDto() })
            if (resp.data != null && resp.error == null) {
                for (row in batch) { if (!row.active) dao.hardDelete(row.uid) else dao.markSynced(row.uid) }
                synced += batch.size
            } else throw Exception(resp.error?.message ?: "Preference push failed")
        }
        synced
    }.toResult("CommPreferenceSyncDelegate")

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}

/** Logs are server-authored — pull-only. Push is a no-op (nothing is ever dirty locally). */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.COMM_LOG)
class CommLogSyncDelegate(
    private val api: CommunicationSyncApi,
    private val dao: CommLogDao,
) : SyncDelegate {
    override val entity = SyncEntity.COMM_LOG

    override suspend fun pullFromServer(): SyncResult = runCatching {
        var page = 0; var applied = 0
        do {
            val resp = api.pullLogs(page, PAGE, null)
            if (resp.error != null) throw Exception(resp.error?.message ?: "Network error")
            val batch = resp.data?.content ?: emptyList()
            for (dto in batch) {
                if (!dto.active) dao.hardDelete(dto.uid) else dao.upsert(dto.toEntity())
                applied++
            }
            page++
            val hasNext = resp.data?.hasNext == true
        } while (hasNext && applied < MAX)
        applied
    }.toResult("CommLogSyncDelegate")

    override suspend fun pushPendingToServer(): SyncResult = SyncResult.Success(0)

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult = pullFromServer()
}

private fun Result<Int>.toResult(tag: String): SyncResult = fold(
    onSuccess = { SyncResult.Success(it) },
    onFailure = { CommunicationLogger.e(tag, "sync failed", it); SyncResult.Failure(it) },
)
