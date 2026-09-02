package com.ampairs.cbmaintenance.data.repository

import com.ampairs.cbmaintenance.data.api.CbMaintenanceApi
import com.ampairs.cbmaintenance.data.db.dao.PmEntryDao
import com.ampairs.cbmaintenance.data.db.entity.toEntity
import com.ampairs.cbmaintenance.data.db.entity.toPmEntry
import com.ampairs.cbmaintenance.domain.model.ChecklistItemResult
import com.ampairs.cbmaintenance.domain.model.PmEntry
import com.ampairs.cbmaintenance.util.CbMaintenanceLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for PM entries. Completion / reassignment / assist are offline-first
 * field updates written here (synced = false) and pushed by PmEntrySyncDelegate; the server's
 * bulk-upsert spawns tickets from any failed checklist item (module plan §6).
 */
@OptIn(ExperimentalTime::class)
@Inject
class PmEntryRepository(
    private val dao: PmEntryDao,
    private val syncStateDao: SyncStateDao,
    // Non-sync, UI-invoked ops call only (allowed exception) — completion/reassign stay local + delegate.
    private val api: CbMaintenanceApi,
) {
    fun observeOpenEntries(): Flow<List<PmEntry>> =
        dao.getOpenEntries().map { list -> list.map { it.toPmEntry() } }

    fun observeAllEntries(): Flow<List<PmEntry>> =
        dao.getAllEntries().map { list -> list.map { it.toPmEntry() } }

    suspend fun getEntry(id: String): PmEntry? = dao.getEntryById(id)?.toPmEntry()

    /** Mark a PM done with its checklist results. Failed items spawn tickets server-side on push. */
    suspend fun completeEntry(
        id: String,
        checklistResult: List<ChecklistItemResult>,
        completedByEmployeeId: String? = null,
    ): Result<Unit> = runCatching {
        val existing = dao.getEntryById(id) ?: return@runCatching Unit
        val updated = existing.toPmEntry().copy(
            status = "DONE",
            completedAt = nowIso(),
            completedByEmployeeId = completedByEmployeeId ?: existing.completedByEmployeeId,
            assignedToEmployeeId = existing.assignedToEmployeeId ?: completedByEmployeeId,
            checklistResult = checklistResult,
        )
        dao.insertEntry(updated.toEntity().copy(synced = false))
        markPending()
        Unit
    }.onFailure { CbMaintenanceLogger.e("PmEntryRepository", "completeEntry failed", it) }

    suspend fun reassignEntry(id: String, newAssigneeId: String): Result<Unit> = runCatching {
        dao.getEntryById(id)?.let {
            dao.insertEntry(it.copy(assignedToEmployeeId = newAssigneeId, synced = false))
            markPending()
        }
        Unit
    }.onFailure { CbMaintenanceLogger.e("PmEntryRepository", "reassignEntry failed", it) }

    /**
     * Ask the server to generate due PM entries now (same work as the nightly job). The new entries
     * arrive on the next pull; the ViewModel triggers one. Returns the count generated.
     */
    suspend fun generateNow(): Result<Int> =
        runCatching { api.generatePmEntries() }
            .onFailure { CbMaintenanceLogger.e("PmEntryRepository", "generateNow failed", it) }

    private fun nowIso(): String = Clock.System.now().toString()

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.CB_PM_ENTRY, Clock.System.now().toEpochMilliseconds())
}
