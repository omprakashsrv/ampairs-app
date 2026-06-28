package com.ampairs.communication.data.repository

import com.ampairs.communication.data.api.BindingDto
import com.ampairs.communication.data.api.CampaignDto
import com.ampairs.communication.data.api.PreferenceDto
import com.ampairs.communication.data.api.ScheduleDto
import com.ampairs.communication.data.api.TemplateAggregateDto
import com.ampairs.communication.data.api.toDto
import com.ampairs.communication.data.api.toEntity
import com.ampairs.communication.data.db.CommBindingDao
import com.ampairs.communication.data.db.CommCampaignDao
import com.ampairs.communication.data.db.CommLogDao
import com.ampairs.communication.data.db.CommLogEntity
import com.ampairs.communication.data.db.CommPreferenceDao
import com.ampairs.communication.data.db.CommScheduleDao
import com.ampairs.communication.data.db.CommTemplateDao
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Local-only message-template store (offline-first). Writes Room with `synced = false` and flags
 * `PENDING_PUSH`; all server traffic lives in [com.ampairs.communication.sync.CommTemplateSyncDelegate].
 */
@Inject
class TemplateRepository(
    private val dao: CommTemplateDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeAll(): Flow<List<TemplateAggregateDto>> =
        dao.observeAllActive().map { rows -> rows.map { it.toDto() } }

    suspend fun get(uid: String): TemplateAggregateDto? = dao.getByUid(uid)?.toDto()

    suspend fun save(template: TemplateAggregateDto) {
        require(template.uid.isNotBlank()) { "Template UID must be set by the ViewModel" }
        dao.upsert(template.toEntity(synced = false))
        markPending()
    }

    suspend fun delete(uid: String) {
        val existing = dao.getByUid(uid) ?: return
        dao.upsert(existing.copy(active = false, synced = false))
        markPending()
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.COMM_TEMPLATE, Clock.System.now().toEpochMilliseconds())
}

@Inject
class BindingRepository(
    private val dao: CommBindingDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeAll(): Flow<List<BindingDto>> = dao.observeAllActive().map { rows -> rows.map { it.toDto() } }

    suspend fun save(binding: BindingDto) {
        require(binding.uid.isNotBlank()) { "Binding UID must be set by the ViewModel" }
        dao.upsert(binding.toEntity(synced = false))
        markPending()
    }

    suspend fun delete(uid: String) {
        val existing = dao.getByUid(uid) ?: return
        dao.upsert(existing.copy(active = false, synced = false))
        markPending()
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.COMM_BINDING, Clock.System.now().toEpochMilliseconds())
}

@Inject
class ScheduleRepository(
    private val dao: CommScheduleDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeAll(): Flow<List<ScheduleDto>> = dao.observeAllActive().map { rows -> rows.map { it.toDto() } }

    suspend fun get(uid: String): ScheduleDto? = dao.getByUid(uid)?.toDto()

    suspend fun save(schedule: ScheduleDto) {
        require(schedule.uid.isNotBlank()) { "Schedule UID must be set by the ViewModel" }
        dao.upsert(schedule.toEntity(synced = false))
        markPending()
    }

    suspend fun setPaused(uid: String, paused: Boolean) {
        val existing = dao.getByUid(uid) ?: return
        dao.upsert(existing.copy(paused = paused, synced = false))
        markPending()
    }

    suspend fun delete(uid: String) {
        val existing = dao.getByUid(uid) ?: return
        dao.upsert(existing.copy(active = false, synced = false))
        markPending()
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.COMM_SCHEDULE, Clock.System.now().toEpochMilliseconds())
}

@Inject
class CampaignRepository(
    private val dao: CommCampaignDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeAll(): Flow<List<CampaignDto>> = dao.observeAllActive().map { rows -> rows.map { it.toDto() } }

    suspend fun get(uid: String): CampaignDto? = dao.getByUid(uid)?.toDto()

    suspend fun save(campaign: CampaignDto) {
        require(campaign.uid.isNotBlank()) { "Campaign UID must be set by the ViewModel" }
        dao.upsert(campaign.toEntity(synced = false))
        markPending()
    }

    suspend fun delete(uid: String) {
        val existing = dao.getByUid(uid) ?: return
        dao.upsert(existing.copy(active = false, synced = false))
        markPending()
    }

    /** Apply a server response from a lifecycle action (start/pause/resume) as already-synced. */
    suspend fun applyServer(campaign: CampaignDto) {
        dao.upsert(campaign.toEntity(synced = true))
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.COMM_CAMPAIGN, Clock.System.now().toEpochMilliseconds())
}

@Inject
class PreferenceRepository(
    private val dao: CommPreferenceDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeAll(): Flow<List<PreferenceDto>> = dao.observeAllActive().map { rows -> rows.map { it.toDto() } }

    suspend fun save(preference: PreferenceDto) {
        require(preference.uid.isNotBlank()) { "Preference UID must be set by the ViewModel" }
        dao.upsert(preference.toEntity(synced = false))
        markPending()
    }

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.COMM_PREFERENCE, Clock.System.now().toEpochMilliseconds())
}

/** Pull-only delivery-log cache — read-only on the device (server-authored). */
@Inject
class LogRepository(
    private val dao: CommLogDao,
) {
    fun observeRecent(): Flow<List<CommLogEntity>> = dao.observeRecent()
}
