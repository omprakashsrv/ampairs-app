package com.ampairs.cbmaintenance.data.repository

import com.ampairs.cbmaintenance.data.db.dao.PmScheduleDao
import com.ampairs.cbmaintenance.data.db.entity.toEntity
import com.ampairs.cbmaintenance.data.db.entity.toPmSchedule
import com.ampairs.cbmaintenance.domain.model.PmSchedule
import com.ampairs.cbmaintenance.util.CbMaintenanceLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Local-only data access for PM schedules. Sync is owned by PmScheduleSyncDelegate. */
@OptIn(ExperimentalTime::class)
@Inject
class PmScheduleRepository(
    private val dao: PmScheduleDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeSchedules(): Flow<List<PmSchedule>> =
        dao.getAllSchedules().map { list -> list.map { it.toPmSchedule() } }

    suspend fun getSchedule(id: String): PmSchedule? = dao.getScheduleById(id)?.toPmSchedule()

    suspend fun activeSchedules(): List<PmSchedule> = dao.getAllSchedules().first().map { it.toPmSchedule() }

    suspend fun saveSchedule(schedule: PmSchedule): Result<PmSchedule> = runCatching {
        require(schedule.uid.isNotBlank()) { "UID must be set by ViewModel" }
        dao.insertSchedule(schedule.toEntity().copy(synced = false))
        markPending()
        schedule
    }.onFailure { CbMaintenanceLogger.e("PmScheduleRepository", "saveSchedule failed", it) }

    suspend fun deleteSchedule(id: String): Result<Unit> = runCatching {
        dao.getScheduleById(id)?.let {
            dao.insertSchedule(it.copy(active = false, synced = false))
            markPending()
        }
        Unit
    }.onFailure { CbMaintenanceLogger.e("PmScheduleRepository", "deleteSchedule failed", it) }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.CB_PM_SCHEDULE, Clock.System.now().toEpochMilliseconds())
}
