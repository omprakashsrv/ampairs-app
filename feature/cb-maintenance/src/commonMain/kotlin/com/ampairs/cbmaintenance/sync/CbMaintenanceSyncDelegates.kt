package com.ampairs.cbmaintenance.sync

import com.ampairs.cbmaintenance.data.api.CbMaintenanceApi
import com.ampairs.cbmaintenance.data.db.dao.AssetCategoryAliasDao
import com.ampairs.cbmaintenance.data.db.dao.PmEntryDao
import com.ampairs.cbmaintenance.data.db.dao.PmScheduleDao
import com.ampairs.cbmaintenance.data.db.dao.TicketBucketDao
import com.ampairs.cbmaintenance.data.db.dao.TicketDao
import com.ampairs.cbmaintenance.data.db.entity.toAlias
import com.ampairs.cbmaintenance.data.db.entity.toEntity
import com.ampairs.cbmaintenance.data.db.entity.toPmEntry
import com.ampairs.cbmaintenance.data.db.entity.toPmSchedule
import com.ampairs.cbmaintenance.data.db.entity.toTicket
import com.ampairs.cbmaintenance.util.CbMaintenanceLogger
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

private const val CAP = 10000

private fun maxTs(list: List<String?>): String =
    list.mapNotNull { it?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""

// --- PM schedules -------------------------------------------------------------------------------
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CB_PM_SCHEDULE)
class PmScheduleSyncDelegate(
    private val api: CbMaintenanceApi,
    private val dao: PmScheduleDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {
    override val entity = SyncEntity.CB_PM_SCHEDULE

    override suspend fun pullFromServer() = pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun pushPendingToServer() = push().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun handleBackendEvent(entityId: String, eventType: String) = pullFromServer()

    private suspend fun push(): Result<Int> = runCatching {
        val unsynced = dao.getUnsyncedSchedules()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0; var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.bulkUpdatePmSchedules(batch.map { it.toPmSchedule() })
                for (e in batch) if (!e.active) dao.hardDeleteSchedule(e.id) else dao.insertSchedule(e.copy(synced = true))
                synced += batch.size
            } catch (err: Exception) { CbMaintenanceLogger.w("PmScheduleSyncDelegate", "push batch failed", err); failed += batch.size }
        }
        if (synced == 0 && failed > 0) throw Exception("$failed schedule(s) failed to push") else synced
    }

    private suspend fun pull(): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CB_PM_SCHEDULE) ?: ""
        var total = 0; var page = 0; var maxTime = ""
        do {
            val resp = api.getPmSchedulesSync(lastSync, page, 100, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val toInsert = rows.mapNotNull { s ->
                    val existing = dao.getScheduleById(s.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !s.active -> { dao.hardDeleteSchedule(s.uid); null }
                        else -> s.toEntity().copy(synced = true)
                    }
                }
                if (toInsert.isNotEmpty()) dao.insertSchedules(toInsert)
                val bm = maxTs(rows.map { it.updatedAt }); if (bm > maxTime) maxTime = bm
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < CAP)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.CB_PM_SCHEDULE, maxTime)
        total
    }
}

// --- Asset-category aliases ---------------------------------------------------------------------
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CB_ASSET_ALIAS)
class AssetCategoryAliasSyncDelegate(
    private val api: CbMaintenanceApi,
    private val dao: AssetCategoryAliasDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {
    override val entity = SyncEntity.CB_ASSET_ALIAS

    override suspend fun pullFromServer() = pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun pushPendingToServer() = push().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun handleBackendEvent(entityId: String, eventType: String) = pullFromServer()

    private suspend fun push(): Result<Int> = runCatching {
        val unsynced = dao.getUnsyncedAliases()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0; var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.bulkUpdateAliases(batch.map { it.toAlias() })
                for (e in batch) if (!e.active) dao.hardDeleteAlias(e.id) else dao.insertAlias(e.copy(synced = true))
                synced += batch.size
            } catch (err: Exception) { CbMaintenanceLogger.w("AssetAliasSyncDelegate", "push batch failed", err); failed += batch.size }
        }
        if (synced == 0 && failed > 0) throw Exception("$failed alias(es) failed to push") else synced
    }

    private suspend fun pull(): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CB_ASSET_ALIAS) ?: ""
        var total = 0; var page = 0; var maxTime = ""
        do {
            val resp = api.getAliasesSync(lastSync, page, 100, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val toInsert = rows.mapNotNull { a ->
                    val existing = dao.getAliasById(a.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !a.active -> { dao.hardDeleteAlias(a.uid); null }
                        else -> a.toEntity().copy(synced = true)
                    }
                }
                if (toInsert.isNotEmpty()) dao.insertAliases(toInsert)
                val bm = maxTs(rows.map { it.updatedAt }); if (bm > maxTime) maxTime = bm
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < CAP)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.CB_ASSET_ALIAS, maxTime)
        total
    }
}

