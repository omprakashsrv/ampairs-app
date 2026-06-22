package com.ampairs.inventory.data.repository

import androidx.paging.PagingSource
import com.ampairs.inventory.data.db.InventoryItemDao
import com.ampairs.inventory.data.db.InventoryTransactionDao
import com.ampairs.inventory.data.db.InventoryTransactionEntity
import com.ampairs.inventory.data.db.toEntity
import com.ampairs.inventory.data.db.toMovement
import com.ampairs.inventory.domain.InventoryMovement
import com.ampairs.inventory.util.InventoryConstants
import com.ampairs.inventory.util.InventoryLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only inventory movement (transaction) data access (spec 014). Movements are append-only:
 * a record persists to Room (synced = false), updates the local item's on-hand for immediate display,
 * and flags INVENTORY_TRANSACTION as PENDING_PUSH. The delegate owns the API; the server returns the
 * authoritative balance/stock on the next pull.
 */
@OptIn(ExperimentalTime::class)
@Inject
class InventoryTransactionRepository(
    private val transactionDao: InventoryTransactionDao,
    private val itemDao: InventoryItemDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeMovements(itemId: String): Flow<List<InventoryMovement>> =
        transactionDao.getMovementsByItem(itemId).map { list -> list.map { it.toMovement() } }

    fun pagingMovements(itemId: String): PagingSource<Int, InventoryTransactionEntity> =
        transactionDao.pagingMovementsByItem(itemId)

    /**
     * Record an append-only movement (manual adjustment / count). Caller (ViewModel) sets the uid and
     * a STOCK_IN/STOCK_OUT [InventoryMovement.transactionType] carrying the direction. Updates the
     * local item on-hand optimistically; the server recomputes the authoritative balance on pull.
     */
    suspend fun recordMovement(movement: InventoryMovement): Result<InventoryMovement> = try {
        require(movement.uid.isNotBlank()) { "Movement uid must be set by the ViewModel" }
        val signed = when (movement.transactionType) {
            InventoryConstants.TXN_TYPE_STOCK_IN -> movement.quantity
            InventoryConstants.TXN_TYPE_STOCK_OUT -> -movement.quantity
            else -> 0.0
        }
        val item = itemDao.getItemById(movement.inventoryItemId)
        val balanceAfter = if (item != null) {
            val newStock = item.currentStock + signed
            val newAvailable = (newStock - item.reservedStock).coerceAtLeast(0.0)
            // Local optimistic update for immediate display; not marked pending (stock is server-derived).
            itemDao.insertItem(item.copy(currentStock = newStock, availableStock = newAvailable))
            newStock
        } else {
            movement.balanceAfter
        }
        transactionDao.insertMovement(movement.copy(balanceAfter = balanceAfter).toEntity().copy(synced = false))
        markPending()
        Result.success(movement)
    } catch (e: Exception) {
        InventoryLogger.e("InventoryTransactionRepository", "recordMovement failed", e)
        Result.failure(e)
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.INVENTORY_TRANSACTION, Clock.System.now().toEpochMilliseconds())
    }
}
