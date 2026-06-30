package com.ampairs.sfa.sync

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sfa.data.api.SfaApi
import com.ampairs.sfa.data.db.dao.VisitSurveyResponseDao
import com.ampairs.sfa.data.db.entity.toVisitSurveyResponse
import com.ampairs.sfa.data.db.entity.toEntity
import com.ampairs.sfa.util.SfaConstants
import com.ampairs.sfa.util.SfaLogger
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.SFA_VISIT_SURVEY)
class VisitSurveyResponseSyncDelegate(
    private val api: SfaApi,
    private val dao: VisitSurveyResponseDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.SFA_VISIT_SURVEY

    override suspend fun pullFromServer(): SyncResult =
        pull().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        push().fold(onSuccess = { SyncResult.Success(it) }, onFailure = { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        pullFromServer()

    private suspend fun push(): Result<Int> = runCatching {
        val unsynced = dao.getUnsyncedVisitSurveyResponses()
        if (unsynced.isEmpty()) return Result.success(0)
        var synced = 0
        var failed = 0
        for (batch in unsynced.chunked(SfaConstants.SYNC_BATCH_SIZE)) {
            try {
                api.bulkUpdateVisitSurveys(batch.map { it.toVisitSurveyResponse() })
                for (row in batch) {
                    if (!row.active) dao.hardDeleteVisitSurveyResponse(row.id) else dao.insertVisitSurveyResponse(row.copy(synced = true))
                }
                synced += batch.size
            } catch (e: Exception) {
                SfaLogger.w("VisitSurveyResponseSyncDelegate", "batch push failed", e)
                failed += batch.size
            }
        }
        if (synced == 0 && failed > 0) throw Exception("$failed VisitSurveyResponse row(s) failed to push — will retry")
        synced
    }

    private suspend fun pull(): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.SFA_VISIT_SURVEY) ?: ""
        var total = 0
        var page = 0
        var maxTime = ""
        do {
            val resp = api.getVisitSurveysSync(lastSync, page, SfaConstants.SYNC_BATCH_SIZE, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val entities = rows.mapNotNull { server ->
                    val existing = dao.getVisitSurveyResponseById(server.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !server.active -> { dao.hardDeleteVisitSurveyResponse(server.uid); null }
                        else -> server.toEntity().copy(synced = true)
                    }
                }
                if (entities.isNotEmpty()) dao.insertVisitSurveyResponses(entities)
                val batchMax = rows.mapNotNull { it.updatedAt?.takeIf(String::isNotBlank) }.maxOrNull() ?: ""
                if (batchMax > maxTime) maxTime = batchMax
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < SfaConstants.MAX_SYNC_RECORDS)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.SFA_VISIT_SURVEY, maxTime)
        total
    }.onFailure { SfaLogger.e("VisitSurveyResponseSyncDelegate", "pull failed", it) }
}
