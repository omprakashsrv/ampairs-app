package com.ampairs.business.agent

import androidx.room.Dao
import androidx.room.Query
import com.ampairs.business.data.db.BusinessEntity

/**
 * Read-only query backing the assistant's business action — separate from the operational
 * [com.ampairs.business.data.db.BusinessDao]. The business profile is a singleton row.
 */
@Dao
interface BusinessAgentDao {
    /** The (single) business profile, or null if none has synced yet. */
    @Query("SELECT * FROM business_profile LIMIT 1")
    suspend fun getProfile(): BusinessEntity?
}
