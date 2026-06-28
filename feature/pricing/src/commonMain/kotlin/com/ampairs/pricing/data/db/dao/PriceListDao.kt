package com.ampairs.pricing.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.pricing.data.db.entity.PriceListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceListDao {

    @Query("SELECT * FROM price_lists WHERE active = 1 ORDER BY priority DESC, name ASC")
    fun getAllPriceLists(): Flow<List<PriceListEntity>>

    @Query("SELECT * FROM price_lists WHERE id = :id")
    suspend fun getPriceListById(id: String): PriceListEntity?

    @Query("SELECT * FROM price_lists WHERE active = 1")
    suspend fun getActivePriceLists(): List<PriceListEntity>

    @Query("SELECT * FROM price_lists WHERE synced = 0")
    suspend fun getUnsyncedPriceLists(): List<PriceListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceList(priceList: PriceListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceLists(priceLists: List<PriceListEntity>)

    @Query("DELETE FROM price_lists WHERE id = :id")
    suspend fun hardDeletePriceList(id: String)

    @Query("DELETE FROM price_lists")
    suspend fun clearAll()
}
