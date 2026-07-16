package com.ampairs.unit.agent

import androidx.room3.Dao
import androidx.room3.Query

/**
 * Read-only report queries backing the assistant's unit actions — separate from the operational
 * [com.ampairs.unit.data.db.dao.UnitDao]. Over the existing `units` table (no schema impact).
 */
@Dao
interface UnitAgentDao {
    /** Count of active units of measure. */
    @Query("SELECT COUNT(*) FROM units WHERE active = 1")
    suspend fun countActive(): Int
}
