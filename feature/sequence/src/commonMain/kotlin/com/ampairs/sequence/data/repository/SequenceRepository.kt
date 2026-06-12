package com.ampairs.sequence.data.repository

import com.ampairs.sequence.data.db.dao.SequenceDefinitionDao
import com.ampairs.sequence.data.db.entity.toDefinition
import com.ampairs.sequence.data.db.entity.toEntity
import com.ampairs.sequence.domain.model.SequenceDefinition
import com.ampairs.sequence.util.SequenceLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for sequence definitions. The [com.ampairs.sequence.data.api.SequenceApi]
 * is owned by the sync layer ([com.ampairs.sequence.sync.SequenceSyncDelegate]); writes here
 * persist to Room as unsynced and mark SEQUENCE as PENDING_PUSH so CentralSyncService runs the
 * automatic bulk push.
 */
@OptIn(ExperimentalTime::class)
@Inject
class SequenceRepository(
    private val definitionDao: SequenceDefinitionDao,
    private val syncStateDao: SyncStateDao,
) {

    fun observeActiveDefinitions(): Flow<List<SequenceDefinition>> =
        definitionDao.observeActiveDefinitions().map { entities -> entities.map { it.toDefinition() } }

    suspend fun getDefinitionByUid(uid: String): SequenceDefinition? =
        definitionDao.getByUid(uid)?.toDefinition()

    suspend fun getActiveWorkspaceDefinition(entityType: String): SequenceDefinition? =
        definitionDao.getActiveWorkspaceDefinition(entityType)?.toDefinition()

    /** Offline-first create/update: persist locally as unsynced and flag for automatic bulk push. */
    suspend fun saveDefinition(definition: SequenceDefinition): Result<SequenceDefinition> {
        return try {
            require(definition.uid.isNotBlank()) { "Definition UID must be set by the caller" }
            definitionDao.insertDefinition(definition.toEntity().copy(synced = false))
            markPending()
            Result.success(definition)
        } catch (e: Exception) {
            SequenceLogger.e("SequenceRepository", "Failed to save sequence definition", e)
            Result.failure(e)
        }
    }

    /** Offline-first deactivate: mark inactive + unsynced so the push carries it in-band. */
    suspend fun deactivateDefinition(uid: String): Result<Unit> {
        return try {
            val existing = definitionDao.getByUid(uid)
            if (existing != null) {
                definitionDao.insertDefinition(existing.copy(active = false, synced = false))
                markPending()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            SequenceLogger.e("SequenceRepository", "Failed to deactivate sequence definition", e)
            Result.failure(e)
        }
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.SEQUENCE, Clock.System.now().toEpochMilliseconds())
    }
}
