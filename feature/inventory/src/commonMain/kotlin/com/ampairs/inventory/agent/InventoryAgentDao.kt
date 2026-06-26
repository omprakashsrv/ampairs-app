package com.ampairs.inventory.agent

import androidx.room.Dao
import androidx.room.Query

/**
 * Read-only report queries backing the assistant's inventory actions — separate from the operational
 * [com.ampairs.inventory.data.db.InventoryItemDao]. Over the existing `inventory_items` table (no
 * schema impact).
 */
@Dao
interface InventoryAgentDao {
    /** Count of active inventory items. */
    @Query("SELECT count(*) FROM inventory_items WHERE active = 1")
    suspend fun countActive(): Int
}
