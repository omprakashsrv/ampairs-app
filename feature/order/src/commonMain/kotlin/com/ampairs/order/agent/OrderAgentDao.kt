package com.ampairs.order.agent

import androidx.room.Dao
import androidx.room.Query

/**
 * Read-only report queries backing the assistant's order actions — separate from the operational
 * [com.ampairs.order.db.OrderDao]. Over the existing `orderEntity` table (no schema impact).
 */
@Dao
interface OrderAgentDao {
    /** Count of active orders. */
    @Query("SELECT count(*) FROM orderEntity WHERE active = 1")
    suspend fun countActive(): Int
}
