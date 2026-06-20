package com.ampairs.printing.data.repository

import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.data.db.PrinterDao
import com.ampairs.printing.data.db.PrintRoutingDao
import com.ampairs.printing.data.db.PrintRoutingEntity
import com.ampairs.printing.data.db.toEntity
import com.ampairs.printing.data.db.toProfile
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Device-local printer configuration and document routing. NOT synced (FR-008) — printers are
 * physical to this device + workspace, so no [com.ampairs.sync.db.SyncStateDao] / SyncDelegate.
 */
@Inject
class PrinterRepository(
    private val printerDao: PrinterDao,
    private val routingDao: PrintRoutingDao,
) {
    fun observePrinters(): Flow<List<PrinterProfile>> =
        printerDao.observeAll().map { rows -> rows.map { it.toProfile() } }

    suspend fun getProfile(id: String): PrinterProfile? = printerDao.getById(id)?.toProfile()

    suspend fun savePrinter(profile: PrinterProfile) = printerDao.upsert(profile.toEntity())

    suspend fun removePrinter(id: String) = printerDao.deactivate(id)

    /** Set the default printer for a document type (e.g. "invoice" -> counter thermal). */
    suspend fun setRoute(documentType: String, printerId: String) =
        routingDao.upsert(PrintRoutingEntity(documentType, printerId))

    /** Resolve the printer configured for a document type, if any. */
    suspend fun resolvePrinter(documentType: String): PrinterProfile? {
        val printerId = routingDao.getForType(documentType)?.printerId ?: return null
        return printerDao.getById(printerId)?.toProfile()
    }
}
