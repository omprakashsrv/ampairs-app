package com.ampairs.pricing.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.pricing.data.db.entity.OfferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfferDao {

    @Query("SELECT * FROM offers WHERE active = 1 ORDER BY updated_at DESC")
    fun getAllOffers(): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers WHERE id = :id")
    suspend fun getOfferById(id: String): OfferEntity?

    @Query("SELECT * FROM offers WHERE synced = 0")
    suspend fun getUnsyncedOffers(): List<OfferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffer(offer: OfferEntity)

    @Query("DELETE FROM offers WHERE id = :id")
    suspend fun hardDeleteOffer(id: String)

    @Query("DELETE FROM offers")
    suspend fun clearAll()
}
