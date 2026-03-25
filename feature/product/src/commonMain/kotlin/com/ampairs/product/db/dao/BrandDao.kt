package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.BrandEntity
import com.ampairs.product.db.model.BrandModel

@Dao
interface BrandDao {

    @Query("SELECT * FROM brandEntity WHERE id = :id")
    suspend fun brandById(id: String): BrandEntity?

    @Transaction
    @Query("SELECT * FROM brandEntity ORDER BY name ASC, active DESC")
    suspend fun getBrands(): List<BrandModel>

    @Query("SELECT * FROM brandEntity WHERE synced = 0")
    suspend fun unSyncedBrands(): List<BrandEntity>

    @Query("SELECT * FROM brandEntity WHERE active = 1 ORDER BY name ASC")
    suspend fun getActiveBrands(): List<BrandEntity>

    @Query("SELECT * FROM brandEntity WHERE name LIKE '%' || :searchText || '%' ORDER BY name ASC")
    suspend fun getBrandsByName(searchText: String): List<BrandEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(brand: BrandEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(brands: List<BrandEntity>)

    @Update
    suspend fun update(brand: BrandEntity)

    @Query("DELETE FROM brandEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM brandEntity")
    suspend fun deleteAll()

    @Query("UPDATE brandEntity SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE brandEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE brandEntity SET active = :active WHERE id = :id")
    suspend fun updateActiveStatus(id: String, active: Int)

    @Transaction
    suspend fun insertBrands(brands: List<BrandEntity>) {
        insertAll(brands)
    }
}