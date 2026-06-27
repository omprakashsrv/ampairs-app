package com.ampairs.customer.agent

import androidx.room.Dao
import androidx.room.Query

/**
 * Read-only report queries backing the assistant's customer actions — separate from the operational
 * [com.ampairs.customer.data.db.CustomerDao]. Over the existing `customers` table (no schema impact).
 */
@Dao
interface CustomerAgentDao {
    /** Count of active customers. */
    @Query("SELECT count(*) FROM customers WHERE active = 1")
    suspend fun countActive(): Int
}
