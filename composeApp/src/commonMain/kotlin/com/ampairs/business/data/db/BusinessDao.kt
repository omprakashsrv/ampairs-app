package com.ampairs.business.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {

    @Query("SELECT * FROM business_profile LIMIT 1")
    fun observeBusiness(): Flow<BusinessEntity?>

    @Query("SELECT * FROM business_profile LIMIT 1")
    suspend fun getBusiness(): BusinessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBusiness(entity: BusinessEntity)

    @Query("UPDATE business_profile SET synced = :synced, last_sync_epoch = :lastSyncEpoch WHERE uid = :uid")
    suspend fun updateSyncStatus(uid: String, synced: Boolean, lastSyncEpoch: Long)

    @Query("SELECT * FROM business_profile WHERE synced = 0 LIMIT 1")
    suspend fun getPendingBusiness(): BusinessEntity?

    @Query("DELETE FROM business_profile")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM business_profile")
    suspend fun count(): Int
}
