package com.ampairs.inventory.data

import com.ampairs.inventory.domain.InventoryStockInfo
import kotlinx.coroutines.flow.Flow

/**
 * Cross-feature contract for reading inventory stock (spec 014). The mobile analogue of the backend's
 * public `InventoryStockService`. `feature/inventory` implements this over its local Room DAO and binds
 * it in `WorkspaceScope`; `feature/order` / `feature/invoice` depend on THIS module only (never on
 * `feature/inventory`) to show availability / low-stock in the line-item editor.
 *
 * Read-side only — stock mutation is server-authoritative (the device pushes the sale, the server
 * deducts, the device pulls the updated item via sync).
 */
interface InventoryDataService {

    /** Current stock snapshot for a product (and optional variant), or null if not tracked. */
    suspend fun getStock(productId: String, productVariantId: String? = null): InventoryStockInfo?

    /** Reactive stock for a product, so an open editor reflects sync updates. */
    fun observeStock(productId: String, productVariantId: String? = null): Flow<InventoryStockInfo?>
}