// --- Ticket buckets (global classification catalog — pull-only) ---------------------------------
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CB_TICKET_BUCKET)
class TicketBucketSyncDelegate(
    private val api: CbMaintenanceApi,
    private val dao: TicketBucketDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {
    override val entity = SyncEntity.CB_TICKET_BUCKET

    override suspend fun pullFromServer() = pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    // Reference data — the app never edits it, so there is nothing to push.
    override suspend fun pushPendingToServer() = SyncResult.Success(0)
    override suspend fun handleBackendEvent(entityId: String, eventType: String) = pullFromServer()

    private suspend fun pull(): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CB_TICKET_BUCKET) ?: ""
        var total = 0; var page = 0; var maxTime = ""
        do {
            val resp = api.getTicketBucketsSync(lastSync, page, 100, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val toInsert = rows.mapNotNull { b ->
                    if (!b.active) { dao.hardDeleteBucket(b.uid); null } else b.toEntity()
                }
                if (toInsert.isNotEmpty()) dao.insertBuckets(toInsert)
                val bm = maxTs(rows.map { it.updatedAt }); if (bm > maxTime) maxTime = bm
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < CAP)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.CB_TICKET_BUCKET, maxTime)
        total
    }
}

// --- PM entries (depend on employees + stores + schedules) --------------------------------------
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CB_PM_ENTRY)
class PmEntrySyncDelegate(
    private val api: CbMaintenanceApi,
    private val dao: PmEntryDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {
    override val entity = SyncEntity.CB_PM_ENTRY
    override val pushDependencies = listOf(SyncEntity.CB_EMPLOYEE, SyncEntity.CB_STORE, SyncEntity.CB_PM_SCHEDULE)

    override suspend fun pullFromServer() = pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun pushPendingToServer() = push().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun handleBackendEvent(entityId: String, eventType: String) = pullFromServer()

    private suspend fun push(): Result<Int> = runCatching {
        val unsynced = dao.getUnsyncedEntries()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0; var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.bulkUpdatePmEntries(batch.map { it.toPmEntry() })
                for (e in batch) if (!e.active) dao.hardDeleteEntry(e.id) else dao.insertEntry(e.copy(synced = true))
                synced += batch.size
            } catch (err: Exception) { CbMaintenanceLogger.w("PmEntrySyncDelegate", "push batch failed", err); failed += batch.size }
        }
        // PM entries are a dependency of tickets; report ANY failure so tickets defer.
        if (failed > 0) throw Exception("$failed PM entry(ies) failed to push") else synced
    }

    private suspend fun pull(): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CB_PM_ENTRY) ?: ""
        var total = 0; var page = 0; var maxTime = ""
        do {
            val resp = api.getPmEntriesSync(lastSync, page, 100, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val toInsert = rows.mapNotNull { p ->
                    val existing = dao.getEntryById(p.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !p.active -> { dao.hardDeleteEntry(p.uid); null }
                        else -> p.toEntity().copy(synced = true)
                    }
                }
                if (toInsert.isNotEmpty()) dao.insertEntries(toInsert)
                val bm = maxTs(rows.map { it.updatedAt }); if (bm > maxTime) maxTime = bm
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < CAP)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.CB_PM_ENTRY, maxTime)
        total
    }
}

// --- Tickets (depend on employees + stores + PM entries) ----------------------------------------
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CB_TICKET)
class TicketSyncDelegate(
    private val api: CbMaintenanceApi,
    private val dao: TicketDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {
    override val entity = SyncEntity.CB_TICKET
    override val pushDependencies = listOf(SyncEntity.CB_EMPLOYEE, SyncEntity.CB_STORE, SyncEntity.CB_PM_ENTRY)

    override suspend fun pullFromServer() = pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun pushPendingToServer() = push().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })
    override suspend fun handleBackendEvent(entityId: String, eventType: String) = pullFromServer()

    private suspend fun push(): Result<Int> = runCatching {
        val unsynced = dao.getUnsyncedTickets()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0; var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.bulkUpdateTickets(batch.map { it.toTicket() })
                for (e in batch) if (!e.active) dao.hardDeleteTicket(e.id) else dao.insertTicket(e.copy(synced = true))
                synced += batch.size
            } catch (err: Exception) { CbMaintenanceLogger.w("TicketSyncDelegate", "push batch failed", err); failed += batch.size }
        }
        if (synced == 0 && failed > 0) throw Exception("$failed ticket(s) failed to push") else synced
    }

    private suspend fun pull(): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CB_TICKET) ?: ""
        var total = 0; var page = 0; var maxTime = ""
        do {
            val resp = api.getTicketsSync(lastSync, page, 100, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val toInsert = rows.mapNotNull { t ->
                    val existing = dao.getTicketById(t.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !t.active -> { dao.hardDeleteTicket(t.uid); null }
                        else -> t.toEntity().copy(synced = true)
                    }
                }
                if (toInsert.isNotEmpty()) dao.insertTickets(toInsert)
                val bm = maxTs(rows.map { it.updatedAt }); if (bm > maxTime) maxTime = bm
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < CAP)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.CB_TICKET, maxTime)
        total
    }
}
