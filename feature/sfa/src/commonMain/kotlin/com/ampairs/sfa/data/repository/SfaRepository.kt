package com.ampairs.sfa.data.repository

import com.ampairs.sfa.data.db.dao.AttendanceDao
import com.ampairs.sfa.data.db.dao.BeatDao
import com.ampairs.sfa.data.db.dao.BeatOutletDao
import com.ampairs.sfa.data.db.dao.JourneyPlanDao
import com.ampairs.sfa.data.db.dao.PlannedVisitDao
import com.ampairs.sfa.data.db.dao.VisitDao
import com.ampairs.sfa.data.db.entity.toAttendance
import com.ampairs.sfa.data.db.entity.toBeat
import com.ampairs.sfa.data.db.entity.toBeatOutlet
import com.ampairs.sfa.data.db.entity.toEntity
import com.ampairs.sfa.data.db.entity.toJourneyPlan
import com.ampairs.sfa.data.db.entity.toPlannedVisit
import com.ampairs.sfa.data.db.entity.toVisit
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit
import com.ampairs.sfa.util.SfaLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for the SFA module. The [com.ampairs.sfa.data.api.SfaApi] is owned by the
 * sync delegates; writes here persist to Room as unsynced and flag the entity PENDING_PUSH so
 * CentralSyncService runs the automatic bulk push.
 */
