package com.ampairs.inventory.data

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.db.InventoryItemDao
import com.ampairs.inventory.data.db.InventoryItemEntity
import com.ampairs.inventory.domain.InventoryStockInfo
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read-side implementation of the cross-feature [InventoryDataService] contract (spec 014).
 * order/invoice depend on `feature/inventory-api` (the interface) and resolve this binding at runtime,
 * without depending on the full inventory feature.
 */
@Inject
@ContributesBinding(WorkspaceScope::class)
class InventoryDataServiceImpl(
    private val itemDao: InventoryItemDao,
) : InventoryDataService {

    override suspend fun getStock(productId: String, productVariantId: String?): InventoryStockInfo? =
        itemDao.getItemByProduct(productId)?.toStockInfo()

    override fun observeStock(productId: String, productVariantId: String?): Flow<InventoryStockInfo?> =
        itemDao.observeItemByProduct(productId).map { it?.toStockInfo() }

    private fun InventoryItemEntity.toStockInfo(): InventoryStockInfo = InventoryStockInfo(
        productId = productId ?: "",
        productVariantId = productVariantId,
        onHand = currentStock,
        available = availableStock,
        reorderLevel = reorderLevel,
        isLowStock = reorderLevel > 0.0 && currentStock > 0.0 && currentStock <= reorderLevel,
    )
}
