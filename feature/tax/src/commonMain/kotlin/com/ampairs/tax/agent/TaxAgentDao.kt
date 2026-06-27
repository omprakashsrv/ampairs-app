package com.ampairs.tax.agent

import androidx.room.Dao
import androidx.room.Query

/**
 * Read-only report queries backing the assistant's tax actions — separate from the operational tax
 * DAOs. Over the existing `tax_codes` table (no schema impact). Note the active flag is `is_active`.
 */
@Dao
interface TaxAgentDao {
    /** Count of active (subscribed) tax codes in this workspace. */
    @Query("SELECT COUNT(*) FROM tax_codes WHERE is_active = 1")
    suspend fun countActive(): Int
}
