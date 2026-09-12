package com.ampairs.cbemployee.sync

import com.ampairs.cbemployee.data.api.CbEmployeeApi
import com.ampairs.cbemployee.data.db.dao.EmployeeDao
import com.ampairs.cbemployee.data.db.entity.toEmployee
import com.ampairs.cbemployee.data.db.entity.toEntity
import com.ampairs.cbemployee.domain.model.Employee
import com.ampairs.cbemployee.util.CbEmployeeLogger
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sync.SyncDelegate
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEntityKey
import com.ampairs.sync.SyncResult
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/** Owns all Employee ↔ server traffic (canonical `/cb_employee/v1/employees/sync`). */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@SyncEntityKey(SyncEntity.CB_EMPLOYEE)
class EmployeeSyncDelegate(
    private val api: CbEmployeeApi,
    private val employeeDao: EmployeeDao,
    private val syncStateDao: SyncStateDao,
) : SyncDelegate {

    override val entity: SyncEntity = SyncEntity.CB_EMPLOYEE

    override suspend fun pullFromServer(): SyncResult =
        pull().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun pushPendingToServer(): SyncResult =
        pushPending().fold({ SyncResult.Success(it) }, { SyncResult.Failure(it) })

    override suspend fun handleBackendEvent(entityId: String, eventType: String): SyncResult =
        runCatching { refreshOne(entityId) }.fold({ SyncResult.Success(1) }, { SyncResult.Failure(it) })

    private suspend fun pushPending(): Result<Int> = runCatching {
        val unsynced = employeeDao.getUnsyncedEmployees()
        if (unsynced.isEmpty()) return@runCatching 0
        var synced = 0
        var failed = 0
        for (batch in unsynced.chunked(100)) {
            try {
                api.bulkUpdateEmployees(batch.map { it.toEmployee() })
                for (e in batch) {
                    if (!e.active) employeeDao.hardDeleteEmployee(e.id)
                    else employeeDao.insertEmployee(e.copy(synced = true))
                }
                synced += batch.size
            } catch (err: Exception) {
                CbEmployeeLogger.w("EmployeeSyncDelegate", "Batch push failed", err)
                failed += batch.size
            }
        }
        if (synced == 0 && failed > 0) throw Exception("$failed employee(s) failed to push") else synced
    }

    private suspend fun pull(batchSize: Int = 100): Result<Int> = runCatching {
        val lastSync = syncStateDao.getLastSyncedAtIso(SyncEntity.CB_EMPLOYEE) ?: ""
        var total = 0
        var page = 0
        var maxTime = ""
        do {
            val resp = api.getEmployeesSync(lastSync, page, batchSize, "updatedAt", "ASC")
            val rows = resp.content
            if (rows.isNotEmpty()) {
                val toInsert = rows.mapNotNull { server ->
                    val existing = employeeDao.getEmployeeById(server.uid)
                    when {
                        existing != null && !existing.synced -> null
                        !server.active -> { employeeDao.hardDeleteEmployee(server.uid); null }
                        else -> server.toEntity().copy(synced = true)
                    }
                }
                if (toInsert.isNotEmpty()) employeeDao.insertEmployees(toInsert)
                val batchMax = maxUpdatedAt(rows)
                if (batchMax > maxTime) maxTime = batchMax
                total += rows.size
            }
            page++
        } while (resp.hasNext && total < 10000)
        if (maxTime.isNotBlank()) syncStateDao.setLastSyncedAtIso(SyncEntity.CB_EMPLOYEE, maxTime)
        total
    }

    private suspend fun refreshOne(id: String) {
        val resp = api.getEmployeeById(id)
        if (resp.data != null && resp.error == null) {
            val existing = employeeDao.getEmployeeById(id)
            if (existing == null || existing.synced) employeeDao.insertEmployee(resp.data!!.toEntity().copy(synced = true))
        }
    }

    private fun maxUpdatedAt(rows: List<Employee>): String =
        rows.mapNotNull { it.updatedAt?.takeIf { ts -> ts.isNotBlank() } }.maxOrNull() ?: ""
}