@OptIn(ExperimentalTime::class)
@Inject
class SfaRepository(
    private val beatDao: BeatDao,
    private val beatOutletDao: BeatOutletDao,
    private val journeyPlanDao: JourneyPlanDao,
    private val plannedVisitDao: PlannedVisitDao,
    private val visitDao: VisitDao,
    private val attendanceDao: AttendanceDao,
    private val syncStateDao: SyncStateDao,
) {
    // ----- Beats -----
    fun observeBeats(): Flow<List<Beat>> = beatDao.getAllBeats().map { rows -> rows.map { it.toBeat() } }
    suspend fun getBeatById(id: String): Beat? = beatDao.getBeatById(id)?.toBeat()

    suspend fun saveBeat(beat: Beat): Result<Beat> = runCatching {
        require(beat.uid.isNotBlank()) { "Beat uid must be set by the ViewModel" }
        beatDao.insertBeat(beat.toEntity().copy(synced = false))
        markPending(SyncEntity.SFA_BEAT)
        beat
    }.onFailure { SfaLogger.e("SfaRepository", "saveBeat failed", it) }

    suspend fun deleteBeat(id: String): Result<Unit> = runCatching {
        val existing = beatDao.getBeatById(id)
        if (existing != null) {
            beatDao.insertBeat(existing.copy(active = false, synced = false))
            markPending(SyncEntity.SFA_BEAT)
        }
        Unit
    }.onFailure { SfaLogger.e("SfaRepository", "deleteBeat failed", it) }

    // ----- Visits -----
    fun observeVisits(): Flow<List<Visit>> = visitDao.getAllVisits().map { rows -> rows.map { it.toVisit() } }
    suspend fun getVisitById(id: String): Visit? = visitDao.getVisitById(id)?.toVisit()

    suspend fun saveVisit(visit: Visit): Result<Visit> = runCatching {
        require(visit.uid.isNotBlank()) { "Visit uid must be set by the ViewModel" }
        visitDao.insertVisit(visit.toEntity().copy(synced = false))
        markPending(SyncEntity.SFA_VISIT)
        visit
    }.onFailure { SfaLogger.e("SfaRepository", "saveVisit failed", it) }

    suspend fun deleteVisit(id: String): Result<Unit> = runCatching {
        val existing = visitDao.getVisitById(id)
        if (existing != null) {
            visitDao.insertVisit(existing.copy(active = false, synced = false))
            markPending(SyncEntity.SFA_VISIT)
        }
        Unit
    }.onFailure { SfaLogger.e("SfaRepository", "deleteVisit failed", it) }

    // ----- Attendance -----
    fun observeAttendance(): Flow<List<Attendance>> = attendanceDao.getAllAttendance().map { rows -> rows.map { it.toAttendance() } }
    suspend fun getAttendanceById(id: String): Attendance? = attendanceDao.getAttendanceById(id)?.toAttendance()

    suspend fun saveAttendance(attendance: Attendance): Result<Attendance> = runCatching {
        require(attendance.uid.isNotBlank()) { "Attendance uid must be set by the ViewModel" }
        attendanceDao.insertAttendance(attendance.toEntity().copy(synced = false))
        markPending(SyncEntity.SFA_ATTENDANCE)
        attendance
    }.onFailure { SfaLogger.e("SfaRepository", "saveAttendance failed", it) }

    // ----- BeatOutlets -----
    fun observeBeatOutlets(): Flow<List<BeatOutlet>> = beatOutletDao.getAllBeatOutlets().map { rows -> rows.map { it.toBeatOutlet() } }
    fun observeOutletsForBeat(beatUid: String): Flow<List<BeatOutlet>> = beatOutletDao.getOutletsForBeat(beatUid).map { rows -> rows.map { it.toBeatOutlet() } }

    suspend fun saveBeatOutlet(row: BeatOutlet): Result<BeatOutlet> = runCatching {
        require(row.uid.isNotBlank()) { "BeatOutlet uid must be set by the ViewModel" }
        beatOutletDao.insertBeatOutlet(row.toEntity().copy(synced = false))
        markPending(SyncEntity.SFA_BEAT_OUTLET)
        row
    }.onFailure { SfaLogger.e("SfaRepository", "saveBeatOutlet failed", it) }

    suspend fun deleteBeatOutlet(id: String): Result<Unit> = runCatching {
        val existing = beatOutletDao.getBeatOutletById(id)
        if (existing != null) {
            beatOutletDao.insertBeatOutlet(existing.copy(active = false, synced = false))
            markPending(SyncEntity.SFA_BEAT_OUTLET)
        }
        Unit
    }.onFailure { SfaLogger.e("SfaRepository", "deleteBeatOutlet failed", it) }

    // ----- JourneyPlans -----
    fun observeJourneyPlans(): Flow<List<JourneyPlan>> = journeyPlanDao.getAllJourneyPlans().map { rows -> rows.map { it.toJourneyPlan() } }

    suspend fun saveJourneyPlan(row: JourneyPlan): Result<JourneyPlan> = runCatching {
        require(row.uid.isNotBlank()) { "JourneyPlan uid must be set by the ViewModel" }
        journeyPlanDao.insertJourneyPlan(row.toEntity().copy(synced = false))
        markPending(SyncEntity.SFA_JOURNEY_PLAN)
        row
    }.onFailure { SfaLogger.e("SfaRepository", "saveJourneyPlan failed", it) }

    suspend fun deleteJourneyPlan(id: String): Result<Unit> = runCatching {
        val existing = journeyPlanDao.getJourneyPlanById(id)
        if (existing != null) {
            journeyPlanDao.insertJourneyPlan(existing.copy(active = false, synced = false))
            markPending(SyncEntity.SFA_JOURNEY_PLAN)
        }
        Unit
    }.onFailure { SfaLogger.e("SfaRepository", "deleteJourneyPlan failed", it) }

    // ----- PlannedVisits -----
    fun observePlannedVisits(): Flow<List<PlannedVisit>> = plannedVisitDao.getAllPlannedVisits().map { rows -> rows.map { it.toPlannedVisit() } }
    suspend fun getPlannedVisitById(id: String): PlannedVisit? = plannedVisitDao.getPlannedVisitById(id)?.toPlannedVisit()

    suspend fun savePlannedVisit(row: PlannedVisit): Result<PlannedVisit> = runCatching {
        require(row.uid.isNotBlank()) { "PlannedVisit uid must be set by the ViewModel" }
        plannedVisitDao.insertPlannedVisit(row.toEntity().copy(synced = false))
        markPending(SyncEntity.SFA_PLANNED_VISIT)
        row
    }.onFailure { SfaLogger.e("SfaRepository", "savePlannedVisit failed", it) }

    private suspend fun markPending(entity: SyncEntity) {
        syncStateDao.markPendingPush(entity, Clock.System.now().toEpochMilliseconds())
    }
}
