package com.ampairs.inventory.data.repository

import com.ampairs.inventory.data.db.InventoryItemDao
import com.ampairs.inventory.data.db.toEntity
import com.ampairs.inventory.data.db.toInventoryItem
import com.ampairs.inventory.domain.InventoryItem
import com.ampairs.inventory.util.InventoryLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only inventory item data access (spec 014). Writes persist to Room (synced = false) and flag
 * INVENTORY as PENDING_PUSH; the [com.ampairs.inventory.sync.InventoryItemSyncDelegate] owns all API
 * traffic. On-hand stock changes ride the transactions feed — item writes carry metadata/pricing/
 * reorder/active only.
 */
@OptIn(ExperimentalTime::class)
@Inject
class InventoryItemRepository(
    private val itemDao: InventoryItemDao,
    private val syncStateDao: SyncStateDao,
) {
    fun observeItems(): Flow<List<InventoryItem>> =
        itemDao.getAllItems().map { list -> list.map { it.toInventoryItem() } }

    fun searchItems(query: String): Flow<List<InventoryItem>> =
        if (query.isBlank()) observeItems()
        else itemDao.searchItems(query).map { list -> list.map { it.toInventoryItem() } }

    fun observeLowStock(): Flow<List<InventoryItem>> =
        itemDao.getLowStockItems().map { list -> list.map { it.toInventoryItem() } }

    fun observeOutOfStock(): Flow<List<InventoryItem>> =
        itemDao.getOutOfStockItems().map { list -> list.map { it.toInventoryItem() } }

    fun observeInactiveItems(): Flow<List<InventoryItem>> =
        itemDao.getInactiveItems().map { list -> list.map { it.toInventoryItem() } }

    fun observeAllItemsIncludingInactive(): Flow<List<InventoryItem>> =
        itemDao.getAllItemsIncludingInactive().map { list -> list.map { it.toInventoryItem() } }

    fun observeTotalStockValue(): Flow<Double> = itemDao.getTotalStockValue()

    fun observeActiveItemCount(): Flow<Int> = itemDao.getActiveItemCount()

    suspend fun getItem(id: String): InventoryItem? = itemDao.getItemById(id)?.toInventoryItem()

    fun observeItem(id: String): Flow<InventoryItem?> =
        itemDao.observeItemById(id).map { it?.toInventoryItem() }

    /** Offline-first create/update — caller (ViewModel) sets the uid. */
    suspend fun saveItem(item: InventoryItem): Result<InventoryItem> = try {
        require(item.uid.isNotBlank()) { "Item uid must be set by the ViewModel" }
        itemDao.insertItem(item.toEntity().copy(synced = false))
        markPending()
        Result.success(item)
    } catch (e: Exception) {
        InventoryLogger.e("InventoryItemRepository", "saveItem failed", e)
        Result.failure(e)
    }

    /** Offline-first soft-delete (active = false, synced = false) so the push picks it up. */
    suspend fun deleteItem(id: String): Result<Unit> = try {
        val existing = itemDao.getItemById(id)
        if (existing != null) {
            itemDao.insertItem(existing.copy(active = false, synced = false))
            markPending()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        InventoryLogger.e("InventoryItemRepository", "deleteItem failed", e)
        Result.failure(e)
    }

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.INVENTORY, Clock.System.now().toEpochMilliseconds())
    }
}
