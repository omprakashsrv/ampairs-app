package com.ampairs.cbmaintenance.data.repository

import com.ampairs.cbmaintenance.data.db.dao.TicketDao
import com.ampairs.cbmaintenance.data.db.entity.toEntity
import com.ampairs.cbmaintenance.data.db.entity.toTicket
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.cbmaintenance.util.CbMaintenanceLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for tickets. Raising / reassigning is offline-first (synced = false) and
 * pushed by TicketSyncDelegate; the server denormalizes the zone and auto-assigns on upsert.
 */
@OptIn(ExperimentalTime::class)
@Inject
class TicketRepository(
    private val dao: TicketDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeTickets(): Flow<List<Ticket>> =
        dao.getAllTickets().map { list -> list.map { it.toTicket() } }

    suspend fun getTicket(id: String): Ticket? = dao.getTicketById(id)?.toTicket()

    /** Raise a reactive ticket. `zonalOfficeId` is left blank — the server denormalizes it. */
    suspend fun raiseTicket(ticket: Ticket): Result<Ticket> = runCatching {
        require(ticket.uid.isNotBlank()) { "UID must be set by ViewModel" }
        dao.insertTicket(ticket.copy(raisedAt = ticket.raisedAt ?: nowIso()).toEntity().copy(synced = false))
        markPending()
        ticket
    }.onFailure { CbMaintenanceLogger.e("TicketRepository", "raiseTicket failed", it) }

    /**
     * Soft-delete a ticket raised by mistake. Offline-first: `active = false, synced = false` so
     * TicketSyncDelegate pushes the deletion and hard-deletes the row locally once the server confirms.
     */
    suspend fun deleteTicket(id: String): Result<Unit> = runCatching {
        dao.getTicketById(id)?.let {
            dao.insertTicket(it.copy(active = false, synced = false))
            markPending()
        }
        Unit
    }.onFailure { CbMaintenanceLogger.e("TicketRepository", "deleteTicket failed", it) }

    suspend fun reassignTicket(id: String, newAssigneeId: String): Result<Unit> = runCatching {
        dao.getTicketById(id)?.let {
            dao.insertTicket(it.copy(assignedToEmployeeId = newAssigneeId, synced = false))
            markPending()
        }
        Unit
    }.onFailure { CbMaintenanceLogger.e("TicketRepository", "reassignTicket failed", it) }

    private fun nowIso(): String = Clock.System.now().toString()

    private suspend fun markPending() =
        syncStateDao.markPendingPush(SyncEntity.CB_TICKET, Clock.System.now().toEpochMilliseconds())
}
